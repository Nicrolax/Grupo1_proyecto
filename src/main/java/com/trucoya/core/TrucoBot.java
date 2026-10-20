package com.trucoya.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Inteligencia artificial progresiva (niveles 1 a 5). Nunca mira cartas ocultas: decide con su
 * mano, la muestra, lo jugado en la mesa y lo que "observa" (cantos, envidos revelados, señas
 * descubiertas, costumbres de cada rival).
 *
 * <ul>
 *   <li>Nivel 1: probabilidades simples, sin engaño.</li>
 *   <li>Nivel 2: umbrales más finos y algún farol ocasional.</li>
 *   <li>Nivel 3: simula Monte Carlo las manos posibles de los rivales con las cartas que faltan.</li>
 *   <li>Nivel 4: además deduce por los cantos (quien envida suele tener tanto), recuerda los
 *       envidos revelados, aprende de las señas descubiertas y lleva la cuenta de los faroles.</li>
 *   <li>Nivel 5: más simulaciones, faroles adaptados a lo que el rival suele
 *       rechazar y repliegue cuando la mano está perdida.</li>
 *   <li>Nivel 6 (el Viejo Vizcacha, Jefe Final): muchas más simulaciones, lee lo que significa
 *       cada respuesta del rival (quien acepta un truco suele tener cartas; quien no quiere el envido,
 *       pocos tantos), aprovecha los tantos cantados y juega con el tanteador: sabe cuándo un
 *       "no quiero" le regala el partido al otro y cuándo conviene arriesgar.</li>
 * </ul>
 *
 * Una instancia por asiento automático; el anfitrión le pasa todos los eventos con
 * {@link #observe(GameEvent, TrucoMatch)}.
 */
public final class TrucoBot {

    /** Decisión del bot: una acción y, si es PLAY_CARD, el índice de la carta en su mano. */
    public static final class Decision {
        public final ActionType action;
        public final int handIndex;

        Decision(ActionType action, int handIndex) {
            this.action = action;
            this.handIndex = handIndex;
        }

        @Override
        public String toString() {
            return action == ActionType.PLAY_CARD ? "PLAY_CARD#" + handIndex : action.name();
        }
    }

    private static final int UNKNOWN = -1;
    private static final int NO_LIMIT = 99;

    private final Random random;
    private final int level;

    // Conocimiento de la ronda actual (por asiento).
    private final int[] knownEnvido = new int[4];
    private final int[] envidoMax = new int[4];
    private final int[] envidoMin = new int[4];
    private final int[] florState = new int[4];
    private final boolean[] hasThree = new boolean[4];
    private final boolean[] weakHand = new boolean[4];
    private final boolean[] trucoCaller = new boolean[4];
    private final int[] knownFlor = new int[4];
    private final boolean[] trucoAccepted = new boolean[4];
    private final boolean[] envidoDeclined = new boolean[4];
    private boolean lastCallWasEnvido;
    private final List<List<Card>> mustHave = new ArrayList<List<Card>>(4);
    private boolean florContested;
    private boolean lastCallWasTruco;

    // Modelo de cada rival a lo largo del partido.
    private final int[] trucoCalls = new int[4];
    private final int[] bluffsSeen = new int[4];
    private final int[] challenged = new int[4];
    private final int[] folds = new int[4];

    public TrucoBot(Random random) {
        this(random, 1);
    }

    // Partes del nivel 6 (se pueden apagar por separado para medir cuánto aporta cada una).
    boolean optMinimax, optTanteador, optLecturas;
    double optLlamarTruco = 0.68;

    public TrucoBot(Random random, int level) {
        this.random = random;
        this.level = Math.max(1, Math.min(6, level));
        optMinimax = optTanteador = optLecturas = this.level >= 6;
        for (int i = 0; i < 4; i++) {
            mustHave.add(new ArrayList<Card>(3));
        }
        resetRound();
    }

    public int getLevel() {
        return level;
    }

    private void resetRound() {
        Arrays.fill(knownEnvido, UNKNOWN);
        Arrays.fill(envidoMax, NO_LIMIT);
        Arrays.fill(envidoMin, 0);
        Arrays.fill(florState, 0);
        Arrays.fill(hasThree, false);
        Arrays.fill(weakHand, false);
        Arrays.fill(trucoCaller, false);
        Arrays.fill(knownFlor, UNKNOWN);
        Arrays.fill(trucoAccepted, false);
        Arrays.fill(envidoDeclined, false);
        lastCallWasEnvido = false;
        for (List<Card> l : mustHave) {
            l.clear();
        }
        florContested = false;
        lastCallWasTruco = false;
    }

    // ------------------------------------------------------------------ observación

    /** Registra un evento público del partido. */
    public void observe(GameEvent e, TrucoMatch m) {
        int s = e.getSeat();
        switch (e.getType()) {
            case ROUND_STARTED:
                resetRound();
                break;
            case CALL: {
                ActionType a = e.getAction();
                if (s < 0 || s > 3 || a == null) break;
                if (a.isEnvidoCall()) {
                    envidoMin[s] = Math.max(envidoMin[s], a == ActionType.ENVIDO ? 26 : a == ActionType.REAL_ENVIDO ? 28 : 30);
                    lastCallWasTruco = false;
                    lastCallWasEnvido = true;
                } else if (a == ActionType.FLOR) {
                    florState[s] = 1;
                    lastCallWasTruco = false;
                } else if (a.isTrucoCall()) {
                    trucoCaller[s] = true;
                    trucoCalls[s]++;
                    if (lastCallWasTruco) {
                        challenged[s]++; // subir implica aceptar
                    }
                    lastCallWasTruco = true;
                } else if (a == ActionType.QUIERO || a == ActionType.NO_QUIERO || a == ActionType.ME_VOY_AL_MAZO) {
                    if (lastCallWasTruco) {
                        challenged[s]++;
                        if (a != ActionType.QUIERO) folds[s]++;
                        if (a == ActionType.QUIERO) trucoAccepted[s] = true;
                    } else if (lastCallWasEnvido && a == ActionType.NO_QUIERO) {
                        envidoDeclined[s] = true;
                    }
                    if (lastCallWasEnvido) {
                        lastCallWasEnvido = false;
                        lastCallWasTruco = false;   // vuelve el truco que estaba en suspenso, si había
                    }
                } else if (a == ActionType.CONTRAFLOR || a == ActionType.CONTRAFLOR_AL_RESTO
                        || a == ActionType.CON_FLOR_ME_ACHICO) {
                    florState[s] = 1;
                }
                break;
            }
            case CARD_PLAYED:
                if (s >= 0 && s < 4 && e.getTrick() == 0 && florState[s] == 0 && !florContested
                        && m.getConfig().isFlorEnabled()) {
                    florState[s] = -1; // jugó sin cantar flor: no la tiene
                }
                break;
            case TANTO_DECLARED:      // los tantos que cada uno cantó en voz alta
                if (s >= 0 && s < 4) {
                    if (e.getAction() == ActionType.FLOR) {
                        knownFlor[s] = e.getValue();
                        florState[s] = 1;
                    } else {
                        knownEnvido[s] = e.getValue();
                    }
                }
                break;
            case ENVIDO_RESOLVED:
                if (s >= 0 && s < 4) {
                    knownEnvido[s] = e.getValue();
                    for (int o = 0; o < m.getConfig().getPlayers(); o++) {
                        if (o % 2 != s % 2) envidoMax[o] = Math.min(envidoMax[o], e.getValue());
                    }
                }
                break;
            case FLOR_RESOLVED:
                if (s >= 0 && s < 4) florState[s] = 1;
                if (e.getValue() >= 0) florContested = true;
                break;
            case ROUND_ENDED:
                for (int o = 0; o < m.getConfig().getPlayers(); o++) {
                    if (trucoCaller[o]) {
                        int played = 0;
                        int best = 0;
                        for (int t = 0; t < 3; t++) {
                            Card c = m.getTrickCard(t, o);
                            if (c != null) {
                                played++;
                                best = Math.max(best, m.getRules().trickRank(c));
                            }
                        }
                        if (played == 3 && best < 10) bluffsSeen[o]++;
                    }
                }
                break;
            default:
                break;
        }
    }

    /** El bot descubrió una seña de {@code seat}: deduce una carta o condición de su mano. */
    public void learnSena(int seat, Sena sena, Card muestra) {
        if (seat < 0 || seat > 3 || sena == null || muestra == null) return;
        switch (sena) {
            case DOS: addMust(seat, piezaCard(2, muestra)); break;
            case CUATRO: addMust(seat, piezaCard(4, muestra)); break;
            case CINCO: addMust(seat, piezaCard(5, muestra)); break;
            case CABALLO: addMust(seat, piezaCard(11, muestra)); break;
            case SOTA: addMust(seat, piezaCard(10, muestra)); break;
            case ANCHO_ESPADA: addMust(seat, Card.of(1, Suit.ESPADA)); break;
            case ANCHO_BASTO: addMust(seat, Card.of(1, Suit.BASTO)); break;
            case SIETE_ESPADA: addMust(seat, Card.of(7, Suit.ESPADA)); break;
            case SIETE_ORO: addMust(seat, Card.of(7, Suit.ORO)); break;
            case TRES: hasThree[seat] = true; break;
            case FLOR: florState[seat] = 1; break;
            case NADA: weakHand[seat] = true; break;
            default: break;
        }
    }

    private static Card piezaCard(int number, Card muestra) {
        return muestra.getNumber() == number ? Card.of(12, muestra.getSuit()) : Card.of(number, muestra.getSuit());
    }

    private void addMust(int seat, Card c) {
        if (!mustHave.get(seat).contains(c)) mustHave.get(seat).add(c);
    }

    private double bluffRateOf(int seat) {
        return (bluffsSeen[seat] + 1.0) / (trucoCalls[seat] + 4.0);
    }

    private double foldRateOfOpponents(int seat, int n) {
        double num = 1;
        double den = 3;
        for (int o = 0; o < n; o++) {
            if (o % 2 != seat % 2) {
                num += folds[o];
                den += challenged[o];
            }
        }
        return num / den;
    }

    private double myBluffRate(int seat, int n) {
        switch (level) {
            case 1: return 0.0;
            case 2: return 0.05;
            case 3: return 0.10;
            case 4: return 0.15;
            case 5: return Math.min(0.35, 0.10 + 0.30 * foldRateOfOpponents(seat, n));
            default: return Math.min(0.40, 0.08 + 0.40 * foldRateOfOpponents(seat, n));
        }
    }

    // ------------------------------------------------------------------ decisión

    /** Aplica la decisión al partido. */
    public ActionResult play(TrucoMatch m, int seat) {
        Decision d = decide(m, seat);
        if (d == null) {
            return ActionResult.fail("El bot no tiene jugadas");
        }
        return d.action == ActionType.PLAY_CARD ? m.playCard(seat, d.handIndex) : m.act(seat, d.action);
    }

    public Decision decide(TrucoMatch m, int seat) {
        List<ActionType> legal = m.legalActions(seat);
        if (legal.isEmpty()) {
            return null;
        }
        Decision d = level <= 2 ? thinkBasic(m, seat, legal) : thinkMonteCarlo(m, seat, legal);
        if (d == null || !legal.contains(d.action)
                || (d.action == ActionType.PLAY_CARD && (d.handIndex < 0 || d.handIndex >= m.getHand(seat).size()))) {
            d = legal.contains(ActionType.PLAY_CARD) ? new Decision(ActionType.PLAY_CARD, chooseCardBasic(m, seat))
                    : new Decision(safeDefault(legal), -1);
        }
        return d;
    }

    private static ActionType safeDefault(List<ActionType> legal) {
        if (legal.contains(ActionType.FLOR)) return ActionType.FLOR;
        if (legal.contains(ActionType.QUIERO)) return ActionType.QUIERO;
        if (legal.contains(ActionType.CON_FLOR_ME_ACHICO)) return ActionType.CON_FLOR_ME_ACHICO;
        return legal.get(0);
    }

    private static Decision act(ActionType a) {
        return new Decision(a, -1);
    }

    // ------------------------------------------------------------------ niveles 1-2

    private Decision thinkBasic(TrucoMatch m, int seat, List<ActionType> legal) {
        CardRules rules = m.getRules();
        List<Card> dealt = m.getDealtHand(seat);
        ActionType pending = m.getPendingCall();
        int envido = rules.envidoPoints(dealt);
        boolean l2 = level >= 2;

        if (legal.contains(ActionType.FLOR)) {
            return act(ActionType.FLOR);
        }
        if (legal.contains(ActionType.CON_FLOR_ME_ACHICO)) {
            int flor = rules.florPoints(dealt);
            if (legal.contains(ActionType.CONTRAFLOR)) {
                if (flor >= 36) {
                    return act(random.nextFloat() < 0.3f ? ActionType.CONTRAFLOR_AL_RESTO : ActionType.CONTRAFLOR);
                }
                return act(ActionType.CON_FLOR_ME_ACHICO);
            }
            if (legal.contains(ActionType.QUIERO)) {
                return act(flor >= 33 ? ActionType.QUIERO : ActionType.NO_QUIERO);
            }
            return act(ActionType.CON_FLOR_ME_ACHICO);
        }

        if (pending != null && pending.isEnvidoCall() && legal.contains(ActionType.QUIERO)) {
            if (envido >= 31 && random.nextFloat() < 0.5f) {
                if (legal.contains(ActionType.REAL_ENVIDO)) return act(ActionType.REAL_ENVIDO);
                if (legal.contains(ActionType.FALTA_ENVIDO) && envido >= 33) return act(ActionType.FALTA_ENVIDO);
            }
            int need = l2 ? 26 : 27;
            if (envido >= need || (envido >= 24 && random.nextFloat() < 0.3f)) {
                return act(ActionType.QUIERO);
            }
            return act(ActionType.NO_QUIERO);
        }

        double strength = handStrength(m, seat);

        if (pending != null && pending.isTrucoCall() && legal.contains(ActionType.QUIERO)) {
            if (legal.contains(ActionType.ENVIDO) && envido >= 28) {
                return act(envido >= 31 ? ActionType.REAL_ENVIDO : ActionType.ENVIDO);
            }
            ActionType raise = ActionType.trucoCallForLevel(pending.trucoLevel() + 1);
            if (raise != null && legal.contains(raise) && strength >= 0.75 && random.nextFloat() < 0.5f) {
                return act(raise);
            }
            double accept = l2 ? 0.48 : 0.45;
            if (strength >= accept || (strength >= 0.3 && random.nextFloat() < 0.3f)) {
                return act(ActionType.QUIERO);
            }
            return act(ActionType.NO_QUIERO);
        }

        if (legal.contains(ActionType.ENVIDO) && envido >= 28 && random.nextFloat() < 0.7f) {
            return act(envido >= 31 ? ActionType.REAL_ENVIDO : ActionType.ENVIDO);
        }
        double bluff = myBluffRate(seat, m.getConfig().getPlayers());
        for (ActionType a : legal) {
            if (a.isTrucoCall()) {
                if (strength >= 0.7 && random.nextFloat() < 0.5f) return act(a);
                if (strength < 0.3 && random.nextDouble() < bluff) return act(a);
            }
        }
        if (legal.contains(ActionType.PLAY_CARD)) {
            return new Decision(ActionType.PLAY_CARD, chooseCardBasic(m, seat));
        }
        return null;
    }

    /** Fuerza estimada de la mano (0..1) según cartas restantes y bazas ya definidas. */
    double handStrength(TrucoMatch m, int seat) {
        CardRules rules = m.getRules();
        List<Card> hand = m.getHand(seat);
        double best = 0;
        double sum = 0;
        for (Card c : hand) {
            double r = rules.trickRank(c) / 19.0;
            best = Math.max(best, r);
            sum += r;
        }
        double avg = hand.isEmpty() ? 0 : sum / hand.size();
        double s = 0.6 * best + 0.4 * avg;
        int team = seat % 2;
        for (int t = 0; t < 3; t++) {
            int res = m.getTrickResult(t);
            if (res == team) s += 0.25;
            else if (res >= 0) s -= 0.25;
        }
        return Math.max(0, Math.min(1, s));
    }

    /** Fuerza (0..1) de una mano cualquiera, para que el anfitrión detecte faroles. */
    public static double strengthOf(List<Card> cards, CardRules rules) {
        double best = 0;
        double sum = 0;
        for (Card c : cards) {
            double r = rules.trickRank(c) / 19.0;
            best = Math.max(best, r);
            sum += r;
        }
        return cards.isEmpty() ? 0 : 0.6 * best + 0.4 * (sum / cards.size());
    }

    /** Gana barato si puede; si el compañero ya gana, tira la peor. */
    int chooseCardBasic(TrucoMatch m, int seat) {
        CardRules rules = m.getRules();
        List<Card> hand = m.getHand(seat);
        int n = m.getConfig().getPlayers();
        int t = m.getTrickIndex();
        int team = seat % 2;
        int oppBest = -1;
        int mateBest = -1;
        boolean anyPlayed = false;
        for (int s = 0; s < n; s++) {
            Card c = m.getTrickCard(t, s);
            if (c == null) continue;
            anyPlayed = true;
            int r = rules.trickRank(c);
            if (s % 2 == team) mateBest = Math.max(mateBest, r);
            else oppBest = Math.max(oppBest, r);
        }
        int lowest = 0;
        int highest = 0;
        for (int i = 1; i < hand.size(); i++) {
            if (rules.trickRank(hand.get(i)) < rules.trickRank(hand.get(lowest))) lowest = i;
            if (rules.trickRank(hand.get(i)) > rules.trickRank(hand.get(highest))) highest = i;
        }
        if (!anyPlayed) {
            if (hand.size() == 3) {
                for (int i = 0; i < 3; i++) {
                    if (i != lowest && i != highest) return i;
                }
            }
            return highest;
        }
        if (mateBest > oppBest) {
            return lowest;
        }
        int cheapestWinner = -1;
        for (int i = 0; i < hand.size(); i++) {
            int r = rules.trickRank(hand.get(i));
            if (r > oppBest && (cheapestWinner < 0 || r < rules.trickRank(hand.get(cheapestWinner)))) {
                cheapestWinner = i;
            }
        }
        return cheapestWinner >= 0 ? cheapestWinner : lowest;
    }

    // ------------------------------------------------------------------ niveles 3-5 (Monte Carlo)

    private int samples() {
        switch (level) {
            case 3: return 24;
            case 4: return 60;
            case 5: return 140;
            default: return 320;
        }
    }

    private Decision thinkMonteCarlo(TrucoMatch m, int seat, List<ActionType> legal) {
        ActionType pending = m.getPendingCall();
        int team = seat % 2;
        int n = m.getConfig().getPlayers();
        int samples = samples();

        if (legal.contains(ActionType.FLOR)) {
            return act(ActionType.FLOR);
        }
        if (legal.contains(ActionType.CON_FLOR_ME_ACHICO)) {
            double pf = florWinProb(m, seat, samples);
            if (legal.contains(ActionType.CONTRAFLOR)) {
                if (pf > 0.85) return act(ActionType.CONTRAFLOR_AL_RESTO);
                if (pf > 0.6) return act(ActionType.CONTRAFLOR);
                return act(ActionType.CON_FLOR_ME_ACHICO);
            }
            if (legal.contains(ActionType.QUIERO)) {
                if (legal.contains(ActionType.CONTRAFLOR_AL_RESTO) && pf > 0.85) return act(ActionType.CONTRAFLOR_AL_RESTO);
                return act(pf > 0.5 ? ActionType.QUIERO : ActionType.NO_QUIERO);
            }
            return act(ActionType.CON_FLOR_ME_ACHICO);
        }

        if (pending != null && pending.isEnvidoCall() && legal.contains(ActionType.QUIERO)) {
            double pe = envidoWinProb(m, seat, samples);
            int v = pending == ActionType.FALTA_ENVIDO ? m.faltaValueFor(team) : pending == ActionType.REAL_ENVIDO ? 3 : 2;
            if (pe > 0.8) {
                if (pe > 0.92 && legal.contains(ActionType.FALTA_ENVIDO) && m.faltaValueFor(team) <= 8) {
                    return act(ActionType.FALTA_ENVIDO);
                }
                if (legal.contains(ActionType.REAL_ENVIDO)) return act(ActionType.REAL_ENVIDO);
            }
            double need = (v - 1.0) / (2.0 * v) + 0.12;
            if (optTanteador) {
                need += tanteador(m, team, declinedPoints(m), v);
            }
            return act(pe > need ? ActionType.QUIERO : ActionType.NO_QUIERO);
        }

        if (pending != null && pending.isTrucoCall() && legal.contains(ActionType.QUIERO)) {
            if (legal.contains(ActionType.ENVIDO)) {
                double pe = envidoWinProb(m, seat, samples);
                if (pe > 0.8 && legal.contains(ActionType.REAL_ENVIDO)) return act(ActionType.REAL_ENVIDO);
                if (pe > 0.62) return act(ActionType.ENVIDO);
            }
            double p = roundWinProb(m, seat, samples, null, true);
            int newV = pending.trucoLevel() + 1;
            int oldV = Round.trucoValue(pending.trucoLevel() - 1);
            ActionType raise = ActionType.trucoCallForLevel(pending.trucoLevel() + 1);
            if (raise != null && legal.contains(raise)) {
                if (p > 0.72) return act(raise);
                if (level >= 5 && p < 0.25 && random.nextDouble() < myBluffRate(seat, n) * 0.4) return act(raise);
            }
            double need = (newV - oldV) / (2.0 * newV) + 0.05;
            if (optTanteador) {
                need += tanteador(m, team, oldV, Round.trucoValue(pending.trucoLevel()));
            }
            return act(p > need ? ActionType.QUIERO : ActionType.NO_QUIERO);
        }

        // Mi turno.
        if (legal.contains(ActionType.ENVIDO)) {
            double pe = envidoWinProb(m, seat, samples);
            if (pe > 0.9 && legal.contains(ActionType.FALTA_ENVIDO) && m.faltaValueFor(team) <= 6) {
                return act(ActionType.FALTA_ENVIDO);
            }
            if (pe > 0.75) return act(ActionType.REAL_ENVIDO);
            if (pe > 0.6) return act(ActionType.ENVIDO);
            if (level >= 4 && pe < 0.2 && random.nextDouble() < myBluffRate(seat, n) * 0.5) {
                return act(ActionType.ENVIDO); // farol de envido
            }
        }
        ActionType trucoCall = null;
        for (ActionType a : legal) {
            if (a.isTrucoCall()) trucoCall = a;
        }
        double p = -1;
        if (trucoCall != null || legal.contains(ActionType.ME_VOY_AL_MAZO)) {
            p = roundWinProb(m, seat, samples, null, false);
        }
        if (trucoCall != null) {
            if (p > optLlamarTruco) return act(trucoCall);
            if (p < 0.3 && random.nextDouble() < myBluffRate(seat, n)) return act(trucoCall);
        }
        if (level >= 5 && p >= 0 && p < 0.04 && m.getTrucoLevel() >= 1 && legal.contains(ActionType.ME_VOY_AL_MAZO)
                && m.getTrickIndex() < 2) {
            return act(ActionType.ME_VOY_AL_MAZO); // repliegue
        }
        if (legal.contains(ActionType.PLAY_CARD)) {
            return new Decision(ActionType.PLAY_CARD, chooseCardMonteCarlo(m, seat, samples));
        }
        return null;
    }

    /**
     * Ajuste del umbral para aceptar según el tanteador (nivel 6). {@code siNo}: lo que se lleva el rival si
     * no quiero; {@code enJuego}: lo que se pierde si quiero y pierdo. Negativo = aceptar más fácil.
     */
    private double tanteador(TrucoMatch m, int team, int siNo, int enJuego) {
        int target = m.getConfig().getTargetScore();
        int mine = m.getScore(team);
        int theirs = m.getScore(1 - team);
        if (theirs + siNo >= target) {
            return -1.0;                      // decir que no es perder el partido: se acepta siempre
        }
        if (mine + enJuego >= target && theirs + enJuego < target) {
            return -0.08;                     // si gano, gano el partido; si pierdo, todavía sigo
        }
        if (theirs + enJuego >= target && mine + enJuego < target) {
            return +0.10;                     // si pierdo, pierdo el partido: más cuidado
        }
        if (mine - theirs >= 8) {
            return +0.04;                     // con ventaja, no hace falta arriesgar
        }
        if (theirs - mine >= 8) {
            return -0.04;                     // atrás, hay que buscar
        }
        return 0.0;
    }

    /** Puntos que se lleva el rival si no se quiere el envido pendiente. */
    private static int declinedPoints(TrucoMatch m) {
        ActionType p = m.getPendingCall();
        if (p == ActionType.ENVIDO) return 1;
        if (p == ActionType.REAL_ENVIDO) return 2;
        return 3;
    }

    private int chooseCardMonteCarlo(TrucoMatch m, int seat, int samples) {
        List<Card> hand = m.getHand(seat);
        if (hand.size() == 1) return 0;
        CardRules rules = m.getRules();
        int best = 0;
        double bestP = -1;
        for (int i = 0; i < hand.size(); i++) {
            double p = roundWinProb(m, seat, samples, hand.get(i), false);
            // A igual probabilidad, guardar las cartas altas.
            boolean better = p > bestP + 1e-9
                    || (Math.abs(p - bestP) <= 1e-9 && rules.trickRank(hand.get(i)) < rules.trickRank(hand.get(best)));
            if (better) {
                bestP = p;
                best = i;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ muestreo

    private List<Card> playedBy(TrucoMatch m, int s) {
        List<Card> out = new ArrayList<Card>(3);
        for (int t = 0; t < 3; t++) {
            Card c = m.getTrickCard(t, s);
            if (c != null) out.add(c);
        }
        return out;
    }

    /** Cartas que este asiento no vio: todo menos su mano, la muestra y lo jugado. */
    private List<Card> unseen(TrucoMatch m, int seat) {
        boolean[] seen = new boolean[Card.DECK_SIZE];
        for (Card c : m.getDealtHand(seat)) seen[c.getId()] = true;
        seen[m.getMuestra().getId()] = true;
        int n = m.getConfig().getPlayers();
        for (int s = 0; s < n; s++) {
            for (Card c : playedBy(m, s)) seen[c.getId()] = true;
        }
        List<Card> out = new ArrayList<Card>();
        for (Card c : Card.all()) {
            if (!seen[c.getId()]) out.add(c);
        }
        return out;
    }

    /**
     * Reparte al azar las cartas ocultas de los demás respetando lo que se sabe.
     * Devuelve las manos RESTANTES por asiento (la propia es la real).
     */
    private List<List<Card>> sample(TrucoMatch m, int seat, List<Card> pool, boolean callerEvidence,
                                    boolean requireOpponentFlor) {
        int n = m.getConfig().getPlayers();
        CardRules rules = m.getRules();
        List<List<Card>> played = new ArrayList<List<Card>>(n);
        int[] need = new int[n];
        for (int s = 0; s < n; s++) {
            List<Card> p = playedBy(m, s);
            played.add(p);
            need[s] = s == seat ? 0 : 3 - p.size();
        }
        List<Card> deck = new ArrayList<Card>(pool);
        int attempts = level >= 6 ? 220 : level >= 4 ? 150 : 40;
        for (int a = 0; a < attempts; a++) {
            Collections.shuffle(deck, random);
            List<List<Card>> rem = assign(m, seat, deck, need, n);
            if (rem != null && consistent(m, seat, rem, played, rules, callerEvidence, requireOpponentFlor, n)) {
                return rem;
            }
        }
        return null; // no se encontró una mano compatible con lo observado
    }

    /** Reparto sin restricciones (respaldo cuando lo observado es contradictorio). */
    private List<List<Card>> sampleFree(TrucoMatch m, int seat, List<Card> pool) {
        int n = m.getConfig().getPlayers();
        int[] need = new int[n];
        for (int s = 0; s < n; s++) {
            need[s] = s == seat ? 0 : 3 - playedBy(m, s).size();
        }
        List<Card> deck = new ArrayList<Card>(pool);
        Collections.shuffle(deck, random);
        return assign(m, seat, deck, need, n);
    }

    private List<List<Card>> assign(TrucoMatch m, int seat, List<Card> deck, int[] need, int n) {
        List<List<Card>> rem = new ArrayList<List<Card>>(n);
        int idx = 0;
        for (int s = 0; s < n; s++) {
            if (s == seat) {
                rem.add(new ArrayList<Card>(m.getHand(seat)));
                continue;
            }
            List<Card> h = new ArrayList<Card>(need[s]);
            for (int k = 0; k < need[s]; k++) {
                if (idx >= deck.size()) return null;
                h.add(deck.get(idx++));
            }
            rem.add(h);
        }
        return rem;
    }

    private boolean consistent(TrucoMatch m, int seat, List<List<Card>> rem, List<List<Card>> played, CardRules rules,
                               boolean callerEvidence, boolean requireOpponentFlor, int n) {
        boolean flor = m.getConfig().isFlorEnabled();
        boolean opponentFlor = false;
        for (int s = 0; s < n; s++) {
            if (s == seat) continue;
            List<Card> full = new ArrayList<Card>(played.get(s));
            full.addAll(rem.get(s));
            if (full.size() != 3) continue;
            boolean hf = rules.hasFlor(full);
            if (s % 2 != seat % 2 && hf) opponentFlor = true;
            if (knownEnvido[s] != UNKNOWN && rules.envidoPoints(full) != knownEnvido[s]) return false;
            if (envidoMax[s] < NO_LIMIT && rules.envidoPoints(full) > envidoMax[s]) return false;
            if (flor && florState[s] == 1 && !hf) return false;
            if (knownFlor[s] != UNKNOWN && (!hf || rules.florPoints(full) != knownFlor[s])) return false;
            if (flor && florState[s] == -1 && hf) return false;
            for (Card c : mustHave.get(s)) {
                if (!full.contains(c)) return false;
            }
            int maxRank = 0;
            boolean three = false;
            for (Card c : full) {
                maxRank = Math.max(maxRank, rules.trickRank(c));
                if (c.getNumber() == 3 && !rules.isPieza(c)) three = true;
            }
            if (hasThree[s] && !three) return false;
            if (weakHand[s] && maxRank >= 10) return false;
            if (optLecturas && s % 2 != seat % 2) {
                // quien acepta un truco suele tener con qué; quien no quiere el envido, suele tener poco
                if (trucoAccepted[s] && maxRank < 8 && random.nextDouble() < 0.55) return false;
                if (envidoDeclined[s] && knownEnvido[s] == UNKNOWN && rules.envidoPoints(full) >= 29
                        && random.nextDouble() < 0.7) return false;
            }
            if (level >= 4 && s % 2 != seat % 2) {
                double honest = 1.0 - bluffRateOf(s);
                if (envidoMin[s] > 0 && knownEnvido[s] == UNKNOWN && rules.envidoPoints(full) < envidoMin[s]
                        && random.nextDouble() < honest) {
                    return false;
                }
                if (callerEvidence && trucoCaller[s] && maxRank < 10 && random.nextDouble() < honest) {
                    return false;
                }
            }
        }
        return !requireOpponentFlor || opponentFlor;
    }

    /** Probabilidad de que el equipo del asiento gane la ronda (opcionalmente jugando ya {@code forced}). */
    double roundWinProb(TrucoMatch m, int seat, int samples, Card forced, boolean callerEvidence) {
        int n = m.getConfig().getPlayers();
        int team = seat % 2;
        int manoTeam = m.getMano() % 2;
        int t0 = m.getTrickIndex();
        Card[] current = new Card[n];
        for (int s = 0; s < n; s++) current[s] = m.getTrickCard(t0, s);
        int[] results = {m.getTrickResult(0), m.getTrickResult(1), m.getTrickResult(2)};
        List<Card> pool = unseen(m, seat);
        CardRules rules = m.getRules();
        int wins = 0;
        int valid = 0;
        for (int i = 0; i < samples; i++) {
            List<List<Card>> rem = sample(m, seat, pool, callerEvidence, false);
            if (rem == null) {
                if (i >= 3 && valid == 0) rem = sampleFree(m, seat, pool); else continue;
            }
            if (rem == null) continue;
            valid++;
            int w = optMinimax
                    ? minimax(rules, n, manoTeam, rem, t0, m.getTrickLeader(), current, results, seat, forced, team)
                    : simulate(rules, n, manoTeam, rem, t0, m.getTrickLeader(), current, results, seat, forced);
            if (w == team) wins++;
        }
        return valid == 0 ? 0.5 : (double) wins / valid;
    }

    /** Probabilidad de ganar el envido. */
    double envidoWinProb(TrucoMatch m, int seat, int samples) {
        int n = m.getConfig().getPlayers();
        CardRules rules = m.getRules();
        List<Card> pool = unseen(m, seat);
        int mine = rules.envidoPoints(m.getDealtHand(seat));
        int wins = 0;
        int valid = 0;
        for (int i = 0; i < samples; i++) {
            List<List<Card>> rem = sample(m, seat, pool, false, false);
            if (rem == null) {
                if (i >= 3 && valid == 0) rem = sampleFree(m, seat, pool); else continue;
            }
            if (rem == null) continue;
            valid++;
            int bestSeat = -1;
            int best = -1;
            for (int k = 0; k < n; k++) {
                int s = (m.getMano() + k) % n;
                int v;
                if (s == seat) {
                    v = mine;
                } else {
                    List<Card> full = new ArrayList<Card>(playedBy(m, s));
                    full.addAll(rem.get(s));
                    v = rules.envidoPoints(full);
                }
                if (v > best) {
                    best = v;
                    bestSeat = s;
                }
            }
            if (bestSeat % 2 == seat % 2) wins++;
        }
        return valid == 0 ? 0.5 : (double) wins / valid;
    }

    /** Probabilidad de ganar una flor disputada (el rival tiene flor). */
    double florWinProb(TrucoMatch m, int seat, int samples) {
        int n = m.getConfig().getPlayers();
        CardRules rules = m.getRules();
        List<Card> pool = unseen(m, seat);
        List<Card> myDealt = m.getDealtHand(seat);
        int mine = rules.hasFlor(myDealt) ? rules.florPoints(myDealt) : -1;
        int wins = 0;
        int valid = 0;
        for (int i = 0; i < samples; i++) {
            List<List<Card>> rem = sample(m, seat, pool, false, true);
            if (rem == null) {
                if (i >= 3 && valid == 0) rem = sampleFree(m, seat, pool); else continue;
            }
            if (rem == null) continue;
            valid++;
            int bestSeat = -1;
            int best = -1;
            for (int k = 0; k < n; k++) {
                int s = (m.getMano() + k) % n;
                int v;
                if (s == seat) {
                    v = mine;
                } else {
                    List<Card> full = new ArrayList<Card>(playedBy(m, s));
                    full.addAll(rem.get(s));
                    v = rules.hasFlor(full) ? rules.florPoints(full) : -1;
                }
                if (v > best) {
                    best = v;
                    bestSeat = s;
                }
            }
            if (bestSeat >= 0 && bestSeat % 2 == seat % 2) wins++;
        }
        return valid == 0 ? 0.5 : (double) wins / valid;
    }

    // ------------------------------------------------------------------ simulación de bazas

    private static int simulate(CardRules rules, int n, int manoTeam, List<List<Card>> rem, int trickIdx, int leader,
                                Card[] current, int[] results, int forcedSeat, Card forcedCard) {
        int[] res = results.clone();
        Card[] cur = current.clone();
        int lead = leader;
        Card forced = forcedCard;
        for (int t = trickIdx; t < 3; t++) {
            for (int k = 0; k < n; k++) {
                int s = (lead + k) % n;
                if (cur[s] != null) continue;
                List<Card> hand = rem.get(s);
                if (hand.isEmpty()) return -1;
                Card c;
                if (s == forcedSeat && forced != null && hand.remove(forced)) {
                    c = forced;
                    forced = null;
                } else {
                    c = policy(rules, n, s, hand, cur, lead, t);
                }
                cur[s] = c;
            }
            int best = -1;
            int bestSeat = -1;
            boolean[] teamHasBest = new boolean[2];
            for (int k = 0; k < n; k++) {
                int s = (lead + k) % n;
                int r = rules.trickRank(cur[s]);
                if (r > best) {
                    best = r;
                    bestSeat = s;
                    teamHasBest[0] = false;
                    teamHasBest[1] = false;
                    teamHasBest[s % 2] = true;
                } else if (r == best) {
                    teamHasBest[s % 2] = true;
                }
            }
            int r = teamHasBest[0] && teamHasBest[1] ? TrickRules.PARDA : bestSeat % 2;
            res[t] = r;
            int w = TrickRules.winner(res[0], res[1], res[2], manoTeam);
            if (w >= 0) return w;
            if (r != TrickRules.PARDA) lead = bestSeat;
            cur = new Card[n];
        }
        return TrickRules.winner(res[0], res[1], res[2], manoTeam);
    }

    // ------------------------------------------------------------------ nivel 6: búsqueda completa

    /**
     * Resultado de la ronda si, en este reparto supuesto, todos juegan perfecto lo que queda
     * (cada uno a favor de su equipo). Con 3 cartas por mano el árbol es chico: se recorre entero.
     */
    private static int minimax(CardRules rules, int n, int manoTeam, List<List<Card>> rem, int trickIdx, int leader,
                               Card[] current, int[] results, int forcedSeat, Card forcedCard, int myTeam) {
        Card[][] manos = new Card[n][];
        for (int s = 0; s < n; s++) {
            manos[s] = rem.get(s).toArray(new Card[0]);
        }
        Card[] cur = current.clone();
        int[] res = results.clone();
        int forcedIdx = -1;
        if (forcedCard != null) {
            for (int i = 0; i < manos[forcedSeat].length; i++) {
                if (manos[forcedSeat][i].equals(forcedCard)) forcedIdx = i;
            }
        }
        return buscar(rules, n, manoTeam, manos, new boolean[n][3], trickIdx, leader, 0, cur, res,
                forcedSeat, forcedIdx, myTeam);
    }

    private static int buscar(CardRules rules, int n, int manoTeam, Card[][] manos, boolean[][] usada, int t, int lead,
                              int k, Card[] cur, int[] res, int forcedSeat, int forcedIdx, int myTeam) {
        // saltear a los que ya jugaron en esta baza
        while (k < n && cur[(lead + k) % n] != null) {
            k++;
        }
        if (k >= n) {
            // baza completa: se resuelve
            int best = -1, bestSeat = -1;
            boolean[] teamHasBest = new boolean[2];
            for (int j = 0; j < n; j++) {
                int s = (lead + j) % n;
                int r = rules.trickRank(cur[s]);
                if (r > best) {
                    best = r;
                    bestSeat = s;
                    teamHasBest[0] = teamHasBest[1] = false;
                    teamHasBest[s % 2] = true;
                } else if (r == best) {
                    teamHasBest[s % 2] = true;
                }
            }
            int r = teamHasBest[0] && teamHasBest[1] ? TrickRules.PARDA : bestSeat % 2;
            int[] res2 = res.clone();
            res2[t] = r;
            int w = TrickRules.winner(res2[0], res2[1], res2[2], manoTeam);
            if (w >= 0 || t >= 2) {
                return w >= 0 ? w : TrickRules.winner(res2[0], res2[1], res2[2], manoTeam);
            }
            int nextLead = r != TrickRules.PARDA ? bestSeat : lead;
            return buscar(rules, n, manoTeam, manos, usada, t + 1, nextLead, 0, new Card[n], res2,
                    forcedSeat, forcedIdx, myTeam);
        }
        int s = (lead + k) % n;
        boolean miEquipo = s % 2 == myTeam;
        int mejor = -2;
        boolean alguna = false;
        for (int i = 0; i < manos[s].length; i++) {
            if (usada[s][i]) continue;
            if (s == forcedSeat && forcedIdx >= 0 && i != forcedIdx) continue;
            alguna = true;
            usada[s][i] = true;
            cur[s] = manos[s][i];
            int fs = s == forcedSeat ? -1 : forcedSeat;
            int w = buscar(rules, n, manoTeam, manos, usada, t, lead, k + 1, cur, res, fs, forcedIdx, myTeam);
            cur[s] = null;
            usada[s][i] = false;
            if (mejor == -2) {
                mejor = w;
            } else if (miEquipo ? (w == myTeam && mejor != myTeam) : (w != myTeam && mejor == myTeam)) {
                mejor = w;
            }
            if (miEquipo ? mejor == myTeam : mejor != myTeam && mejor >= 0) {
                break;     // ya no se puede mejorar
            }
        }
        return alguna ? mejor : -1;
    }

    /** Política simple usada en las simulaciones para todos los jugadores. */
    private static Card policy(CardRules rules, int n, int s, List<Card> hand, Card[] cur, int lead, int t) {
        if (hand.size() == 1) return hand.remove(0);
        int best = -1;
        int mask = 0;
        boolean any = false;
        for (int k = 0; k < n; k++) {
            int seat = (lead + k) % n;
            Card c = cur[seat];
            if (c == null) continue;
            any = true;
            int r = rules.trickRank(c);
            if (r > best) {
                best = r;
                mask = 1 << (seat % 2);
            } else if (r == best) {
                mask |= 1 << (seat % 2);
            }
        }
        int lowest = 0;
        int highest = 0;
        for (int i = 1; i < hand.size(); i++) {
            if (rules.trickRank(hand.get(i)) < rules.trickRank(hand.get(lowest))) lowest = i;
            if (rules.trickRank(hand.get(i)) > rules.trickRank(hand.get(highest))) highest = i;
        }
        int pick;
        if (!any) {
            if (t == 0 && hand.size() == 3) {
                pick = 3 - lowest - highest;
                if (pick < 0 || pick > 2 || pick == lowest || pick == highest) pick = highest;
            } else {
                pick = highest;
            }
        } else if (mask == (1 << (s % 2))) {
            pick = lowest;
        } else {
            int cheapest = -1;
            for (int i = 0; i < hand.size(); i++) {
                int r = rules.trickRank(hand.get(i));
                if (r > best && (cheapest < 0 || r < rules.trickRank(hand.get(cheapest)))) cheapest = i;
            }
            pick = cheapest >= 0 ? cheapest : lowest;
        }
        return hand.remove(pick);
    }
}
