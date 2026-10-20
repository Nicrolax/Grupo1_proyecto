package com.trucoya.core;

/** Voces de los cantos (cada una con 5 tomas por canto, que suenan al azar). */
public enum Voz {
    HOMBRE, GAUCHO, VIEJO, MUJER, PAISANA,
    /** El Mandinga (la Salamanca). */
    DIABLO;

    public String clave() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Voz porNombre(String s) {
        for (Voz v : values()) {
            if (v.clave().equalsIgnoreCase(s)) return v;
        }
        return HOMBRE;
    }

    public static Voz porNumero(int i) {
        Voz[] all = values();
        return i >= 0 && i < all.length ? all[i] : HOMBRE;
    }
}
