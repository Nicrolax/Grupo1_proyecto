package com.trucoya.core;

/**
 * Señas del Truco (rueda de la tecla G). En 2v2 el compañero las recibe con discreción;
 * cada seña tiene una probabilidad de ser "descubierta" por los rivales.
 */
public enum Sena {
    DOS("Guiño de ojo", "el 2 de la muestra"),
    CUATRO("Morder el labio inferior", "el 4 de la muestra"),
    CINCO("Levantar las cejas", "el 5 de la muestra"),
    CABALLO("Inflar un cachete", "el caballo de la muestra"),
    SOTA("Fruncir la nariz", "la sota de la muestra"),
    ANCHO_ESPADA("Torcer la boca a la derecha", "el ancho de espadas"),
    ANCHO_BASTO("Torcer la boca a la izquierda", "el ancho de bastos"),
    SIETE_ESPADA("Apretar los labios", "el 7 de espadas"),
    SIETE_ORO("Asomar la punta de la lengua", "el 7 de oro"),
    TRES("Mostrar tres dedos sobre la mesa", "un 3"),
    FLOR("Inflar los dos cachetes", "flor"),
    NADA("Cerrar los ojos", "nada que valga");

    private final String gesture;
    private final String meaning;

    Sena(String gesture, String meaning) {
        this.gesture = gesture;
        this.meaning = meaning;
    }

    /** Gesto visible (lo que ven todos si la seña es descubierta). */
    public String getGesture() {
        return gesture;
    }

    /** Significado (solo lo interpreta el compañero). */
    public String getMeaning() {
        return meaning;
    }

    public static Sena byOrdinal(int ordinal) {
        Sena[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
