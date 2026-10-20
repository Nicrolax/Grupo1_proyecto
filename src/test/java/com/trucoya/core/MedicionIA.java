package com.trucoya.core;

import java.util.Random;

/**
 * Mide cuánto tarda la IA de nivel 5 en decidir (requisito RNF-R4): 30 partidos 2 contra 2 entre bots.
 * Uso: java -cp out com.trucoya.core.MedicionIA
 */
public final class MedicionIA {

    private MedicionIA() {
    }

    public static void main(String[] a) {
        long maxNs = 0, total = 0; int n = 0;
        for (int g = 0; g < 30; g++) {
            TrucoMatch m = new TrucoMatch(MatchConfig.defaults2v2(), g);
            TrucoBot[] b = new TrucoBot[4];
            for (int s = 0; s < 4; s++) b[s] = new TrucoBot(new Random(g * 10 + s), 5);
            m.start();
            int guard = 0;
            while (!m.isOver() && guard++ < 5000) {
                if (m.isRoundFinished()) { m.startNextRound(); continue; }
                int t = -1;
                for (int s = 0; s < 4; s++) if (!m.legalActions(s).isEmpty()) { t = s; break; }
                if (t < 0) break;
                long t0 = System.nanoTime();
                b[t].play(m, t);
                long d = System.nanoTime() - t0;
                maxNs = Math.max(maxNs, d); total += d; n++;
                for (GameEvent e : m.drainEvents()) for (TrucoBot x : b) x.observe(e, m);
            }
        }
        System.out.printf("decisiones=%d promedio=%.2f ms maximo=%.2f ms%n", n, total / 1e6 / n, maxNs / 1e6);
    }
}
