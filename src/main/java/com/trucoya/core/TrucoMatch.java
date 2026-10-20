package com.trucoya.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Partido de Truco Uruguayo: fachada pública del motor (Fase 1).
 *
 * <p>Uso desde el servidor de Minecraft (único dueño del estado):</p>
 * <pre>
 *   TrucoMatch m = new TrucoMatch(MatchConfig.defaults1v1(), new Random());
 *   m.start();
 *   m.legalActions(seat);          // botones habilitados para ese jugador
 *   m.act(seat, ActionType.TRUCO); // canto o respuesta
 *   m.playCard(seat, indice);      // jugar carta de la mano
 *   m.drainEvents();               // eventos para audio, globos y sincronización
 *   m.viewFor(seat);               // vista privada para enviar al cliente
 *   m.startNextRound();            // tras ROUND_ENDED (el servidor decide la pausa)
 * </pre>
 *
 * <p>No es thread-safe a propósito: en Forge toda llamada debe hacerse en el hilo del
 * servidor (p. ej. dentro de {@code NetworkEvent.Context#enqueueWork}).</p>
 */
public final class TrucoMatch {

    private final MatchConfig config;
    private final Random random;
    private final int[] score = new int[2];
    private final List<GameEvent> events = new ArrayList<GameEvent>();

    private Round round;
    private int dealer = -1;
    private boolean started;
    private boolean over;
    private int winner = -1;
    private int roundsPlayed;

    public TrucoMatch(MatchConfig config, Random random) {
        if (config == null || random == null) {
            throw new IllegalArgumentException("Configuración y Random son obligatorios");
        }
        this.config = config;
        this.random = random;
    }

    public TrucoMatch(MatchConfig config, long seed) {
        this(config, new Random(seed));
    }

    // ------------------------------------------------------------------ ciclo de vida

    /** Sortea el primer repartidor y reparte la primera ronda. */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        dealer = random.nextInt(config.getPlayers());
        round = new Round(this, dealer, new Deck(random));
        roundsPlayed = 1;
    }

    /**
     * Comienza una ronda con repartidor y mazo dados (pruebas, repeticiones, depuración).
     * Si la partida no había comenzado, la marca como iniciada.
     */
    public ActionResult startRound(int dealerSeat, Deck deck) {
        if (over) {
            return ActionResult.fail("El partido terminó");
        }
        if (round != null && !round.isFinished()) {
            return ActionResult.fail("La ronda actual no terminó");
        }
        if (dealerSeat < 0 || dealerSeat >= config.getPlayers()) {
            return ActionResult.fail("Repartidor inválido");
        }
        started = true;
        dealer = dealerSeat;
        round = new Round(this, dealer, deck);
        roundsPlayed++;
        return ActionResult.ok();
    }

    /** Pasa el mazo al siguiente jugador y reparte. */
    public ActionResult startNextRound() {
        if (!started) {
            return ActionResult.fail("El partido no comenzó");
        }
        if (over) {
            return ActionResult.fail("El partido terminó");
        }
        if (round != null && !round.isFinished()) {
            return ActionResult.fail("La ronda actual no terminó");
        }
        dealer = (dealer + 1) % config.getPlayers();
        round = new Round(this, dealer, new Deck(random));
        roundsPlayed++;
        return ActionResult.ok();
    }

    // ------------------------------------------------------------------ acciones

    public List<ActionType> legalActions(int seat) {
        if (round == null || over) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(round.legalActions(seat));
    }

    public boolean isLegal(int seat, ActionType action) {
        return legalActions(seat).contains(action);
    }

    public ActionResult act(int seat, ActionType action) {
        if (round == null) {
            return ActionResult.fail("El partido no comenzó");
        }
        if (over) {
            return ActionResult.fail("El partido terminó");
        }
        return round.act(seat, action);
    }

    public ActionResult playCard(int seat, int handIndex) {
        if (round == null) {
            return ActionResult.fail("El partido no comenzó");
        }
        if (over) {
            return ActionResult.fail("El partido terminó");
        }
        return round.playCard(seat, handIndex);
    }

    /** Juega una carta identificándola por id (más robusto ante desfasajes de índice en red). */
    public ActionResult playCardById(int seat, int cardId) {
        if (round == null || seat < 0 || seat >= config.getPlayers()) {
            return ActionResult.fail("Jugada inválida");
        }
        List<Card> h = round.getHand(seat);
        for (int i = 0; i < h.size(); i++) {
            if (h.get(i).getId() == cardId) {
                return playCard(seat, i);
            }
        }
        return ActionResult.fail("Esa carta no está en tu mano");
    }

    // ------------------------------------------------------------------ puntaje

    void award(int team, int points, String reason) {
        if (over || points <= 0) {
            return;
        }
        int half = config.getHalf();
        int before = score[team];
        score[team] = Math.min(config.getTargetScore(), before + points);
        emit(GameEvent.pointsAwarded(team, score[team] - before, reason));
        if (before <= half && score[team] > half && score[team] < config.getTargetScore()) {
            emit(GameEvent.stageChanged(team));
        }
        if (score[team] >= config.getTargetScore()) {
            over = true;
            winner = team;
            emit(GameEvent.matchEnded(team));
        }
    }

    /** Puntos que se lleva {@code winnerTeam} con la Falta Envido / Contraflor al Resto. */
    int faltaPointsFor(int winnerTeam) {
        int target = config.getTargetScore();
        int leader = Math.max(score[0], score[1]);
        boolean sameStage = isInBuenas(0) == isInBuenas(1);
        if (config.getFaltaMode() == MatchConfig.FaltaMode.PARTIDO_SALVO_MALAS_Y_BUENAS && sameStage) {
            return target - score[winnerTeam]; // se juega el partido
        }
        return Math.max(1, target - leader);
    }

    void emit(GameEvent event) {
        events.add(event);
    }

    /** Devuelve y vacía la cola de eventos pendientes. */
    public List<GameEvent> drainEvents() {
        List<GameEvent> out = new ArrayList<GameEvent>(events);
        events.clear();
        return out;
    }

    // ------------------------------------------------------------------ consultas

    public MatchConfig getConfig() { return config; }
    public boolean isStarted() { return started; }
    public boolean isOver() { return over; }
    public int getWinner() { return winner; }
    public int getScore(int team) { return score[team]; }
    public int getRoundsPlayed() { return roundsPlayed; }
    public boolean isRoundFinished() { return round == null || round.isFinished(); }
    public int getRoundWinner() { return round == null ? -1 : round.getWinnerTeam(); }
    public int getDealer() { return round == null ? -1 : round.getDealer(); }
    public int getMano() { return round == null ? -1 : round.getMano(); }
    public int getTurn() { return round == null ? -1 : round.getTurn(); }
    public Card getMuestra() { return round == null ? null : round.getMuestra(); }
    public CardRules getRules() { return round == null ? null : round.getRules(); }
    public int getTrickIndex() { return round == null ? -1 : round.getTrickIndex(); }
    public int getTrucoLevel() { return round == null ? 0 : round.getTrucoLevel(); }
    /** Quién salió (o sale) en la baza actual. */
    public int getTrickLeader() { return round == null ? -1 : round.getTrickLeader(); }
    /** Puntos de la Falta Envido si la gana {@code team} (para la IA y la GUI). */
    public int faltaValueFor(int team) { return faltaPointsFor(team); }

    /** Mano actual del jugador. Solo para uso del servidor (no enviar cartas ajenas). */
    public List<Card> getHand(int seat) {
        return round == null ? Collections.<Card>emptyList() : round.getHand(seat);
    }

    /** Las tres cartas repartidas al jugador (para envido/flor). Solo servidor. */
    public List<Card> getDealtHand(int seat) {
        return round == null ? Collections.<Card>emptyList() : round.getDealt(seat);
    }

    public Card getTrickCard(int trick, int seat) {
        return round == null ? null : round.getTrickCard(trick, seat);
    }

    public int getTrickResult(int trick) {
        return round == null ? Round.TRICK_NOT_PLAYED : round.getTrickResult(trick);
    }

    public boolean hasPendingCall() {
        return round != null && round.getPending() != null;
    }

    /** Equipo que debe responder el canto pendiente, o -1. */
    public int getPendingAwaitingTeam() {
        return hasPendingCall() ? round.getPending().awaitingTeam : -1;
    }

    /** Último canto que espera respuesta, o null. */
    public ActionType getPendingCall() {
        return hasPendingCall() ? round.getPending().lastCall : null;
    }

    public boolean isInBuenas(int team) {
        return score[team] > config.getHalf();
    }

    /** Texto del anotador: "7 malas", "3 buenas". */
    public String stageLabel(int team) {
        int half = config.getHalf();
        int s = score[team];
        if (s <= half) {
            return s + (s == 1 ? " mala" : " malas");
        }
        int b = s - half;
        return b + (b == 1 ? " buena" : " buenas");
    }

    /** Vista privada para el jugador en {@code seat}: nunca incluye cartas ocultas ajenas. */
    public PlayerView viewFor(int seat) {
        return new PlayerView(this, seat);
    }
}
