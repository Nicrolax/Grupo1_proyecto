package com.trucoya.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Pruebas del motor de Truco Uruguayo (Fase 1). Sin dependencias externas:
 *   ./gradlew coreTests
 * o con javac/java directamente (ver README).
 */
public final class TrucoCoreTests {

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<String>();

    public static void main(String[] args) {
        run("Mazo de 40 cartas sin 8 ni 9", TrucoCoreTests::testDeck);
        run("Piezas y valores de envido", TrucoCoreTests::testPiezas);
        run("Muestra mata pieza (Rey hereda)", TrucoCoreTests::testMuestraMataPieza);
        run("Jerarquía completa de cartas", TrucoCoreTests::testHierarchy);
        run("Cálculo de envido", TrucoCoreTests::testEnvidoPoints);
        run("Detección y puntos de flor", TrucoCoreTests::testFlor);
        run("Truco querido y bazas", TrucoCoreTests::testTrucoQuerido);
        run("Truco/Retruco/Vale cuatro no querido", TrucoCoreTests::testTrucoNoQuerido);
        run("Derecho a subir el truco", TrucoCoreTests::testRaiseRights);
        run("El envido está primero", TrucoCoreTests::testEnvidoPrimero);
        run("Valores de la cadena de envido", TrucoCoreTests::testEnvidoChain);
        run("Falta envido", TrucoCoreTests::testFaltaEnvido);
        run("Empate de envido gana la mano", TrucoCoreTests::testEnvidoTie);
        run("Flor sin oposición y anula envido", TrucoCoreTests::testFlorUncontested);
        run("Flor obligatoria antes de jugar", TrucoCoreTests::testFlorMandatory);
        run("Contraflor, al resto y achique", TrucoCoreTests::testContraflor);
        run("Pardas", TrucoCoreTests::testPardas);
        run("Me voy al mazo", TrucoCoreTests::testMazo);
        run("Malas, buenas y fin de partido", TrucoCoreTests::testStagesAndMatchEnd);
        run("2v2: turnos y respuestas por equipo", TrucoCoreTests::testTeams);
        run("Rechazo de jugadas ilegales", TrucoCoreTests::testIllegal);
        run("Vista privada del jugador", TrucoCoreTests::testPlayerView);
        run("Simulación aleatoria masiva (invariantes)", TrucoCoreTests::testFuzz);
        run("Serialización de vista y mesa (red)", TrucoCoreTests::testSnapshotRoundTrip);
        run("Bots: partidos completos sin jugadas ilegales", TrucoCoreTests::testBots);
        run("IA niveles 1-5: siempre jugadas legales (1v1 y 2v2)", TrucoCoreTests::testBotLevels);
        run("IA nivel 5 le gana claramente al nivel 1", TrucoCoreTests::testBotStrength);
        run("IA: señas descubiertas y envido revelado", TrucoCoreTests::testBotKnowledge);
        run("Tantos cantados: en orden y con son buenas", TrucoCoreTests::testTantosCantados);
        run("IA nivel 6 (Vizcacha): legal y al menos pareja con el 5", TrucoCoreTests::testBotNivel6);
        run("Torneo: partidos cortos a 20 (10 malas + 10 buenas)", TrucoCoreTests::testPartidoA20);
        run("Prestigio Criollo: niveles y puntos", TrucoCoreTests::testPrestigio);
        run("Salamanca: mazo armado con flor y 'con flor me achico'", TrucoCoreTests::testSalamancaFlor);

        System.out.println();
        System.out.println("Resultado: " + passed + " pruebas OK, " + failed + " fallidas");
        for (String f : failures) {
            System.out.println("  FALLA: " + f);
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ mini framework

    private interface TestBody {
        void run() throws Exception;
    }

    private static void run(String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("[OK]    " + name);
        } catch (Throwable t) {
            failed++;
            failures.add(name + ": " + t);
            System.out.println("[FALLA] " + name + " -> " + t);
            t.printStackTrace(System.out);
        }
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }

    private static void eq(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg + " (esperado " + expected + ", obtenido " + actual + ")");
        }
    }

    private static void ok(ActionResult r) {
        if (!r.isSuccess()) {
            throw new AssertionError("Acción rechazada: " + r.getReason());
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Card c(int n, Suit s) {
        return Card.of(n, s);
    }

    private static List<Card> h(Card... cards) {
        return Arrays.asList(cards);
    }

    /** Arma un mazo que reparte exactamente {@code hands[seat]} y deja {@code muestra}. */
    private static Deck deck(int players, int dealer, Card muestra, Card[]... hands) {
        int mano = (dealer + 1) % players;
        List<Card> order = new ArrayList<Card>();
        for (int r = 0; r < 3; r++) {
            for (int k = 0; k < players; k++) {
                order.add(hands[(mano + k) % players][r]);
            }
        }
        order.add(muestra);
        for (Card x : Card.all()) {
            if (!order.contains(x)) {
                order.add(x);
            }
        }
        return new Deck(order);
    }

    private static Card[] hand(Card a, Card b, Card cc) {
        return new Card[] {a, b, cc};
    }

    /** Partido 1v1 donde reparte el asiento 1 (mano = asiento 0). */
    private static TrucoMatch match1v1(MatchConfig cfg, Card muestra, Card[] seat0, Card[] seat1) {
        TrucoMatch m = new TrucoMatch(cfg, 1L);
        ok(m.startRound(1, deck(2, 1, muestra, seat0, seat1)));
        m.drainEvents();
        return m;
    }

    private static TrucoMatch match1v1(Card muestra, Card[] seat0, Card[] seat1) {
        return match1v1(MatchConfig.defaults1v1(), muestra, seat0, seat1);
    }

    private static MatchConfig noFlor() {
        return new MatchConfig(2, 30, false, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN);
    }

    private static int indexOf(TrucoMatch m, int seat, Card card) {
        List<Card> hh = m.getHand(seat);
        for (int i = 0; i < hh.size(); i++) {
            if (hh.get(i) == card) return i;
        }
        throw new AssertionError("La carta " + card + " no está en la mano del asiento " + seat);
    }

    private static void play(TrucoMatch m, int seat, Card card) {
        ok(m.playCard(seat, indexOf(m, seat, card)));
    }

    // Manos neutras sin flor ni piezas (muestra 7 de Copas -> piezas de Copas).
    private static final Card MUESTRA_COPA = c(7, Suit.COPA);

    // ------------------------------------------------------------------ pruebas de cartas

    private static void testDeck() {
        Card[] all = Card.all();
        eq(40, all.length, "tamaño del mazo");
        boolean[] seen = new boolean[40];
        for (Card x : all) {
            check(x.getNumber() != 8 && x.getNumber() != 9, "no hay 8 ni 9");
            check(!seen[x.getId()], "id repetido");
            seen[x.getId()] = true;
            check(Card.byId(x.getId()) == x, "byId");
            check(Card.of(x.getNumber(), x.getSuit()) == x, "of");
        }
        Deck d = new Deck(new Random(5));
        boolean[] drawn = new boolean[40];
        for (int i = 0; i < 40; i++) {
            Card x = d.draw();
            check(!drawn[x.getId()], "el mazo barajado repite cartas");
            drawn[x.getId()] = true;
        }
        eq(0, d.remaining(), "mazo agotado");
    }

    private static void testPiezas() {
        CardRules r = new CardRules(c(7, Suit.ORO));
        eq(30, r.piezaEnvidoValue(c(2, Suit.ORO)), "2 de la muestra");
        eq(29, r.piezaEnvidoValue(c(4, Suit.ORO)), "4 de la muestra");
        eq(28, r.piezaEnvidoValue(c(5, Suit.ORO)), "5 de la muestra");
        eq(27, r.piezaEnvidoValue(c(11, Suit.ORO)), "caballo de la muestra");
        eq(27, r.piezaEnvidoValue(c(10, Suit.ORO)), "sota de la muestra");
        check(!r.isPieza(c(12, Suit.ORO)), "el rey no es pieza si la muestra no es pieza");
        check(!r.isPieza(c(2, Suit.ESPADA)), "2 de otro palo no es pieza");
        check(r.trickRank(c(2, Suit.ORO)) > r.trickRank(c(4, Suit.ORO)), "2 > 4");
        check(r.trickRank(c(4, Suit.ORO)) > r.trickRank(c(5, Suit.ORO)), "4 > 5");
        check(r.trickRank(c(5, Suit.ORO)) > r.trickRank(c(11, Suit.ORO)), "5 > caballo");
        check(r.trickRank(c(11, Suit.ORO)) > r.trickRank(c(10, Suit.ORO)), "caballo > sota");
        check(r.trickRank(c(10, Suit.ORO)) > r.trickRank(c(1, Suit.ESPADA)), "sota pieza > ancho de espadas");
        eq(4, r.trickRank(c(7, Suit.BASTO)), "7 de bastos es falso");
        eq(12, r.trickRank(c(7, Suit.ESPADA)), "7 de espadas sigue siendo mata");
    }

    private static void testMuestraMataPieza() {
        int[] piezas = {2, 4, 5, 11, 10};
        int[] envido = {30, 29, 28, 27, 27};
        int[] rank = {19, 18, 17, 16, 15};
        for (int i = 0; i < piezas.length; i++) {
            Card muestra = c(piezas[i], Suit.ESPADA);
            CardRules r = new CardRules(muestra);
            Card rey = c(12, Suit.ESPADA);
            check(r.isPieza(rey), "el rey hereda la pieza " + piezas[i]);
            eq(piezas[i], r.piezaRole(rey), "rol del rey");
            eq(envido[i], r.piezaEnvidoValue(rey), "envido del rey como " + piezas[i]);
            eq(rank[i], r.trickRank(rey), "jerarquía del rey como " + piezas[i]);
            check(!r.isPieza(muestra), "la muestra misma no es pieza");
        }
        CardRules r = new CardRules(c(3, Suit.BASTO));
        check(!r.isPieza(c(12, Suit.BASTO)), "con muestra 3 el rey no es pieza");
    }

    private static void testHierarchy() {
        CardRules r = new CardRules(MUESTRA_COPA);
        Card[] order = {
            c(2, Suit.COPA), c(4, Suit.COPA), c(5, Suit.COPA), c(11, Suit.COPA), c(10, Suit.COPA),
            c(1, Suit.ESPADA), c(1, Suit.BASTO), c(7, Suit.ESPADA), c(7, Suit.ORO),
            c(3, Suit.ORO), c(2, Suit.ORO), c(1, Suit.ORO), c(12, Suit.ORO), c(11, Suit.ORO),
            c(10, Suit.ORO), c(7, Suit.BASTO), c(6, Suit.ORO), c(5, Suit.ORO), c(4, Suit.ORO)
        };
        for (int i = 0; i + 1 < order.length; i++) {
            check(r.compare(order[i], order[i + 1]) > 0, order[i] + " debe ganarle a " + order[i + 1]);
        }
        eq(0, r.compare(c(3, Suit.ESPADA), c(3, Suit.BASTO)), "los 3 empardan");
        eq(0, r.compare(c(1, Suit.COPA), c(1, Suit.ORO)), "anchos falsos empardan");
        eq(0, r.compare(c(12, Suit.COPA), c(12, Suit.ORO)), "reyes empardan (muestra no pieza)");
        eq(0, r.compare(c(6, Suit.COPA), c(6, Suit.ESPADA)), "el 6 de la muestra no es pieza");
        eq(0, r.compare(c(3, Suit.COPA), c(3, Suit.ORO)), "el 3 de la muestra no es pieza");
    }

    private static void testEnvidoPoints() {
        CardRules r = new CardRules(MUESTRA_COPA);
        eq(33, r.envidoPoints(h(c(7, Suit.ORO), c(6, Suit.ORO), c(1, Suit.BASTO))), "7+6 de oro");
        eq(7, r.envidoPoints(h(c(7, Suit.ORO), c(6, Suit.BASTO), c(1, Suit.ESPADA))), "sin pares: la más alta");
        eq(20, r.envidoPoints(h(c(12, Suit.ORO), c(11, Suit.ORO), c(1, Suit.BASTO))), "figuras valen 0");
        eq(0, r.envidoPoints(h(c(12, Suit.ORO), c(11, Suit.BASTO), c(10, Suit.ESPADA))), "tres figuras sueltas");
        eq(37, r.envidoPoints(h(c(2, Suit.COPA), c(7, Suit.ORO), c(1, Suit.BASTO))), "pieza 2 + 7");
        eq(35, r.envidoPoints(h(c(4, Suit.COPA), c(3, Suit.ORO), c(6, Suit.BASTO))), "pieza 4 + 6 (ej. pagat)");
        eq(27, r.envidoPoints(h(c(10, Suit.COPA), c(12, Suit.ORO), c(11, Suit.BASTO))), "sota pieza sola");
        // Sin flor habilitada, dos piezas: la mayor + valor de suma de la otra.
        eq(39, r.envidoPoints(h(c(2, Suit.COPA), c(4, Suit.COPA), c(1, Suit.BASTO))), "2 y 4 de la muestra");
    }

    private static void testFlor() {
        CardRules r = new CardRules(MUESTRA_COPA);
        check(r.hasFlor(h(c(5, Suit.ORO), c(6, Suit.ORO), c(7, Suit.ORO))), "tres del mismo palo");
        eq(38, r.florPoints(h(c(5, Suit.ORO), c(6, Suit.ORO), c(7, Suit.ORO))), "5-6-7 = 38");
        check(r.hasFlor(h(c(2, Suit.COPA), c(6, Suit.ORO), c(7, Suit.ORO))), "pieza + dos del mismo palo");
        eq(43, r.florPoints(h(c(2, Suit.COPA), c(6, Suit.ORO), c(7, Suit.ORO))), "20+10+6+7");
        check(r.hasFlor(h(c(2, Suit.COPA), c(4, Suit.COPA), c(7, Suit.ORO))), "dos piezas + cualquiera");
        check(r.hasFlor(h(c(2, Suit.COPA), c(4, Suit.COPA), c(5, Suit.COPA))), "tres piezas");
        eq(47, r.florPoints(h(c(2, Suit.COPA), c(4, Suit.COPA), c(5, Suit.COPA))), "flor máxima 47");
        check(!r.hasFlor(h(c(2, Suit.COPA), c(6, Suit.ORO), c(7, Suit.BASTO))), "pieza + dos de distinto palo");
        check(!r.hasFlor(h(c(1, Suit.ORO), c(6, Suit.ORO), c(7, Suit.BASTO))), "dos del mismo palo");
        // Rey heredando pieza cuenta como pieza para la flor.
        CardRules r2 = new CardRules(c(4, Suit.BASTO));
        check(r2.hasFlor(h(c(12, Suit.BASTO), c(3, Suit.ORO), c(1, Suit.ORO))), "rey-pieza + dos de oro");
        eq(33, r2.florPoints(h(c(12, Suit.BASTO), c(3, Suit.ORO), c(1, Suit.ORO))), "20+9+3+1");
    }

    // ------------------------------------------------------------------ pruebas de juego

    private static final Card[] A_STRONG = hand(c(1, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ORO));
    private static final Card[] B_WEAK = hand(c(4, Suit.ESPADA), c(5, Suit.BASTO), c(6, Suit.ORO));

    private static void testTrucoQuerido() {
        TrucoMatch m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        eq(0, m.getMano(), "mano");
        ok(m.act(0, ActionType.TRUCO));
        check(m.legalActions(0).isEmpty(), "quien canta espera");
        check(m.legalActions(1).contains(ActionType.QUIERO), "el rival responde");
        check(!m.playCard(0, 0).isSuccess(), "no se juega con canto pendiente");
        ok(m.act(1, ActionType.QUIERO));
        play(m, 0, c(1, Suit.ESPADA));
        play(m, 1, c(4, Suit.ESPADA));
        play(m, 0, c(1, Suit.BASTO));
        play(m, 1, c(5, Suit.BASTO));
        check(m.isRoundFinished(), "dos bazas ganadas terminan la ronda");
        eq(0, m.getRoundWinner(), "gana A");
        eq(2, m.getScore(0), "truco querido vale 2");
        eq(0, m.getScore(1), "B sin puntos");
        ok(m.startNextRound());
        eq(0, m.getDealer(), "el mazo pasa al siguiente");
        eq(1, m.getMano(), "nueva mano");
    }

    private static void testTrucoNoQuerido() {
        TrucoMatch m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.NO_QUIERO));
        eq(1, m.getScore(0), "truco no querido = 1");

        m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.RETRUCO));
        ok(m.act(0, ActionType.NO_QUIERO));
        eq(2, m.getScore(1), "retruco no querido = 2");

        m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.RETRUCO));
        ok(m.act(0, ActionType.VALE_CUATRO));
        ok(m.act(1, ActionType.NO_QUIERO));
        eq(3, m.getScore(0), "vale cuatro no querido = 3");

        m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.RETRUCO));
        ok(m.act(0, ActionType.VALE_CUATRO));
        ok(m.act(1, ActionType.QUIERO));
        check(!m.legalActions(0).contains(ActionType.VALE_CUATRO), "no hay más que vale cuatro");
        play(m, 0, c(1, Suit.ESPADA));
        play(m, 1, c(4, Suit.ESPADA));
        play(m, 0, c(1, Suit.BASTO));
        play(m, 1, c(5, Suit.BASTO));
        eq(4, m.getScore(0), "vale cuatro querido = 4");
    }

    private static void testRaiseRights() {
        TrucoMatch m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.QUIERO));
        check(!m.legalActions(0).contains(ActionType.RETRUCO), "quien cantó truco no puede retrucar su propio canto");
        play(m, 0, c(3, Suit.ORO));
        check(m.legalActions(1).contains(ActionType.RETRUCO), "quien aceptó puede retrucar en su turno");
        ok(m.act(1, ActionType.RETRUCO));
        ok(m.act(0, ActionType.QUIERO));
        eq(2, m.getTrucoLevel(), "retruco aceptado");
        check(!m.legalActions(1).contains(ActionType.VALE_CUATRO), "B no puede subir lo que cantó");
        play(m, 1, c(6, Suit.ORO));
        check(m.legalActions(0).contains(ActionType.VALE_CUATRO), "A aceptó: puede vale cuatro");
    }

    private static void testEnvidoPrimero() {
        // Asiento 0 (mano): 7 y 3 de espadas = 30. Asiento 1 (pie): 7+6 de oro = 33.
        Card[] mano = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA));
        Card[] pie = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO));
        TrucoMatch m = match1v1(MUESTRA_COPA, mano, pie);
        ok(m.act(0, ActionType.TRUCO));
        check(m.legalActions(1).contains(ActionType.ENVIDO), "el envido está primero");
        ok(m.act(1, ActionType.ENVIDO));
        ok(m.act(0, ActionType.QUIERO));
        eq(2, m.getScore(1), "envido querido para el pie (33 vs 30)");
        check(m.hasPendingCall(), "el truco vuelve a quedar pendiente");
        check(m.getPendingCall() == ActionType.TRUCO, "truco pendiente");
        eq(1, m.getPendingAwaitingTeam(), "lo sigue debiendo responder el pie");
        check(!m.legalActions(1).contains(ActionType.ENVIDO), "no se envida dos veces");
        ok(m.act(1, ActionType.QUIERO));
        eq(1, m.getTrucoLevel(), "truco aceptado");
        eq(0, m.getTurn(), "el turno vuelve a la mano");

        // Una vez jugada la primera carta, ese jugador ya no puede envidar.
        m = match1v1(MUESTRA_COPA, mano, pie);
        play(m, 0, c(1, Suit.BASTO));
        ok(m.act(1, ActionType.TRUCO));
        check(!m.legalActions(0).contains(ActionType.ENVIDO), "la mano ya jugó: no puede envidar");
    }

    private static void testEnvidoChain() {
        Card[] a = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO)); // 33
        Card[] b = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA)); // 30
        TrucoMatch m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.ENVIDO));
        check(!m.legalActions(0).contains(ActionType.ENVIDO), "solo dos envidos");
        ok(m.act(0, ActionType.QUIERO));
        eq(4, m.getScore(0), "envido envido querido = 4");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.REAL_ENVIDO));
        ok(m.act(0, ActionType.NO_QUIERO));
        eq(2, m.getScore(1), "envido + real no querido = 2");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.REAL_ENVIDO));
        check(!m.legalActions(1).contains(ActionType.REAL_ENVIDO), "real solo sube a falta");
        ok(m.act(1, ActionType.NO_QUIERO));
        eq(1, m.getScore(0), "real no querido = 1");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.REAL_ENVIDO));
        ok(m.act(0, ActionType.QUIERO));
        eq(5, m.getScore(0), "envido + real querido = 5");
        check(!m.legalActions(0).contains(ActionType.ENVIDO), "envido cerrado");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.ENVIDO));
        ok(m.act(0, ActionType.REAL_ENVIDO));
        ok(m.act(1, ActionType.FALTA_ENVIDO));
        ok(m.act(0, ActionType.NO_QUIERO));
        eq(7, m.getScore(1), "E+E+R no querida la falta = 7");
    }

    private static void testFaltaEnvido() {
        Card[] a = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO)); // 33
        Card[] b = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA)); // 30
        TrucoMatch m = match1v1(MUESTRA_COPA, a, b);
        m.award(1, 20, "test");
        m.award(0, 5, "test");
        ok(m.act(0, ActionType.FALTA_ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        eq(15, m.getScore(0), "malas vs buenas: lo que le falta al que va ganando (30-20=10)");
        check(!m.isOver(), "no se gana el partido");

        // Ambos en las malas: se juega el partido.
        m = match1v1(MUESTRA_COPA, a, b);
        m.award(1, 10, "test");
        ok(m.act(0, ActionType.FALTA_ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        check(m.isOver(), "ambos en malas: la falta es por el partido");
        eq(0, m.getWinner(), "gana A");

        // Ambos en las buenas: también se juega el partido (aunque gane el que va perdiendo).
        m = match1v1(MUESTRA_COPA, a, b);
        m.award(0, 17, "test");
        m.award(1, 25, "test");
        ok(m.act(0, ActionType.FALTA_ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        check(m.isOver(), "ambos en buenas: la falta es por el partido");
        eq(0, m.getWinner(), "gana A desde atrás");

        // Variante alternativa configurable.
        MatchConfig alt = new MatchConfig(2, 30, true, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN);
        m = match1v1(alt, MUESTRA_COPA, a, b);
        m.award(1, 10, "test");
        ok(m.act(0, ActionType.FALTA_ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        eq(20, m.getScore(0), "alternativa: 30-10");
    }

    private static void testEnvidoTie() {
        Card[] a = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO)); // 33
        Card[] b = hand(c(7, Suit.ESPADA), c(6, Suit.ESPADA), c(4, Suit.ORO)); // 33
        TrucoMatch m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        eq(2, m.getScore(0), "empate: gana la mano (asiento 0)");
        eq(0, m.getScore(1), "pie sin puntos");
    }

    private static void testFlorUncontested() {
        Card[] a = hand(c(5, Suit.ORO), c(6, Suit.ORO), c(7, Suit.ORO));
        Card[] b = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA));
        TrucoMatch m = match1v1(MUESTRA_COPA, b, a); // asiento 1 tiene flor
        ok(m.act(0, ActionType.ENVIDO));
        eq(1, m.legalActions(1).size(), "con flor solo puede cantar flor");
        ok(m.act(1, ActionType.FLOR));
        eq(3, m.getScore(1), "flor sin oposición = 3");
        eq(0, m.getScore(0), "el envido se anula");
        check(!m.hasPendingCall(), "nada pendiente");
        check(!m.legalActions(0).contains(ActionType.ENVIDO), "no se puede envidar tras la flor");

        MatchConfig cfg = noFlor();
        m = match1v1(cfg, MUESTRA_COPA, a, b);
        check(!m.legalActions(0).contains(ActionType.FLOR), "flor deshabilitada");
        check(m.legalActions(0).contains(ActionType.ENVIDO), "sin flor se envida normalmente");
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        eq(2, m.getScore(0), "envido 33 vs 30");
    }

    private static void testFlorMandatory() {
        Card[] a = hand(c(5, Suit.ORO), c(6, Suit.ORO), c(7, Suit.ORO));
        Card[] b = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA));
        TrucoMatch m = match1v1(MUESTRA_COPA, a, b);
        check(!m.legalActions(0).contains(ActionType.PLAY_CARD), "debe cantar flor antes de jugar");
        check(!m.playCard(0, 0).isSuccess(), "jugar sin cantar flor es rechazado");
        ok(m.act(0, ActionType.FLOR));
        ok(m.playCard(0, 0));
        check(!m.legalActions(1).contains(ActionType.FLOR), "B no tiene flor");
    }

    private static void testContraflor() {
        Card[] a = hand(c(5, Suit.ORO), c(6, Suit.ORO), c(7, Suit.ORO));        // 38
        Card[] b = hand(c(1, Suit.ESPADA), c(3, Suit.ESPADA), c(2, Suit.ESPADA)); // 26
        TrucoMatch m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.FLOR));
        check(m.hasPendingCall(), "flor con oposición queda pendiente");
        ok(m.act(1, ActionType.CON_FLOR_ME_ACHICO));
        eq(3, m.getScore(0), "con flor me achico = 3");
        // El rival (asiento 1) se va al mazo sin truco: un punto más (3 + 1).
        ok(m.playCard(0, 0));
        ok(m.act(1, ActionType.ME_VOY_AL_MAZO));
        eq(4, m.getScore(0), "achique 3 + mazo sin truco 1");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.FLOR));
        ok(m.act(1, ActionType.CONTRAFLOR));
        ok(m.act(0, ActionType.QUIERO));
        eq(6, m.getScore(0), "contraflor querida: gana 38 (6 puntos)");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.FLOR));
        ok(m.act(1, ActionType.CONTRAFLOR));
        ok(m.act(0, ActionType.NO_QUIERO));
        eq(3, m.getScore(1), "contraflor no querida = 3");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.FLOR));
        ok(m.act(1, ActionType.CONTRAFLOR));
        ok(m.act(0, ActionType.CONTRAFLOR_AL_RESTO));
        ok(m.act(1, ActionType.NO_QUIERO));
        eq(6, m.getScore(0), "al resto no querido tras contraflor = 6");

        m = match1v1(MUESTRA_COPA, a, b);
        ok(m.act(0, ActionType.FLOR));
        ok(m.act(1, ActionType.CONTRAFLOR_AL_RESTO));
        ok(m.act(0, ActionType.QUIERO));
        check(m.isOver(), "contraflor al resto querida con 0-0 da el partido");
        eq(0, m.getWinner(), "gana la mejor flor");

        // Flor cantada como respuesta al truco: el truco se reanuda después.
        Card[] plain = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ORO));
        m = match1v1(MUESTRA_COPA, plain, a);
        ok(m.act(0, ActionType.TRUCO));
        check(m.legalActions(1).contains(ActionType.FLOR), "puede responder con flor");
        ok(m.act(1, ActionType.FLOR));
        eq(3, m.getScore(1), "flor sin oposición");
        check(m.getPendingCall() == ActionType.TRUCO, "el truco sigue esperando respuesta");
        ok(m.act(1, ActionType.QUIERO));
        eq(1, m.getTrucoLevel(), "truco aceptado tras la flor");
    }

    private static void testPardas() {
        // 1ª parda, 2ª la gana B -> B.
        Card[] a = hand(c(3, Suit.ORO), c(4, Suit.BASTO), c(5, Suit.BASTO));
        Card[] b = hand(c(3, Suit.ESPADA), c(1, Suit.ESPADA), c(6, Suit.BASTO));
        TrucoMatch m = match1v1(noFlor(), MUESTRA_COPA, a, b);
        play(m, 0, c(3, Suit.ORO));
        play(m, 1, c(3, Suit.ESPADA));
        eq(-1, m.getTrickResult(0), "primera parda");
        eq(0, m.getTurn(), "tras parda sale el mismo que salió");
        play(m, 0, c(4, Suit.BASTO));
        play(m, 1, c(1, Suit.ESPADA));
        check(m.isRoundFinished(), "parda + ganada define");
        eq(1, m.getRoundWinner(), "gana B");
        eq(1, m.getScore(1), "1 punto sin truco");

        // 1ª gana A, 2ª parda -> A.
        a = hand(c(1, Suit.ESPADA), c(3, Suit.ORO), c(4, Suit.BASTO));
        b = hand(c(4, Suit.ESPADA), c(3, Suit.ESPADA), c(1, Suit.BASTO));
        m = match1v1(noFlor(), MUESTRA_COPA, a, b);
        play(m, 0, c(1, Suit.ESPADA));
        play(m, 1, c(4, Suit.ESPADA));
        play(m, 0, c(3, Suit.ORO));
        play(m, 1, c(3, Suit.ESPADA));
        check(m.isRoundFinished(), "ganada + parda define");
        eq(0, m.getRoundWinner(), "gana quien ganó la primera");

        // A gana 1ª, B gana 2ª, 3ª parda -> A.
        a = hand(c(1, Suit.ESPADA), c(4, Suit.BASTO), c(3, Suit.ORO));
        b = hand(c(4, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA));
        m = match1v1(noFlor(), MUESTRA_COPA, a, b);
        play(m, 0, c(1, Suit.ESPADA));
        play(m, 1, c(4, Suit.ESPADA));
        play(m, 0, c(4, Suit.BASTO));
        play(m, 1, c(1, Suit.BASTO));
        eq(1, m.getTurn(), "sale quien ganó la baza");
        play(m, 1, c(3, Suit.ESPADA));
        play(m, 0, c(3, Suit.ORO));
        eq(0, m.getRoundWinner(), "tercera parda: gana quien ganó la primera");

        // Todas pardas -> la mano.
        a = hand(c(3, Suit.ORO), c(6, Suit.ORO), c(5, Suit.BASTO));
        b = hand(c(3, Suit.ESPADA), c(6, Suit.ESPADA), c(5, Suit.ESPADA));
        m = match1v1(noFlor(), MUESTRA_COPA, b, a); // mano = asiento 0 con (3E,6E,5E)
        play(m, 0, c(3, Suit.ESPADA));
        play(m, 1, c(3, Suit.ORO));
        play(m, 0, c(6, Suit.ESPADA));
        play(m, 1, c(6, Suit.ORO));
        check(!m.isRoundFinished(), "dos pardas: se juega la tercera");
        play(m, 0, c(5, Suit.ESPADA));
        play(m, 1, c(5, Suit.BASTO));
        eq(0, m.getRoundWinner(), "todas pardas: gana la mano");
    }

    private static void testMazo() {
        TrucoMatch m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.ME_VOY_AL_MAZO));
        eq(1, m.getScore(1), "mazo sin truco = 1");

        m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.ME_VOY_AL_MAZO));
        eq(1, m.getScore(0), "mazo ante truco = no quiero = 1");

        m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.QUIERO));
        play(m, 0, c(1, Suit.ESPADA));
        ok(m.act(1, ActionType.ME_VOY_AL_MAZO));
        eq(2, m.getScore(0), "mazo con truco querido = 2");

        m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        ok(m.act(0, ActionType.ENVIDO));
        check(!m.legalActions(1).contains(ActionType.ME_VOY_AL_MAZO), "primero se responde el envido");
    }

    private static void testStagesAndMatchEnd() {
        TrucoMatch m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        m.award(0, 15, "test");
        eq(15, m.getScore(0), "15");
        check("15 malas".equals(m.stageLabel(0)), "15 malas: " + m.stageLabel(0));
        check(!m.isInBuenas(0), "15 todavía es malas");
        m.drainEvents();
        m.award(0, 1, "test");
        boolean stage = false;
        for (GameEvent e : m.drainEvents()) {
            if (e.getType() == GameEvent.Type.STAGE_CHANGED) stage = true;
        }
        check(stage, "evento de paso a las buenas");
        check("1 buena".equals(m.stageLabel(0)), "1 buena: " + m.stageLabel(0));
        ok(m.act(0, ActionType.TRUCO));
        ok(m.act(1, ActionType.QUIERO));
        m.award(0, 12, "test"); // 28
        play(m, 0, c(1, Suit.ESPADA));
        play(m, 1, c(4, Suit.ESPADA));
        play(m, 0, c(1, Suit.BASTO));
        play(m, 1, c(5, Suit.BASTO));
        check(m.isOver(), "llega a 30");
        eq(30, m.getScore(0), "tope 30");
        eq(0, m.getWinner(), "ganador");
        check(!m.startNextRound().isSuccess(), "no hay más rondas");
        check(m.legalActions(1).isEmpty(), "sin acciones al terminar");

        MatchConfig a40 = new MatchConfig(2, 40, true, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN);
        m = match1v1(a40, MUESTRA_COPA, A_STRONG, B_WEAK);
        m.award(1, 20, "test");
        check("20 malas".equals(m.stageLabel(1)), "a 40: 20 malas");
        m.award(1, 1, "test");
        check("1 buena".equals(m.stageLabel(1)), "a 40: 1 buena");
    }

    private static void testTeams() {
        MatchConfig cfg = new MatchConfig(4, 30, true, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN);
        Card[] s0 = hand(c(1, Suit.ESPADA), c(4, Suit.ORO), c(5, Suit.BASTO));
        Card[] s1 = hand(c(4, Suit.ESPADA), c(5, Suit.ORO), c(6, Suit.BASTO));
        Card[] s2 = hand(c(1, Suit.BASTO), c(6, Suit.ORO), c(4, Suit.BASTO));
        Card[] s3 = hand(c(5, Suit.ESPADA), c(6, Suit.ESPADA), c(7, Suit.BASTO));
        TrucoMatch m = new TrucoMatch(cfg, 3L);
        ok(m.startRound(3, deck(4, 3, MUESTRA_COPA, s0, s1, s2, s3)));
        eq(0, m.getMano(), "mano 0");
        ok(m.act(0, ActionType.TRUCO));
        check(m.legalActions(1).contains(ActionType.QUIERO), "rival 1 puede responder");
        check(m.legalActions(3).contains(ActionType.QUIERO), "rival 3 puede responder");
        check(m.legalActions(2).isEmpty(), "el compañero no responde su propio truco");
        ok(m.act(3, ActionType.QUIERO));
        play(m, 0, c(1, Suit.ESPADA));
        check(!m.playCard(2, 0).isSuccess(), "fuera de turno");
        play(m, 1, c(4, Suit.ESPADA));
        play(m, 2, c(6, Suit.ORO));
        play(m, 3, c(5, Suit.ESPADA));
        eq(0, m.getTrickResult(0), "equipo 0 gana la primera");
        eq(0, m.getTurn(), "sale el asiento 0");
        play(m, 0, c(4, Suit.ORO));
        play(m, 1, c(5, Suit.ORO));
        play(m, 2, c(1, Suit.BASTO));
        play(m, 3, c(7, Suit.BASTO));
        check(m.isRoundFinished(), "fin de ronda");
        eq(2, m.getScore(0), "equipo 0 suma el truco");
    }

    private static void testIllegal() {
        TrucoMatch m = match1v1(MUESTRA_COPA, A_STRONG, B_WEAK);
        check(!m.playCard(1, 0).isSuccess(), "fuera de turno");
        check(!m.act(1, ActionType.TRUCO).isSuccess(), "cantar fuera de turno");
        check(!m.act(0, ActionType.QUIERO).isSuccess(), "quiero sin canto");
        check(!m.act(0, ActionType.RETRUCO).isSuccess(), "retruco sin truco");
        check(!m.act(0, ActionType.FLOR).isSuccess(), "flor falsa");
        check(!m.act(0, ActionType.PLAY_CARD).isSuccess(), "PLAY_CARD por act");
        check(!m.act(0, null).isSuccess(), "acción nula");
        check(!m.playCard(0, 7).isSuccess(), "índice fuera de rango");
        check(!m.playCard(9, 0).isSuccess(), "asiento inexistente");
        check(!m.playCardById(0, c(7, Suit.ORO).getId()).isSuccess(), "carta ajena por id");
        ok(m.playCardById(0, c(1, Suit.ESPADA).getId()));
        check(!m.startNextRound().isSuccess(), "no se reparte con ronda en curso");
        eq(0, m.getScore(0) + m.getScore(1), "las jugadas ilegales no suman puntos");
    }

    private static void testPlayerView() {
        Card[] a = hand(c(2, Suit.COPA), c(6, Suit.ESPADA), c(7, Suit.BASTO));
        TrucoMatch m = match1v1(MUESTRA_COPA, a, B_WEAK);
        PlayerView v = m.viewFor(0);
        eq(3, v.hand.size(), "3 cartas");
        check(v.handIsPieza[0] && !v.handIsPieza[1], "resaltado de piezas");
        eq(37, v.envidoPoints, "envido propio");
        check(v.muestra == MUESTRA_COPA, "muestra visible");
        check(v.legalActions.contains(ActionType.PLAY_CARD), "botonera");
        PlayerView vb = m.viewFor(1);
        for (Card x : vb.hand) {
            check(!Arrays.asList(a).contains(x), "la vista de B no contiene cartas de A");
        }
        check(vb.legalActions.isEmpty(), "B no tiene acciones fuera de turno");
    }

    // ------------------------------------------------------------------ simulación

    private static void testFuzz() {
        Random rnd = new Random(20261015L);
        int matches = 0;
        long actions = 0;
        MatchConfig[] configs = {
            new MatchConfig(2, 30, true, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN),
            new MatchConfig(2, 40, false, MatchConfig.FaltaMode.PARTIDO_SALVO_MALAS_Y_BUENAS),
            new MatchConfig(4, 30, true, MatchConfig.FaltaMode.PARTIDO_SALVO_MALAS_Y_BUENAS),
            new MatchConfig(4, 40, true, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN),
            new MatchConfig(4, 30, false, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN),
        };
        ActionType[] allActions = ActionType.values();
        for (int iter = 0; iter < 4000; iter++) {
            MatchConfig cfg = configs[iter % configs.length];
            int n = cfg.getPlayers();
            TrucoMatch m = new TrucoMatch(cfg, rnd.nextLong());
            m.start();
            int guard = 0;
            int[] lastScore = {0, 0};
            while (!m.isOver()) {
                check(++guard < 20000, "el partido no termina");
                if (m.isRoundFinished()) {
                    checkRoundInvariants(m, n);
                    ok(m.startNextRound());
                    continue;
                }
                checkRoundInvariants(m, n);
                // Intento ilegal aleatorio: debe rechazarse sin efectos.
                int rs = rnd.nextInt(n);
                ActionType ra = allActions[rnd.nextInt(allActions.length)];
                if (ra != ActionType.PLAY_CARD && !m.legalActions(rs).contains(ra)) {
                    int s0 = m.getScore(0), s1 = m.getScore(1);
                    check(!m.act(rs, ra).isSuccess(), "acción ilegal aceptada: " + ra);
                    check(s0 == m.getScore(0) && s1 == m.getScore(1), "acción ilegal cambió el puntaje");
                }
                // Elegir un jugador con acciones legales.
                List<Integer> actors = new ArrayList<Integer>();
                for (int s = 0; s < n; s++) {
                    if (!m.legalActions(s).isEmpty()) actors.add(s);
                }
                check(!actors.isEmpty(), "bloqueo: nadie puede actuar");
                if (m.hasPendingCall()) {
                    for (int s : actors) {
                        eq(m.getPendingAwaitingTeam(), s % 2, "solo responde el equipo desafiado");
                    }
                } else {
                    eq(1, actors.size(), "sin cantos pendientes actúa solo quien tiene el turno");
                    eq(m.getTurn(), actors.get(0), "actúa quien tiene el turno");
                }
                int seat = actors.get(rnd.nextInt(actors.size()));
                List<ActionType> legal = m.legalActions(seat);
                // Sesgo: jugar cartas más a menudo para partidos realistas.
                ActionType a = legal.contains(ActionType.PLAY_CARD) && rnd.nextInt(3) > 0
                        ? ActionType.PLAY_CARD : legal.get(rnd.nextInt(legal.size()));
                if (a == ActionType.ME_VOY_AL_MAZO && rnd.nextInt(4) > 0) {
                    a = legal.get(0);
                }
                if (a == ActionType.PLAY_CARD) {
                    ok(m.playCard(seat, rnd.nextInt(m.getHand(seat).size())));
                } else {
                    ok(m.act(seat, a));
                }
                actions++;
                for (int t = 0; t < 2; t++) {
                    check(m.getScore(t) >= lastScore[t], "el puntaje nunca baja");
                    check(m.getScore(t) <= cfg.getTargetScore(), "el puntaje no supera el tope");
                    lastScore[t] = m.getScore(t);
                }
                m.drainEvents();
            }
            check(m.getWinner() == 0 || m.getWinner() == 1, "hay ganador");
            eq(cfg.getTargetScore(), m.getScore(m.getWinner()), "el ganador llega al tope");
            matches++;
        }
        System.out.println("        " + matches + " partidos simulados, " + actions + " acciones válidas");
    }

    private static void testSnapshotRoundTrip() throws Exception {
        Random rnd = new Random(99);
        TrucoBot bot = new TrucoBot(new Random(5));
        for (int iter = 0; iter < 300; iter++) {
            MatchConfig cfg = iter % 2 == 0 ? MatchConfig.defaults1v1() : MatchConfig.defaults2v2();
            TrucoMatch m = new TrucoMatch(cfg, rnd.nextLong());
            m.start();
            int steps = rnd.nextInt(40);
            for (int k = 0; k < steps && !m.isOver(); k++) {
                if (m.isRoundFinished()) { ok(m.startNextRound()); continue; }
                for (int s = 0; s < cfg.getPlayers(); s++) {
                    if (!m.legalActions(s).isEmpty()) { ok(bot.play(m, s)); break; }
                }
            }
            for (int s = 0; s < cfg.getPlayers(); s++) {
                PlayerView v = m.viewFor(s);
                List<String> names = new ArrayList<String>();
                boolean[] bots = new boolean[cfg.getPlayers()];
                for (int i = 0; i < cfg.getPlayers(); i++) { names.add("Jugador ñ " + i); bots[i] = i % 2 == 1; }
                TableSnapshot snap = new TableSnapshot(v, names, bots, Arrays.asList("¡Truco!", "Envido: 33"),
                        1, "¡Quiero retruco!", cfg.getPlayers(), 30, true, 1, true, true,
                        "Nivel 2 · Parroquiano (75 pts)", Arrays.asList(new TableSnapshot.PotEntry("minecraft:diamond", 3, "Paisana")), false);
                TableSnapshot back = TableSnapshot.fromBytes(snap.toBytes());
                PlayerView w = back.view;
                eq(v.seat, w.seat, "asiento");
                check(v.hand.equals(w.hand), "mano");
                check(v.muestra == w.muestra, "muestra");
                check(v.legalActions.equals(w.legalActions), "acciones");
                check(Arrays.equals(v.score, w.score), "puntaje");
                check(Arrays.equals(v.stageLabel, w.stageLabel), "etapas");
                check(Arrays.equals(v.trickResults, w.trickResults), "bazas");
                check(Arrays.deepEquals(v.table, w.table), "mesa");
                eq(v.envidoPoints, w.envidoPoints, "envido");
                eq(v.turn, w.turn, "turno");
                check(v.pendingCall == w.pendingCall, "canto pendiente");
                check(back.names.equals(names) && Arrays.equals(back.isBot, bots), "nombres");
                check("¡Quiero retruco!".equals(back.lastCallText), "texto");
                check(back.myPrestige.startsWith("Nivel 2"), "prestigio");
                check(back.pot.size() == 1 && back.pot.get(0).count == 3 && "minecraft:diamond".equals(back.pot.get(0).itemId)
                        && "Paisana".equals(back.pot.get(0).owner), "pozo");
            }
        }
        TableSnapshot empty = new TableSnapshot(null, new ArrayList<String>(), new boolean[0],
                new ArrayList<String>(), -1, null, 4, 40, false, 3, false, false, null, null, true);
        TableSnapshot e2 = TableSnapshot.fromBytes(empty.toBytes());
        check(e2.view == null && e2.cfgPlayers == 4 && e2.cfgTarget == 40 && !e2.cfgFlor && e2.clandestina && e2.pot.isEmpty(), "mesa vacía");
        boolean rejected = false;
        try { TableSnapshot.fromBytes(new byte[] {1, 0, 0, 0}); } catch (Exception ex) { rejected = true; }
        check(rejected, "datos corruptos rechazados");
    }

    private static void testBots() {
        Random rnd = new Random(7);
        int[] wins = new int[2];
        for (int iter = 0; iter < 1500; iter++) {
            MatchConfig cfg = new MatchConfig(iter % 3 == 0 ? 4 : 2, iter % 2 == 0 ? 30 : 40, iter % 5 != 0);
            TrucoMatch m = new TrucoMatch(cfg, rnd.nextLong());
            TrucoBot bot = new TrucoBot(new Random(rnd.nextLong()));
            m.start();
            int guard = 0;
            while (!m.isOver()) {
                check(++guard < 5000, "partido de bots sin fin");
                if (m.isRoundFinished()) { ok(m.startNextRound()); continue; }
                boolean acted = false;
                for (int s = 0; s < cfg.getPlayers() && !acted; s++) {
                    if (!m.legalActions(s).isEmpty()) {
                        TrucoBot.Decision d = bot.decide(m, s);
                        check(m.legalActions(s).contains(d.action), "decisión ilegal del bot: " + d);
                        ok(bot.play(m, s));
                        acted = true;
                    }
                }
                check(acted, "nadie actúa");
            }
            wins[m.getWinner()]++;
        }
        System.out.println("        1500 partidos bot vs bot (victorias por equipo: " + wins[0] + " / " + wins[1] + ")");
    }

    /** Juega un partido completo entre bots (uno por asiento, con observación de eventos). */
    private static TrucoMatch playBots(MatchConfig cfg, long seed, int[] levels) {
        TrucoMatch m = new TrucoMatch(cfg, seed);
        Random r = new Random(seed * 31 + 7);
        TrucoBot[] bots = new TrucoBot[cfg.getPlayers()];
        for (int s = 0; s < bots.length; s++) bots[s] = new TrucoBot(new Random(r.nextLong()), levels[s % levels.length]);
        m.start();
        int guard = 0;
        while (!m.isOver()) {
            check(++guard < 5000, "partido sin fin");
            for (GameEvent e : m.drainEvents()) for (TrucoBot b : bots) b.observe(e, m);
            if (m.isRoundFinished()) { ok(m.startNextRound()); continue; }
            boolean acted = false;
            for (int s = 0; s < bots.length && !acted; s++) {
                if (!m.legalActions(s).isEmpty()) {
                    TrucoBot.Decision d = bots[s].decide(m, s);
                    check(d != null && m.legalActions(s).contains(d.action), "decisión ilegal nivel " + bots[s].getLevel() + ": " + d);
                    ok(d.action == ActionType.PLAY_CARD ? m.playCard(s, d.handIndex) : m.act(s, d.action));
                    acted = true;
                }
            }
            check(acted, "nadie actúa");
        }
        return m;
    }

    private static void testBotLevels() {
        Random rnd = new Random(11);
        for (int iter = 0; iter < 60; iter++) {
            MatchConfig cfg = new MatchConfig(iter % 2 == 0 ? 2 : 4, iter % 3 == 0 ? 40 : 30, iter % 4 != 0);
            int[] levels = {1 + iter % 5, 1 + (iter / 5) % 5};
            TrucoMatch m = playBots(cfg, rnd.nextLong(), levels);
            check(m.isOver(), "terminó");
        }
    }

    private static void testBotStrength() {
        Random rnd = new Random(2026);
        int wins5 = 0;
        int games = 120;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < games; i++) {
            boolean fiveFirst = i % 2 == 0;
            TrucoMatch m = playBots(MatchConfig.defaults1v1(), rnd.nextLong(), fiveFirst ? new int[] {5, 1} : new int[] {1, 5});
            int winnerSeat = m.getWinner();
            if ((winnerSeat == 0) == fiveFirst) wins5++;
        }
        long ms = System.currentTimeMillis() - t0;
        System.out.println("        Nivel 5 vs nivel 1: " + wins5 + "/" + games + " victorias (" + (ms / games) + " ms por partido)");
        check(wins5 > games * 0.6, "el nivel 5 debería ganar más del 60%");
    }

    private static void testBotKnowledge() {
        // Seña descubierta: el bot sabe que el rival tiene el ancho de espadas.
        Card[] a = hand(c(1, Suit.ESPADA), c(4, Suit.ORO), c(5, Suit.BASTO));
        Card[] b = hand(c(3, Suit.ESPADA), c(3, Suit.ORO), c(6, Suit.BASTO));
        TrucoMatch m = match1v1(noFlor(), MUESTRA_COPA, a, b);
        TrucoBot bot = new TrucoBot(new Random(1), 5);
        double before = bot.roundWinProb(m, 1, 400, null, false);
        bot.learnSena(0, Sena.ANCHO_ESPADA, m.getMuestra());
        double after = bot.roundWinProb(m, 1, 400, null, false);
        check(after < before, "saber del ancho de espadas rival baja la probabilidad (" + before + " -> " + after + ")");
        // Envido revelado: restricción exacta.
        TrucoBot bot2 = new TrucoBot(new Random(2), 4);
        Card[] a2 = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO)); // 33
        Card[] b2 = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA)); // 30
        TrucoMatch m2 = match1v1(noFlor(), MUESTRA_COPA, a2, b2);
        ok(m2.act(0, ActionType.ENVIDO));
        ok(m2.act(1, ActionType.QUIERO));
        for (GameEvent e : m2.drainEvents()) bot2.observe(e, m2);
        double pe = bot2.envidoWinProb(m2, 1, 200);
        check(pe < 0.01, "conociendo el 33 rival, el bot sabe que pierde el envido (" + pe + ")");
    }

    private static void testTantosCantados() {
        Card[] a = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO)); // 33
        Card[] b = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA)); // 30
        TrucoMatch m = match1v1(noFlor(), MUESTRA_COPA, a, b);
        m.drainEvents();
        ok(m.act(0, ActionType.ENVIDO));
        ok(m.act(1, ActionType.QUIERO));
        int[] dicho = {-1, -1};
        int orden = 0;
        int primero = -1;
        for (GameEvent e : m.drainEvents()) {
            if (e.getType() == GameEvent.Type.TANTO_DECLARED) {
                dicho[e.getSeat()] = e.getValue();
                if (orden++ == 0) primero = e.getSeat();
                check(e.getAction() == ActionType.ENVIDO, "tanto de envido");
            }
        }
        eq(33, dicho[0], "tantos del asiento 0");
        eq(GameEvent.SON_BUENAS, dicho[1], "el que no le gana no muestra su número: dice son buenas");
        eq(m.getMano(), primero, "canta primero la mano");

        // 2 contra 2: canta la mano 33; los dos rivales no le ganan y dicen "son buenas"; su compañero se calla
        Card[] p0 = hand(c(7, Suit.ORO), c(6, Suit.ORO), c(4, Suit.BASTO));       // 33
        Card[] p1 = hand(c(7, Suit.ESPADA), c(1, Suit.BASTO), c(3, Suit.ESPADA)); // 30
        Card[] p2 = hand(c(2, Suit.BASTO), c(5, Suit.BASTO), c(12, Suit.ESPADA)); // 27
        Card[] p3 = hand(c(6, Suit.ESPADA), c(5, Suit.ESPADA), c(10, Suit.ORO));  // 31
        TrucoMatch m4 = new TrucoMatch(new MatchConfig(4, 30, false, MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN), 1L);
        ok(m4.startRound(3, deck(4, 3, MUESTRA_COPA, p0, p1, p2, p3)));
        m4.drainEvents();
        ok(m4.act(0, ActionType.ENVIDO));
        ok(m4.act(1, ActionType.QUIERO));
        int[] d4 = {-1, -1, -1, -1};
        int ganador = -1;
        for (GameEvent e : m4.drainEvents()) {
            if (e.getType() == GameEvent.Type.TANTO_DECLARED) d4[e.getSeat()] = e.getValue();
            if (e.getType() == GameEvent.Type.ENVIDO_RESOLVED) ganador = e.getSeat();
        }
        eq(33, d4[0], "2v2: la mano canta 33");
        eq(GameEvent.SON_BUENAS, d4[1], "2v2: el rival con 30 dice son buenas");
        eq(-1, d4[2], "2v2: el compañero del que gana no canta");
        eq(GameEvent.SON_BUENAS, d4[3], "2v2: el otro rival (31) también dice son buenas");
        eq(0, ganador, "2v2: gana la mano");
    }

    private static void testBotNivel6() {
        Random rnd = new Random(77);
        int wins6 = 0;
        int games = 160;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < games; i++) {
            boolean sixFirst = i % 2 == 0;
            TrucoMatch m = playBots(MatchConfig.defaults1v1(), rnd.nextLong(), sixFirst ? new int[] {6, 5} : new int[] {5, 6});
            if ((m.getWinner() == 0) == sixFirst) wins6++;
        }
        long ms = System.currentTimeMillis() - t0;
        System.out.println("        Nivel 6 vs nivel 5: " + wins6 + "/" + games + " victorias (" + (ms / games) + " ms por partido)");
        check(wins6 > games * 0.4, "el nivel 6 no debería jugar peor que el 5");
    }

    private static void testPartidoA20() {
        Random rnd = new Random(20);
        for (int i = 0; i < 40; i++) {
            TrucoMatch m = playBots(new MatchConfig(2, 20, i % 2 == 0), rnd.nextLong(), new int[] {3, 4});
            check(m.isOver(), "terminó");
            check(m.getScore(m.getWinner()) == 20, "el ganador llega justo a 20");
        }
        eq(10, new MatchConfig(2, 20, true).getHalf(), "10 malas y 10 buenas");
    }

    private static void testSalamancaFlor() {
        java.util.Set<Integer> conFlor = new java.util.HashSet<Integer>(java.util.Arrays.asList(2, 3));
        Random rnd = new Random(7);
        for (int iter = 0; iter < 400; iter++) {
            int dealer = iter % 4;      // con dealer 2 la mano es Ledesma: canta él primero y el Vizcacha contraflorea
            TrucoMatch m = new TrucoMatch(new MatchConfig(4, 30, true), rnd.nextLong());
            ok(m.startRound(dealer, Deck.withFlor(dealer, 4, conFlor, rnd)));
            CardRules r = m.getRules();
            for (int s = 0; s < 4; s++) {
                check(r.hasFlor(m.getDealtHand(s)) == conFlor.contains(s), "flor solo en 2 y 3 (asiento " + s + ")");
            }
            boolean achico = false;
            for (int guard = 0; guard < 20 && !m.isRoundFinished(); guard++) {
                int s = -1;
                for (int k = 0; k < 4; k++) if (!m.legalActions(k).isEmpty()) s = k;
                check(s >= 0, "alguien actúa");
                java.util.List<ActionType> legal = m.legalActions(s);
                if (s == 3 && legal.contains(ActionType.CON_FLOR_ME_ACHICO)) {
                    ok(m.act(s, ActionType.CON_FLOR_ME_ACHICO));
                    achico = true;
                    eq(3, m.getScore(0), "con flor me achico: 3 para el Vizcacha y su compañero");
                    eq(0, m.getScore(1), "nada para el Mandinga");
                    break;
                } else if (s == 2 && legal.contains(ActionType.CONTRAFLOR)) {
                    ok(m.act(s, ActionType.CONTRAFLOR));
                } else if (legal.contains(ActionType.FLOR)) {
                    check(s == 2 || s == 3, "solo cantan flor el Vizcacha o Ledesma");
                    ok(m.act(s, ActionType.FLOR));
                } else {
                    ok(m.playCard(s, 0));
                }
            }
            check(achico, "Ledesma se achicó (dealer " + dealer + ")");
        }
    }

    private static void testPrestigio() {
        eq(1, Prestigio.nivel(0), "nivel inicial");
        eq(2, Prestigio.nivel(60), "nivel 2");
        eq(5, Prestigio.nivel(10000), "tope");
        eq(0, Prestigio.aplicar(5, Prestigio.DERROTA), "no baja de 0");
        check(Prestigio.victoria(5) > Prestigio.victoria(1), "ganarle a mejores da más");
        check("Leyenda del Tapete".equals(Prestigio.rango(5)), "rango 5");
        Random r = new Random(3);
        for (int i = 0; i < 100; i++) {
            int n = Prestigio.nivelForastero(1 + i % 5, r);
            check(n >= 1 && n <= 4, "forastero entre 1 y 4");
        }
        eq(60, Prestigio.faltanParaSubir(0), "faltan 60");
    }

    /** Cada carta existe una sola vez entre manos, mesa y muestra (sin duplicaciones). */
    private static void checkRoundInvariants(TrucoMatch m, int n) {
        boolean[] seen = new boolean[40];
        int count = 0;
        for (int s = 0; s < n; s++) {
            for (Card x : m.getHand(s)) {
                check(!seen[x.getId()], "carta duplicada en mano: " + x);
                seen[x.getId()] = true;
                count++;
            }
            for (int t = 0; t < 3; t++) {
                Card x = m.getTrickCard(t, s);
                if (x != null) {
                    check(!seen[x.getId()], "carta duplicada en mesa: " + x);
                    seen[x.getId()] = true;
                    count++;
                }
            }
        }
        Card mu = m.getMuestra();
        check(!seen[mu.getId()], "la muestra está en juego");
        eq(3L * n, count, "cartas en juego");
    }
}
