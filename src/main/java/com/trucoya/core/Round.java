package com.trucoya.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una ronda (una "mano" de reparto) de Truco Uruguayo: reparto, muestra, cantos y bazas.
 *
 * Orden de prioridad de los cantos: Flor &gt; Envido &gt; Truco ("el envido está primero").
 * Asientos 0..n-1 en orden de juego; equipo = asiento % 2 (en 2v2: 0 y 2 contra 1 y 3).
 *
 * Esta clase es de uso interno; la fachada pública es {@link TrucoMatch}.
 */
final class Round {

    enum PendingKind { TRUCO, ENVIDO, FLOR }

    enum FlorStage { FLOR, CONTRAFLOR, AL_RESTO }

    /** Canto a la espera de respuesta del equipo {@code awaitingTeam}. */
    static final class Pending {
        final PendingKind kind;
        final int callerSeat;
        final int awaitingTeam;
        /** Solo truco: nivel propuesto (1 truco, 2 retruco, 3 vale cuatro). */
        final int proposedLevel;
        final ActionType lastCall;

        Pending(PendingKind kind, int callerSeat, int awaitingTeam, int proposedLevel, ActionType lastCall) {
            this.kind = kind;
            this.callerSeat = callerSeat;
            this.awaitingTeam = awaitingTeam;
            this.proposedLevel = proposedLevel;
            this.lastCall = lastCall;
        }
    }

    static final int TRICK_NOT_PLAYED = -2;
    static final int TRICK_PARDA = -1;

    // Valores fijos de la Flor. Achicarse (o no querer) entrega lo ya cantado:
    // la flor (3), o la contraflor (6) si la contraflor fue subida a "al resto".
    // El punto de la ronda (por ejemplo, si después el rival se va al mazo sin truco) se suma aparte.
    static final int FLOR_POINTS = 3;
    static final int FLOR_ACHICO_POINTS = 3;
    static final int CONTRAFLOR_POINTS = 6;

    private final TrucoMatch match;
    private final int n;
    private final int dealer;
    private final int mano;
    private final Card muestra;
    private final CardRules rules;
    private final List<List<Card>> dealt;
    private final List<List<Card>> hand;

    // Bazas
    private int trickIndex;
    private int trickLeader;
    private int turn;
    private int playedInTrick;
    private final Card[][] trickCards;
    private final int[] trickResult;
    private final int[] trickWinnerSeat;

    // Truco
    private int trucoLevel;
    private int trucoRaiseTeam = -1;
    private boolean trucoEverCalled;

    // Cantos pendientes
    private Pending pending;
    private Pending suspendedTruco;

    // Envido
    private boolean envidoClosed;
    private final List<ActionType> envidoChain = new ArrayList<ActionType>();

    // Flor
    private boolean florClosed;
    private final boolean[] florSung;
    private FlorStage florStage;
    private boolean contraflorRaised;

    // Fin
    private boolean finished;
    private int winnerTeam = -1;

    Round(TrucoMatch match, int dealer, Deck deck) {
        this.match = match;
        this.n = match.getConfig().getPlayers();
        this.dealer = dealer;
        this.mano = (dealer + 1) % n;
        this.dealt = new ArrayList<List<Card>>(n);
        this.hand = new ArrayList<List<Card>>(n);
        for (int i = 0; i < n; i++) {
            dealt.add(new ArrayList<Card>(3));
            hand.add(new ArrayList<Card>(3));
        }
        // Reparto de a una carta, empezando por la mano.
        for (int r = 0; r < 3; r++) {
            for (int k = 0; k < n; k++) {
                int seat = (mano + k) % n;
                Card c = deck.draw();
                dealt.get(seat).add(c);
                hand.get(seat).add(c);
            }
        }
        // La carta siguiente del mazo queda boca arriba como Muestra.
        this.muestra = deck.draw();
        this.rules = new CardRules(muestra);

        this.trickCards = new Card[3][n];
        this.trickResult = new int[] {TRICK_NOT_PLAYED, TRICK_NOT_PLAYED, TRICK_NOT_PLAYED};
        this.trickWinnerSeat = new int[] {-1, -1, -1};
        this.trickIndex = 0;
        this.trickLeader = mano;
        this.turn = mano;
        this.florSung = new boolean[n];
        this.florClosed = !match.getConfig().isFlorEnabled();

        match.emit(GameEvent.roundStarted(mano, dealer, muestra));
    }

    // ------------------------------------------------------------------ consultas

    private static int teamOf(int seat) {
        return seat % 2;
    }

    private static int otherTeam(int team) {
        return 1 - team;
    }

    static int trucoValue(int level) {
        return level == 0 ? 1 : level + 1;
    }

    private boolean hasPlayedFirstCard(int seat) {
        return trickCards[0][seat] != null;
    }

    /** Puede (y por lo tanto debe) cantar Flor: la flor es obligatoria antes de jugar su primera carta. */
    boolean canSingFlor(int seat) {
        return !florClosed
                && !florSung[seat]
                && trickIndex == 0
                && !hasPlayedFirstCard(seat)
                && rules.hasFlor(dealt.get(seat));
    }

    private boolean canStartEnvido(int seat) {
        return !envidoClosed
                && trickIndex == 0
                && !hasPlayedFirstCard(seat)
                && !trucoEverCalled
                && !canSingFlor(seat);
    }

    /** "El envido está primero": responder al primer Truco con un canto de envido. */
    private boolean canAnswerTrucoWithEnvido(int seat) {
        return pending != null
                && pending.kind == PendingKind.TRUCO
                && pending.proposedLevel == 1
                && suspendedTruco == null
                && !envidoClosed
                && trickIndex == 0
                && !hasPlayedFirstCard(seat)
                && !canSingFlor(seat);
    }

    private List<ActionType> envidoRaises() {
        List<ActionType> out = new ArrayList<ActionType>(3);
        if (envidoChain.isEmpty()) {
            return out;
        }
        ActionType last = envidoChain.get(envidoChain.size() - 1);
        if (last == ActionType.ENVIDO) {
            if (envidoChain.size() == 1) {
                out.add(ActionType.ENVIDO);
            }
            out.add(ActionType.REAL_ENVIDO);
            out.add(ActionType.FALTA_ENVIDO);
        } else if (last == ActionType.REAL_ENVIDO) {
            out.add(ActionType.FALTA_ENVIDO);
        }
        return out;
    }

    List<ActionType> legalActions(int seat) {
        List<ActionType> out = new ArrayList<ActionType>();
        if (finished || match.isOver() || seat < 0 || seat >= n) {
            return out;
        }
        int team = teamOf(seat);

        if (pending != null) {
            if (pending.awaitingTeam != team) {
                return out;
            }
            switch (pending.kind) {
                case ENVIDO:
                    if (canSingFlor(seat)) {
                        out.add(ActionType.FLOR); // la flor anula el envido
                        return out;
                    }
                    out.add(ActionType.QUIERO);
                    out.add(ActionType.NO_QUIERO);
                    out.addAll(envidoRaises());
                    return out;
                case FLOR:
                    if (!rules.hasFlor(dealt.get(seat))) {
                        return out; // solo responde quien tiene flor
                    }
                    switch (florStage) {
                        case FLOR:
                            out.add(ActionType.CONTRAFLOR);
                            out.add(ActionType.CONTRAFLOR_AL_RESTO);
                            out.add(ActionType.CON_FLOR_ME_ACHICO);
                            break;
                        case CONTRAFLOR:
                            out.add(ActionType.QUIERO);
                            out.add(ActionType.NO_QUIERO);
                            out.add(ActionType.CONTRAFLOR_AL_RESTO);
                            out.add(ActionType.CON_FLOR_ME_ACHICO);
                            break;
                        case AL_RESTO:
                            out.add(ActionType.QUIERO);
                            out.add(ActionType.NO_QUIERO);
                            out.add(ActionType.CON_FLOR_ME_ACHICO);
                            break;
                        default:
                            break;
                    }
                    return out;
                case TRUCO:
                    out.add(ActionType.QUIERO);
                    out.add(ActionType.NO_QUIERO);
                    ActionType raise = ActionType.trucoCallForLevel(pending.proposedLevel + 1);
                    if (raise != null) {
                        out.add(raise);
                    }
                    if (canSingFlor(seat)) {
                        out.add(ActionType.FLOR);
                    }
                    if (canAnswerTrucoWithEnvido(seat)) {
                        out.add(ActionType.ENVIDO);
                        out.add(ActionType.REAL_ENVIDO);
                        out.add(ActionType.FALTA_ENVIDO);
                    }
                    out.add(ActionType.ME_VOY_AL_MAZO);
                    return out;
                default:
                    return out;
            }
        }

        if (seat != turn) {
            return out;
        }
        if (canSingFlor(seat)) {
            out.add(ActionType.FLOR);
            out.add(ActionType.ME_VOY_AL_MAZO);
            return out;
        }
        out.add(ActionType.PLAY_CARD);
        if (canStartEnvido(seat)) {
            out.add(ActionType.ENVIDO);
            out.add(ActionType.REAL_ENVIDO);
            out.add(ActionType.FALTA_ENVIDO);
        }
        if (trucoLevel < 3 && (trucoRaiseTeam < 0 || trucoRaiseTeam == team)) {
            out.add(ActionType.trucoCallForLevel(trucoLevel + 1));
        }
        out.add(ActionType.ME_VOY_AL_MAZO);
        return out;
    }

    // ------------------------------------------------------------------ acciones

    ActionResult playCard(int seat, int handIndex) {
        if (finished || match.isOver()) {
            return ActionResult.fail("La ronda terminó");
        }
        if (seat < 0 || seat >= n) {
            return ActionResult.fail("Asiento inválido");
        }
        if (pending != null) {
            return ActionResult.fail("Hay un canto esperando respuesta");
        }
        if (seat != turn) {
            return ActionResult.fail("No es tu turno");
        }
        if (canSingFlor(seat)) {
            return ActionResult.fail("Tenés flor: debés cantarla antes de jugar");
        }
        List<Card> h = hand.get(seat);
        if (handIndex < 0 || handIndex >= h.size()) {
            return ActionResult.fail("Carta inválida");
        }
        Card card = h.remove(handIndex);
        trickCards[trickIndex][seat] = card;
        playedInTrick++;
        match.emit(GameEvent.cardPlayed(seat, card, trickIndex));
        if (playedInTrick == n) {
            resolveTrick();
        } else {
            turn = (turn + 1) % n;
        }
        return ActionResult.ok();
    }

    ActionResult act(int seat, ActionType action) {
        if (action == null || action == ActionType.PLAY_CARD) {
            return ActionResult.fail("Acción inválida");
        }
        if (finished || match.isOver()) {
            return ActionResult.fail("La ronda terminó");
        }
        if (!legalActions(seat).contains(action)) {
            return ActionResult.fail("Jugada no permitida en este momento: " + action.getSpeech());
        }
        int team = teamOf(seat);
        match.emit(GameEvent.call(seat, action));

        if (action == ActionType.ME_VOY_AL_MAZO) {
            goToMazo(seat);
            return ActionResult.ok();
        }

        if (pending == null) {
            if (action.isTrucoCall()) {
                trucoEverCalled = true;
                pending = new Pending(PendingKind.TRUCO, seat, otherTeam(team), action.trucoLevel(), action);
            } else if (action.isEnvidoCall()) {
                startEnvido(seat, action);
            } else if (action == ActionType.FLOR) {
                singFlor(seat);
            }
            return ActionResult.ok();
        }

        switch (pending.kind) {
            case TRUCO:
                handleTrucoResponse(seat, action);
                break;
            case ENVIDO:
                handleEnvidoResponse(seat, action);
                break;
            case FLOR:
                handleFlorResponse(seat, action);
                break;
            default:
                break;
        }
        return ActionResult.ok();
    }

    // ------------------------------------------------------------------ truco

    private void handleTrucoResponse(int seat, ActionType action) {
        int team = teamOf(seat);
        Pending p = pending;
        if (action == ActionType.QUIERO) {
            trucoLevel = p.proposedLevel;
            trucoRaiseTeam = team; // solo quien aceptó puede subir
            pending = null;
        } else if (action == ActionType.NO_QUIERO) {
            int callerTeam = teamOf(p.callerSeat);
            pending = null;
            endRound(callerTeam, trucoValue(p.proposedLevel - 1), "No quiero (" + p.lastCall.getSpeech() + ")");
        } else if (action.isTrucoCall()) {
            // "Quiero retruco" / "Quiero vale cuatro": acepta y sube.
            trucoLevel = p.proposedLevel;
            pending = new Pending(PendingKind.TRUCO, seat, otherTeam(team), action.trucoLevel(), action);
        } else if (action.isEnvidoCall()) {
            suspendedTruco = p;
            pending = null;
            startEnvido(seat, action);
        } else if (action == ActionType.FLOR) {
            suspendedTruco = p;
            pending = null;
            singFlor(seat);
        }
    }

    private void goToMazo(int seat) {
        int team = teamOf(seat);
        int points;
        if (pending != null && pending.kind == PendingKind.TRUCO) {
            points = trucoValue(pending.proposedLevel - 1);
        } else {
            points = trucoValue(trucoLevel);
        }
        pending = null;
        suspendedTruco = null;
        endRound(otherTeam(team), points, "Se fue al mazo");
    }

    // ------------------------------------------------------------------ envido

    private void startEnvido(int seat, ActionType action) {
        envidoChain.clear();
        envidoChain.add(action);
        pending = new Pending(PendingKind.ENVIDO, seat, otherTeam(teamOf(seat)), 0, action);
    }

    private static int envidoStake(ActionType a) {
        return a == ActionType.REAL_ENVIDO ? 3 : 2;
    }

    private void handleEnvidoResponse(int seat, ActionType action) {
        int team = teamOf(seat);
        if (action == ActionType.FLOR) {
            // La flor anula el envido sin puntos.
            envidoChain.clear();
            pending = null;
            singFlor(seat);
            return;
        }
        if (action.isEnvidoCall()) {
            envidoChain.add(action);
            pending = new Pending(PendingKind.ENVIDO, seat, otherTeam(team), 0, action);
            return;
        }
        if (action == ActionType.QUIERO) {
            int winnerSeat = bestEnvidoSeat();
            int winTeam = teamOf(winnerSeat);
            int points;
            if (envidoChain.contains(ActionType.FALTA_ENVIDO)) {
                points = match.faltaPointsFor(winTeam);
            } else {
                points = 0;
                for (ActionType a : envidoChain) {
                    points += envidoStake(a);
                }
            }
            int value = rules.envidoPoints(dealt.get(winnerSeat));
            int[] tantos = new int[n];
            boolean[] tiene = new boolean[n];
            for (int s = 0; s < n; s++) {
                tantos[s] = rules.envidoPoints(dealt.get(s));
                tiene[s] = true;
            }
            cantarTantos(tantos, tiene, false);
            closeEnvido();
            match.emit(GameEvent.envidoResolved(winnerSeat, value, points));
            award(winTeam, points, "Envido (" + value + ")");
            return;
        }
        if (action == ActionType.NO_QUIERO) {
            int callerTeam = otherTeam(team);
            int points;
            if (envidoChain.size() == 1) {
                points = 1;
            } else {
                points = 0;
                for (int i = 0; i < envidoChain.size() - 1; i++) {
                    points += envidoStake(envidoChain.get(i));
                }
            }
            closeEnvido();
            award(callerTeam, points, "Envido no querido");
        }
    }

    /**
     * Los tantos se cantan como en la mesa de verdad, empezando por la mano: el primero dice su número; después,
     * cada rival del que va ganando o lo supera (y dice el suyo) o dice "son buenas" sin mostrar nada; los
     * compañeros del que va ganando no hablan. En empate gana el más cerca de la mano, así que empatar es
     * "son buenas".
     */
    private void cantarTantos(int[] tantos, boolean[] tiene, boolean flor) {
        int lider = -1;
        for (int k = 0; k < n; k++) {
            int s = (mano + k) % n;
            if (!tiene[s]) continue;
            if (lider < 0) {
                lider = s;
                match.emit(GameEvent.tantoDeclared(s, tantos[s], flor));
            } else if (teamOf(s) == teamOf(lider)) {
                continue;                                   // su compañero ya va ganando: se calla
            } else if (tantos[s] > tantos[lider]) {
                lider = s;
                match.emit(GameEvent.tantoDeclared(s, tantos[s], flor));
            } else {
                match.emit(GameEvent.tantoDeclared(s, GameEvent.SON_BUENAS, flor));
            }
        }
    }

    private void closeEnvido() {
        envidoClosed = true;
        envidoChain.clear();
        pending = suspendedTruco;
        suspendedTruco = null;
    }

    /** Mejor envido; en empate gana quien está más cerca de la mano. */
    private int bestEnvidoSeat() {
        int bestSeat = -1;
        int best = -1;
        for (int k = 0; k < n; k++) {
            int s = (mano + k) % n;
            int v = rules.envidoPoints(dealt.get(s));
            if (v > best) {
                best = v;
                bestSeat = s;
            }
        }
        return bestSeat;
    }

    // ------------------------------------------------------------------ flor

    private void singFlor(int seat) {
        int team = teamOf(seat);
        florSung[seat] = true;
        envidoClosed = true;
        envidoChain.clear();

        boolean contested = false;
        for (int s = 0; s < n; s++) {
            if (teamOf(s) != team && !florSung[s] && rules.hasFlor(dealt.get(s))) {
                contested = true;
                break;
            }
        }
        if (contested) {
            florStage = FlorStage.FLOR;
            contraflorRaised = false;
            pending = new Pending(PendingKind.FLOR, seat, otherTeam(team), 0, ActionType.FLOR);
        } else {
            pending = suspendedTruco;
            suspendedTruco = null;
            match.emit(GameEvent.florResolved(seat, team, -1, FLOR_POINTS));
            award(team, FLOR_POINTS, "Flor");
        }
    }

    private void handleFlorResponse(int seat, ActionType action) {
        int team = teamOf(seat);
        int callerTeam = otherTeam(team);
        switch (florStage) {
            case FLOR:
                if (action == ActionType.CONTRAFLOR) {
                    florStage = FlorStage.CONTRAFLOR;
                    contraflorRaised = true;
                    pending = new Pending(PendingKind.FLOR, seat, callerTeam, 0, action);
                } else if (action == ActionType.CONTRAFLOR_AL_RESTO) {
                    florStage = FlorStage.AL_RESTO;
                    pending = new Pending(PendingKind.FLOR, seat, callerTeam, 0, action);
                } else if (action == ActionType.CON_FLOR_ME_ACHICO) {
                    closeFlor();
                    award(callerTeam, FLOR_ACHICO_POINTS, "Con flor me achico");
                }
                break;
            case CONTRAFLOR:
                if (action == ActionType.QUIERO) {
                    resolveFlorComparison(CONTRAFLOR_POINTS, false);
                } else if (action == ActionType.NO_QUIERO || action == ActionType.CON_FLOR_ME_ACHICO) {
                    closeFlor();
                    award(callerTeam, FLOR_ACHICO_POINTS, "Contraflor no querida");
                } else if (action == ActionType.CONTRAFLOR_AL_RESTO) {
                    florStage = FlorStage.AL_RESTO;
                    pending = new Pending(PendingKind.FLOR, seat, callerTeam, 0, action);
                }
                break;
            case AL_RESTO:
                if (action == ActionType.QUIERO) {
                    resolveFlorComparison(0, true);
                } else if (action == ActionType.NO_QUIERO || action == ActionType.CON_FLOR_ME_ACHICO) {
                    closeFlor();
                    award(callerTeam, contraflorRaised ? CONTRAFLOR_POINTS : FLOR_ACHICO_POINTS,
                            "Contraflor al resto no querida");
                }
                break;
            default:
                break;
        }
    }

    private void resolveFlorComparison(int points, boolean alResto) {
        int bestSeat = -1;
        int best = -1;
        for (int k = 0; k < n; k++) {
            int s = (mano + k) % n;
            if (rules.hasFlor(dealt.get(s))) {
                int v = rules.florPoints(dealt.get(s));
                if (v > best) {
                    best = v;
                    bestSeat = s;
                }
            }
        }
        int[] tantos = new int[n];
        boolean[] tiene = new boolean[n];
        for (int s = 0; s < n; s++) {
            tiene[s] = rules.hasFlor(dealt.get(s));
            tantos[s] = tiene[s] ? rules.florPoints(dealt.get(s)) : 0;
        }
        cantarTantos(tantos, tiene, true);
        int winTeam = teamOf(bestSeat);
        int pts = alResto ? match.faltaPointsFor(winTeam) : points;
        closeFlor();
        match.emit(GameEvent.florResolved(bestSeat, winTeam, best, pts));
        award(winTeam, pts, alResto ? "Contraflor al resto (" + best + ")" : "Contraflor (" + best + ")");
    }

    private void closeFlor() {
        florClosed = true;
        florStage = null;
        pending = suspendedTruco;
        suspendedTruco = null;
    }

    // ------------------------------------------------------------------ bazas

    private void resolveTrick() {
        int t = trickIndex;
        int best = -1;
        int bestSeat = -1;
        boolean[] teamHasBest = new boolean[2];
        for (int k = 0; k < n; k++) {
            int s = (trickLeader + k) % n;
            int r = rules.trickRank(trickCards[t][s]);
            if (r > best) {
                best = r;
                bestSeat = s;
                teamHasBest[0] = false;
                teamHasBest[1] = false;
                teamHasBest[teamOf(s)] = true;
            } else if (r == best) {
                teamHasBest[teamOf(s)] = true;
            }
        }
        int result = (teamHasBest[0] && teamHasBest[1]) ? TRICK_PARDA : teamOf(bestSeat);
        trickResult[t] = result;
        trickWinnerSeat[t] = result == TRICK_PARDA ? -1 : bestSeat;
        match.emit(GameEvent.trickResolved(t, result, trickWinnerSeat[t]));

        if (t == 0) {
            envidoClosed = true;
            florClosed = true;
        }

        int winner = decideRoundByTricks();
        if (winner >= 0) {
            endRound(winner, trucoValue(trucoLevel), "Bazas");
            return;
        }
        trickIndex++;
        playedInTrick = 0;
        if (result != TRICK_PARDA) {
            trickLeader = bestSeat;
        }
        // En parda vuelve a salir quien salió en la baza anterior.
        turn = trickLeader;
    }

    /** Equipo ganador por bazas, o -1 si todavía no está definido. */
    int decideRoundByTricks() {
        return TrickRules.winner(trickResult[0], trickResult[1], trickResult[2], teamOf(mano));
    }

    // ------------------------------------------------------------------ puntaje y fin

    private void award(int team, int points, String reason) {
        if (points > 0) {
            match.award(team, points, reason);
        }
        if (match.isOver()) {
            finished = true;
            pending = null;
            suspendedTruco = null;
        }
    }

    private void endRound(int team, int points, String reason) {
        if (finished) {
            return;
        }
        finished = true;
        winnerTeam = team;
        pending = null;
        suspendedTruco = null;
        match.emit(GameEvent.roundEnded(team));
        award(team, points, reason);
    }

    // ------------------------------------------------------------------ getters

    int getDealer() { return dealer; }
    int getMano() { return mano; }
    Card getMuestra() { return muestra; }
    CardRules getRules() { return rules; }
    int getTurn() { return turn; }
    int getTrickLeader() { return trickLeader; }
    boolean isFlorSung(int seat) { return florSung[seat]; }
    int getTrickIndex() { return trickIndex; }
    int getTrucoLevel() { return trucoLevel; }
    boolean isFinished() { return finished; }
    int getWinnerTeam() { return winnerTeam; }
    Pending getPending() { return pending; }
    FlorStage getFlorStage() { return florStage; }

    List<Card> getHand(int seat) {
        return Collections.unmodifiableList(new ArrayList<Card>(hand.get(seat)));
    }

    List<Card> getDealt(int seat) {
        return Collections.unmodifiableList(new ArrayList<Card>(dealt.get(seat)));
    }

    Card getTrickCard(int trick, int seat) {
        return trickCards[trick][seat];
    }

    int getTrickResult(int trick) {
        return trickResult[trick];
    }

    int getTrickWinnerSeat(int trick) {
        return trickWinnerSeat[trick];
    }
}
