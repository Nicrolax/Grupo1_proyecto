package com.trucoya.core;

/**
 * Evento emitido por el motor. La capa de Minecraft los consume para reproducir voces,
 * mostrar globos de texto, animar cartas y sincronizar a los clientes.
 *
 * Campos no aplicables valen -1 / null. Ningún evento revela cartas ocultas: solo se
 * informan cartas jugadas, la muestra y los tantos de envido/flor que cada uno canta al resolver
 * (como en la mesa de verdad, donde se dicen en voz alta).
 */
public final class GameEvent {

    public enum Type {
        ROUND_STARTED,     // seat = mano, value = repartidor, card = muestra
        CARD_PLAYED,       // seat, card, trick
        CALL,              // seat, action (canto o respuesta: usar para voz y globo)
        TRICK_RESOLVED,    // trick, team ganador (-1 parda), seat ganador (-1 parda)
        TANTO_DECLARED,    // seat, value = los tantos que canta, action = ENVIDO o FLOR (se emite antes de *_RESOLVED)
        ENVIDO_RESOLVED,   // seat/team ganador, value = sus puntos de envido, points = puntos en juego
        FLOR_RESOLVED,     // seat/team ganador (seat -1 si no hubo comparación), value = puntos de flor, points
        POINTS_AWARDED,    // team, points, text = motivo
        STAGE_CHANGED,     // team pasó de Las Malas a Las Buenas
        ROUND_ENDED,       // team ganador de la ronda (por bazas, no quiero o mazo)
        MATCH_ENDED        // team ganador del partido
    }

    private final Type type;
    private final int seat;
    private final int team;
    private final Card card;
    private final ActionType action;
    private final int trick;
    private final int value;
    private final int points;
    private final String text;

    private GameEvent(Type type, int seat, int team, Card card, ActionType action,
                      int trick, int value, int points, String text) {
        this.type = type;
        this.seat = seat;
        this.team = team;
        this.card = card;
        this.action = action;
        this.trick = trick;
        this.value = value;
        this.points = points;
        this.text = text;
    }

    static GameEvent roundStarted(int mano, int dealer, Card muestra) {
        return new GameEvent(Type.ROUND_STARTED, mano, mano % 2, muestra, null, -1, dealer, 0, null);
    }

    static GameEvent cardPlayed(int seat, Card card, int trick) {
        return new GameEvent(Type.CARD_PLAYED, seat, seat % 2, card, ActionType.PLAY_CARD, trick, -1, 0, null);
    }

    static GameEvent call(int seat, ActionType action) {
        return new GameEvent(Type.CALL, seat, seat % 2, null, action, -1, -1, 0, action.getSpeech());
    }

    static GameEvent trickResolved(int trick, int team, int seat) {
        return new GameEvent(Type.TRICK_RESOLVED, seat, team, null, null, trick, -1, 0, team < 0 ? "Parda" : null);
    }

    static GameEvent tantoDeclared(int seat, int value, boolean flor) {
        return new GameEvent(Type.TANTO_DECLARED, seat, seat % 2, null, flor ? ActionType.FLOR : ActionType.ENVIDO,
                -1, value, 0, null);
    }

    static GameEvent envidoResolved(int seat, int value, int points) {
        return new GameEvent(Type.ENVIDO_RESOLVED, seat, seat % 2, null, null, -1, value, points, null);
    }

    static GameEvent florResolved(int seat, int team, int value, int points) {
        return new GameEvent(Type.FLOR_RESOLVED, seat, team, null, null, -1, value, points, null);
    }

    static GameEvent pointsAwarded(int team, int points, String reason) {
        return new GameEvent(Type.POINTS_AWARDED, -1, team, null, null, -1, -1, points, reason);
    }

    static GameEvent stageChanged(int team) {
        return new GameEvent(Type.STAGE_CHANGED, -1, team, null, null, -1, -1, 0, "Las Buenas");
    }

    static GameEvent roundEnded(int team) {
        return new GameEvent(Type.ROUND_ENDED, -1, team, null, null, -1, -1, 0, null);
    }

    static GameEvent matchEnded(int team) {
        return new GameEvent(Type.MATCH_ENDED, -1, team, null, null, -1, -1, 0, null);
    }

    public Type getType() { return type; }
    public int getSeat() { return seat; }
    public int getTeam() { return team; }
    public Card getCard() { return card; }
    public ActionType getAction() { return action; }
    public int getTrick() { return trick; }
    public int getValue() { return value; }
    public int getPoints() { return points; }
    public String getText() { return text; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type.name());
        if (seat >= 0) sb.append(" asiento=").append(seat);
        if (team >= 0) sb.append(" equipo=").append(team);
        if (card != null) sb.append(" carta=").append(card);
        if (action != null && type == Type.CALL) sb.append(" accion=").append(action);
        if (trick >= 0) sb.append(" baza=").append(trick + 1);
        if (value >= 0) sb.append(" valor=").append(value);
        if (points != 0) sb.append(" puntos=").append(points);
        if (text != null) sb.append(" (").append(text).append(')');
        return sb.toString();
    }
}
