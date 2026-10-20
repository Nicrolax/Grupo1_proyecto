package com.trucoya.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluador de cartas para una ronda, según la Muestra.
 *
 * <h3>Piezas (palo de la muestra)</h3>
 * <pre>
 *  Pieza   Envido  Valor de suma  Jerarquía
 *  2       30      10             1ª (la más alta del juego)
 *  4       29       9             2ª
 *  5       28       8             3ª
 *  11      27       7             4ª
 *  10      27       7             5ª
 * </pre>
 * Regla "muestra mata pieza": si la muestra es 2, 4, 5, 11 o 10, el Rey (12) del mismo palo
 * hereda el valor y la jerarquía de esa pieza.
 *
 * <h3>Jerarquía restante</h3>
 * Ancho de Espadas &gt; Ancho de Bastos &gt; 7 de Espadas &gt; 7 de Oro &gt; 3 &gt; 2 &gt; 1 (falsos)
 * &gt; 12 &gt; 11 &gt; 10 &gt; 7 (falsos) &gt; 6 &gt; 5 &gt; 4.
 *
 * <h3>Envido</h3>
 * <ul>
 *   <li>Con pieza: valor de la pieza (30..27) + la carta de mayor valor entre las otras dos
 *       (una pieza acompañante suma su valor de suma 10/9/8/7).</li>
 *   <li>Sin pieza: dos cartas del mismo palo = 20 + ambos valores; si no, la carta más alta.</li>
 *   <li>Figuras (10, 11, 12 que no son pieza) valen 0.</li>
 * </ul>
 *
 * <h3>Flor</h3>
 * Tres cartas del mismo palo; una pieza + dos cartas del mismo palo; dos piezas + cualquier
 * carta; o tres piezas (la pieza vale como cualquier palo). Puntos = 20 + suma de valores
 * (pieza: 10/9/8/7/7). Máximo posible: 2+4+5 de la muestra = 47.
 */
public final class CardRules {

    private static final int[] PIEZA_NUMBERS = {2, 4, 5, 11, 10};

    private final Card muestra;

    public CardRules(Card muestra) {
        if (muestra == null) {
            throw new IllegalArgumentException("La muestra no puede ser nula");
        }
        this.muestra = muestra;
    }

    public Card getMuestra() {
        return muestra;
    }

    public static boolean isPiezaNumber(int number) {
        for (int n : PIEZA_NUMBERS) {
            if (n == number) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rol de pieza que cumple la carta: 2, 4, 5, 11 o 10; 0 si no es pieza.
     * El Rey de la muestra devuelve el número de la muestra cuando la muestra es pieza.
     */
    public int piezaRole(Card card) {
        if (card.getSuit() != muestra.getSuit() || card == muestra) {
            return 0;
        }
        int n = card.getNumber();
        if (isPiezaNumber(n)) {
            return n;
        }
        if (n == 12 && isPiezaNumber(muestra.getNumber())) {
            return muestra.getNumber();
        }
        return 0;
    }

    public boolean isPieza(Card card) {
        return piezaRole(card) != 0;
    }

    /** Valor de envido de la pieza sola (30, 29, 28, 27, 27); 0 si no es pieza. */
    public int piezaEnvidoValue(Card card) {
        int role = piezaRole(card);
        return role == 0 ? 0 : 20 + piezaSumValue(role);
    }

    private static int piezaSumValue(int role) {
        switch (role) {
            case 2: return 10;
            case 4: return 9;
            case 5: return 8;
            case 11:
            case 10: return 7;
            default: return 0;
        }
    }

    /** Valor que aporta la carta a una suma de envido/flor (pieza: 10/9/8/7; resto: 1..7, figuras 0). */
    public int sumValue(Card card) {
        int role = piezaRole(card);
        return role != 0 ? piezaSumValue(role) : card.baseEnvidoValue();
    }

    /** Jerarquía para ganar bazas: número mayor gana. Rango 1 (4 falso) .. 19 (2 de la muestra). */
    public int trickRank(Card card) {
        int role = piezaRole(card);
        switch (role) {
            case 2: return 19;
            case 4: return 18;
            case 5: return 17;
            case 11: return 16;
            case 10: return 15;
            default: break;
        }
        int n = card.getNumber();
        Suit s = card.getSuit();
        if (n == 1 && s == Suit.ESPADA) return 14;
        if (n == 1 && s == Suit.BASTO) return 13;
        if (n == 7 && s == Suit.ESPADA) return 12;
        if (n == 7 && s == Suit.ORO) return 11;
        switch (n) {
            case 3: return 10;
            case 2: return 9;
            case 1: return 8;
            case 12: return 7;
            case 11: return 6;
            case 10: return 5;
            case 7: return 4;
            case 6: return 3;
            case 5: return 2;
            case 4: return 1;
            default: throw new IllegalStateException("Carta inesperada: " + card);
        }
    }

    /** Compara dos cartas por jerarquía de truco (positivo si a gana a b, 0 si empardan). */
    public int compare(Card a, Card b) {
        return Integer.compare(trickRank(a), trickRank(b));
    }

    /** ¿La mano (3 cartas) tiene Flor? */
    public boolean hasFlor(List<Card> hand) {
        if (hand.size() != 3) {
            return false;
        }
        List<Card> normals = new ArrayList<Card>(3);
        int piezas = 0;
        for (Card c : hand) {
            if (isPieza(c)) {
                piezas++;
            } else {
                normals.add(c);
            }
        }
        if (piezas >= 2) {
            return true;
        }
        if (piezas == 1) {
            return normals.get(0).getSuit() == normals.get(1).getSuit();
        }
        return normals.get(0).getSuit() == normals.get(1).getSuit()
                && normals.get(1).getSuit() == normals.get(2).getSuit();
    }

    /** Puntos de Flor: 20 + suma de valores. Solo significativo si {@link #hasFlor}. */
    public int florPoints(List<Card> hand) {
        int sum = 20;
        for (Card c : hand) {
            sum += sumValue(c);
        }
        return sum;
    }

    /** Puntos de envido de la mano original (1 a 3 cartas). */
    public int envidoPoints(List<Card> hand) {
        int best = 0;
        // Caso con pieza: pieza de mayor valor + la mejor de las restantes.
        int bestPiezaIndex = -1;
        int bestPiezaValue = -1;
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (isPieza(c)) {
                int v = piezaEnvidoValue(c);
                if (v > bestPiezaValue) {
                    bestPiezaValue = v;
                    bestPiezaIndex = i;
                }
            }
        }
        if (bestPiezaIndex >= 0) {
            int bestOther = 0;
            for (int i = 0; i < hand.size(); i++) {
                if (i != bestPiezaIndex) {
                    bestOther = Math.max(bestOther, sumValue(hand.get(i)));
                }
            }
            best = bestPiezaValue + bestOther;
        }
        // Caso sin pieza: pares del mismo palo o carta suelta.
        for (int i = 0; i < hand.size(); i++) {
            Card a = hand.get(i);
            if (isPieza(a)) {
                continue;
            }
            best = Math.max(best, a.baseEnvidoValue());
            for (int j = i + 1; j < hand.size(); j++) {
                Card b = hand.get(j);
                if (!isPieza(b) && a.getSuit() == b.getSuit()) {
                    best = Math.max(best, 20 + a.baseEnvidoValue() + b.baseEnvidoValue());
                }
            }
        }
        return best;
    }
}
