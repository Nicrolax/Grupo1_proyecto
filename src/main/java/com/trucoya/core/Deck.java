package com.trucoya.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Mazo de 40 cartas. Cada ronda usa un mazo nuevo y barajado. */
public final class Deck {

    private final List<Card> cards;
    private int position;

    public Deck(Random random) {
        cards = new ArrayList<Card>(Card.DECK_SIZE);
        Collections.addAll(cards, Card.all());
        Collections.shuffle(cards, random);
        position = 0;
    }

    /** Mazo con orden fijo (para pruebas y repeticiones). La primera carta es el tope. */
    public Deck(List<Card> orderedCards) {
        if (orderedCards.size() != Card.DECK_SIZE || !isCompleteDeck(orderedCards)) {
            throw new IllegalArgumentException("El mazo debe contener las 40 cartas sin repetir");
        }
        cards = new ArrayList<Card>(orderedCards);
        position = 0;
    }

    private static boolean isCompleteDeck(List<Card> list) {
        boolean[] seen = new boolean[Card.DECK_SIZE];
        for (Card c : list) {
            if (c == null || seen[c.getId()]) {
                return false;
            }
            seen[c.getId()] = true;
        }
        return true;
    }

    /**
     * Un mazo armado para que los asientos {@code conFlor} tengan flor y los demás no (la última mano de la
     * Salamanca). La flor es de un palo distinto al de la muestra, así no depende de las piezas.
     */
    public static Deck withFlor(int dealer, int players, java.util.Set<Integer> conFlor, Random random) {
        for (int intento = 0; intento < 5000; intento++) {
            List<Card> todas = new ArrayList<Card>();
            Collections.addAll(todas, Card.all());
            Collections.shuffle(todas, random);
            Card muestra = todas.remove(0);
            CardRules rules = new CardRules(muestra);
            List<List<Card>> manos = new ArrayList<List<Card>>();
            for (int s = 0; s < players; s++) manos.add(new ArrayList<Card>(3));
            boolean ok = true;
            for (int s = 0; s < players && ok; s++) {
                if (!conFlor.contains(s)) continue;
                Suit palo = null;
                for (Card c : todas) {
                    if (c.getSuit() != muestra.getSuit() && !rules.isPieza(c)) {
                        palo = c.getSuit();
                        break;
                    }
                }
                for (java.util.Iterator<Card> it = todas.iterator(); it.hasNext() && manos.get(s).size() < 3; ) {
                    Card c = it.next();
                    if (c.getSuit() == palo && !rules.isPieza(c)) {
                        manos.get(s).add(c);
                        it.remove();
                    }
                }
                ok = manos.get(s).size() == 3 && rules.hasFlor(manos.get(s));
            }
            for (int s = 0; s < players && ok; s++) {
                if (conFlor.contains(s)) continue;
                for (int k = 0; k < 3; k++) manos.get(s).add(todas.remove(0));
                ok = !rules.hasFlor(manos.get(s));
            }
            if (!ok) continue;
            List<Card> orden = new ArrayList<Card>(Card.DECK_SIZE);
            int mano = (dealer + 1) % players;
            for (int r = 0; r < 3; r++) {
                for (int k = 0; k < players; k++) orden.add(manos.get((mano + k) % players).get(r));
            }
            orden.add(muestra);
            orden.addAll(todas);
            return new Deck(orden);
        }
        return new Deck(random);
    }

    public Card draw() {
        if (position >= cards.size()) {
            throw new IllegalStateException("Mazo agotado");
        }
        return cards.get(position++);
    }

    public int remaining() {
        return cards.size() - position;
    }
}
