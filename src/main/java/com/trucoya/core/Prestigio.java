package com.trucoya.core;

import java.util.Random;

/**
 * Prestigio Criollo: reputación del jugador. Se gana ganando partidos (más contra rivales
 * de mayor nivel), cantando flor y con faroles exitosos; se pierde con las derrotas.
 */
public final class Prestigio {

    /** Puntos mínimos para cada nivel (índice 0 = nivel 1). */
    public static final int[] UMBRAL = {0, 60, 160, 320, 600};
    public static final String[] RANGO = {
        "Aprendiz de Pulpería", "Parroquiano", "Tigre de Boliche", "Taita del Truco", "Leyenda del Tapete"
    };

    public static final int DERROTA = -10;
    public static final int FLOR = 2;
    public static final int FAROL = 3;
    public static final int MAX_NIVEL = 5;

    private Prestigio() {
    }

    /** Nivel 1..5 para una cantidad de puntos. */
    public static int nivel(int puntos) {
        int n = 1;
        for (int i = 0; i < UMBRAL.length; i++) {
            if (puntos >= UMBRAL[i]) {
                n = i + 1;
            }
        }
        return n;
    }

    public static String rango(int nivel) {
        return RANGO[Math.max(1, Math.min(MAX_NIVEL, nivel)) - 1];
    }

    /** Puntos que faltan para el próximo nivel (0 si ya es Leyenda). */
    public static int faltanParaSubir(int puntos) {
        int n = nivel(puntos);
        return n >= MAX_NIVEL ? 0 : UMBRAL[n] - puntos;
    }

    /** Puntos por ganar un partido contra un rival de ese nivel (1..5). */
    public static int victoria(int nivelRival) {
        return 20 + 5 * Math.max(1, Math.min(MAX_NIVEL, nivelRival));
    }

    public static int aplicar(int actual, int delta) {
        return Math.max(0, actual + delta);
    }

    /** Nivel del Gaucho forastero que viene a desafiar: cerca del nivel del jugador (nunca 5). */
    public static int nivelForastero(int nivelJugador, Random r) {
        int n = nivelJugador + r.nextInt(3) - 1;
        return Math.max(1, Math.min(4, n));
    }

    /** Texto corto: "Nivel 3 · Tigre de Boliche (175 pts)". */
    public static String describir(int puntos) {
        int n = nivel(puntos);
        return "Nivel " + n + " · " + rango(n) + " (" + puntos + " pts)";
    }
}
