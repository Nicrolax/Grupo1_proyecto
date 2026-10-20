package com.trucoya.core;

/** Palos de la baraja española. */
public enum Suit {
    ESPADA("Espadas"),
    BASTO("Bastos"),
    ORO("Oro"),
    COPA("Copas");

    private final String displayName;

    Suit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
