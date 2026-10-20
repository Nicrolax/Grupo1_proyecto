package com.trucoya.core;

/**
 * Carta de la baraja española de 40 cartas (sin 8 ni 9).
 *
 * Las cartas son instancias únicas e inmutables (patrón flyweight): existe exactamente
 * un objeto por carta, lo que permite compararlas por referencia y serializarlas por id
 * (0..39) en paquetes de red y NBT sin riesgo de duplicación.
 */
public final class Card {

    /** Números válidos en el mazo de Truco (sin 8 ni 9). */
    public static final int[] NUMBERS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12};
    public static final int DECK_SIZE = 40;

    private static final Card[] ALL = new Card[DECK_SIZE];

    static {
        int id = 0;
        for (Suit suit : Suit.values()) {
            for (int number : NUMBERS) {
                ALL[id] = new Card(number, suit, id);
                id++;
            }
        }
    }

    private final int number;
    private final Suit suit;
    private final int id;

    private Card(int number, Suit suit, int id) {
        this.number = number;
        this.suit = suit;
        this.id = id;
    }

    /** Devuelve la carta única para número y palo. */
    public static Card of(int number, Suit suit) {
        if (suit == null) {
            throw new IllegalArgumentException("Palo nulo");
        }
        int index = indexOfNumber(number);
        if (index < 0) {
            throw new IllegalArgumentException("Número inválido para el mazo de Truco: " + number);
        }
        return ALL[suit.ordinal() * NUMBERS.length + index];
    }

    /** Devuelve la carta por id (0..39); usado para deserializar. */
    public static Card byId(int id) {
        if (id < 0 || id >= DECK_SIZE) {
            throw new IllegalArgumentException("Id de carta inválido: " + id);
        }
        return ALL[id];
    }

    /** Copia del mazo completo en orden canónico. */
    public static Card[] all() {
        return ALL.clone();
    }

    private static int indexOfNumber(int number) {
        for (int i = 0; i < NUMBERS.length; i++) {
            if (NUMBERS[i] == number) {
                return i;
            }
        }
        return -1;
    }

    public int getNumber() {
        return number;
    }

    public Suit getSuit() {
        return suit;
    }

    public int getId() {
        return id;
    }

    /** Sota (10), Caballo (11) o Rey (12). */
    public boolean isFigure() {
        return number >= 10;
    }

    /** Valor base para envido/flor de una carta que NO es pieza: 1..7 su número, figuras 0. */
    public int baseEnvidoValue() {
        return isFigure() ? 0 : number;
    }

    public String getNumberName() {
        switch (number) {
            case 1: return "Ancho";
            case 10: return "Sota";
            case 11: return "Caballo";
            case 12: return "Rey";
            default: return String.valueOf(number);
        }
    }

    /** Clave estable para texturas/traducciones, p. ej. "espada_1". */
    public String getKey() {
        return suit.name().toLowerCase(java.util.Locale.ROOT) + "_" + number;
    }

    @Override
    public String toString() {
        return number + " de " + suit.getDisplayName();
    }
}
