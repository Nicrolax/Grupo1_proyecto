package com.trucoya.entity;

/**
 * Aspecto de cada variante de gaucho (generado por tools/generar_gaucho.py: no editar a mano).
 * Fila = {@link GauchoEntity.Rol} (mismo orden), columna = variante.
 * Textura: textures/entity/gaucho_&lt;rol&gt;_&lt;variante&gt;.png
 */
public final class GauchoAspectos {

    public static final int MUJER = 1;
    public static final int SOMBRERO = 2;
    public static final int ALA = 4;
    public static final int PELO_LARGO = 8;
    public static final int POLLERA = 16;
    public static final int GUITARRA = 32;

    private static final int[][] FLAGS = {
            {6, 6, 6, 6, 6, 6}, // paisano
            {2, 2, 25}, // cantinero
            {6, 6, 6, 6}, // forastero
            {6}, // vizcacha
            {25, 31, 25, 25}, // paisana
            {6, 6, 6, 6}, // matrero
            {0, 6, 2, 6}, // peon
            {38, 38}, // payador
            {0, 0, 0, 0}, // fundador
            {6}, // mandinga
            {6}, // ledesma
    };

    private GauchoAspectos() {
    }

    public static int variantes(GauchoEntity.Rol rol) {
        return rol.ordinal() < FLAGS.length ? FLAGS[rol.ordinal()].length : 1;
    }

    /** Variante válida (las guardadas fuera de rango se reducen con módulo). */
    public static int normalizar(GauchoEntity.Rol rol, int variante) {
        int n = variantes(rol);
        return Math.floorMod(Math.max(0, variante), n);
    }

    public static int flags(GauchoEntity.Rol rol, int variante) {
        if (rol.ordinal() >= FLAGS.length) return SOMBRERO | ALA;
        return FLAGS[rol.ordinal()][normalizar(rol, variante)];
    }

    public static boolean tiene(GauchoEntity.Rol rol, int variante, int flag) {
        return (flags(rol, variante) & flag) != 0;
    }
}
