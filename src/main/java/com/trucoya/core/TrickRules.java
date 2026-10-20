package com.trucoya.core;

/** Reglas puras para decidir la ronda por bazas (compartidas por el motor y la IA). */
public final class TrickRules {

    public static final int NOT_PLAYED = -2;
    public static final int PARDA = -1;

    private TrickRules() {
    }

    /**
     * Equipo ganador de la ronda según las bazas, o -1 si todavía no está definido.
     * 1ª parda: decide la 2ª · ganada + parda: gana la ganada · 1-1 o parda-parda: decide la 3ª ·
     * 3ª parda: gana quien ganó la 1ª · todas pardas: la mano.
     */
    public static int winner(int r1, int r2, int r3, int manoTeam) {
        if (r2 == NOT_PLAYED) {
            return -1;
        }
        if (r3 == NOT_PLAYED) {
            if (r1 >= 0 && r1 == r2) return r1;
            if (r1 == PARDA && r2 >= 0) return r2;
            if (r1 >= 0 && r2 == PARDA) return r1;
            return -1;
        }
        if (r3 >= 0) {
            return r3;
        }
        if (r1 >= 0) {
            return r1;
        }
        return manoTeam;
    }
}
