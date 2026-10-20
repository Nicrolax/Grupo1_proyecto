package com.trucoya.core;

/** Parámetros acordados antes de la partida (panel de configuración, Fase 3). Inmutable. */
public final class MatchConfig {

    /** Cómo se valora la Falta Envido y la Contraflor al Resto. */
    public enum FaltaMode {
        /**
         * Regla uruguaya (por defecto): se juega el partido entero, salvo que un equipo esté en
         * Las Malas y el otro en Las Buenas; en ese caso vale lo que le falta al que va ganando.
         */
        PARTIDO_SALVO_MALAS_Y_BUENAS,
        /** Alternativa: siempre vale lo que le falta al que va ganando. */
        LEADER_NEEDS_TO_WIN
    }

    private final int players;
    private final int targetScore;
    private final boolean florEnabled;
    private final FaltaMode faltaMode;

    public MatchConfig(int players, int targetScore, boolean florEnabled, FaltaMode faltaMode) {
        if (players != 2 && players != 4) {
            throw new IllegalArgumentException("Modalidades soportadas: 1v1 (2 jugadores) o 2v2 (4 jugadores)");
        }
        if (targetScore != 30 && targetScore != 40 && targetScore != 20) {
            throw new IllegalArgumentException("El partido se juega a 30 o a 40 puntos (a 20 en los torneos)");
        }
        this.players = players;
        this.targetScore = targetScore;
        this.florEnabled = florEnabled;
        this.faltaMode = faltaMode == null ? FaltaMode.PARTIDO_SALVO_MALAS_Y_BUENAS : faltaMode;
    }

    public MatchConfig(int players, int targetScore, boolean florEnabled) {
        this(players, targetScore, florEnabled, FaltaMode.PARTIDO_SALVO_MALAS_Y_BUENAS);
    }

    public static MatchConfig defaults1v1() {
        return new MatchConfig(2, 30, true);
    }

    public static MatchConfig defaults2v2() {
        return new MatchConfig(4, 30, true);
    }

    public int getPlayers() {
        return players;
    }

    public int getTargetScore() {
        return targetScore;
    }

    /** Puntos de cada etapa: 15 malas + 15 buenas (a 30), 20 + 20 (a 40) o 10 + 10 (a 20, partido corto de torneo). */
    public int getHalf() {
        return targetScore / 2;
    }

    public boolean isFlorEnabled() {
        return florEnabled;
    }

    public FaltaMode getFaltaMode() {
        return faltaMode;
    }
}
