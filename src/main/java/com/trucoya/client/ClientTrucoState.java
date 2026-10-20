package com.trucoya.client;

import com.trucoya.core.TableSnapshot;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Estado del lado cliente: última mesa vista, globos de diálogo y avisos de señas. */
@OnlyIn(Dist.CLIENT)
public final class ClientTrucoState {

    private static final long BUBBLE_MS = 3000L;
    private static final long SENA_MS = 6000L;

    public static BlockPos tablePos;
    public static TableSnapshot snapshot;

    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();
    private static String senaText;
    private static boolean senaFromPartner;
    private static long senaUntil;

    public static final class Bubble {
        public final String text;
        final long until;

        Bubble(String text, long until) {
            this.text = text;
            this.until = until;
        }
    }

    static void addBubble(int entityId, String text) {
        BUBBLES.put(entityId, new Bubble(text, System.currentTimeMillis() + BUBBLE_MS));
    }

    public static Bubble bubbleFor(int entityId) {
        if (BUBBLES.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Bubble>> it = BUBBLES.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().until < now) {
                it.remove();
            }
        }
        return BUBBLES.get(entityId);
    }

    static void setSena(String text, boolean fromPartner) {
        senaText = text;
        senaFromPartner = fromPartner;
        senaUntil = System.currentTimeMillis() + SENA_MS;
    }

    /** Aviso de seña vigente, o null. */
    public static String currentSena() {
        return senaText != null && System.currentTimeMillis() < senaUntil ? senaText : null;
    }

    public static boolean senaFromPartner() {
        return senaFromPartner;
    }

    private ClientTrucoState() {
    }
}
