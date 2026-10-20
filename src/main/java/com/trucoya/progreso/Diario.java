package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Los desafíos del día: cada día (de los de verdad, según el reloj del servidor) cada jugador tiene tres desafíos
 * chicos, distintos para cada uno. Cada desafío cumplido da prestigio y esmeraldas; los tres juntos, un premio más
 * y suman a la racha de días seguidos.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Diario {

    public static final int PARTIDOS = 0, ENVIDO = 1, FLOR = 2, FAROL = 3, VALE_CUATRO = 4, RETRUCO = 5, CUADRERA = 6,
            MATE = 7, PEDIDO = 8, FORASTERO = 9, PULPERIA = 10;

    /** Texto, meta y si es de los difíciles (sale como mucho uno por día). */
    private static final String[] TEXTO = {
            "Ganá dos partidos de truco", "Ganá tres envidos", "Cantá una flor", "Hacé un farol: cantá truco con mano floja y que no te quieran",
            "Ganá una mano con el vale cuatro querido", "Ganá una mano con el retruco querido", "Ganá una carrera cuadrera",
            "Cebá y convidá tres mates", "Cumplí un pedido de un paisano", "Ganale un partido a un forastero",
            "Ganá un partido en una pulpería"};
    private static final int[] META = {2, 3, 1, 1, 1, 1, 1, 3, 1, 1, 1};
    private static final boolean[] DIFICIL = {false, false, true, false, true, false, true, false, false, true, false};

    private static final String RAIZ = "trucoya_diario";
    private static final String RACHA = "diario_racha";
    private static final String MEJOR = "diario_mejor_racha";
    private static final String COMPLETOS = "diario_completos";

    private Diario() {
    }

    private static long hoy() {
        return LocalDate.now().toEpochDay();
    }

    private static CompoundNBT datos(PlayerEntity p) {
        CompoundNBT pers = p.getPersistentData();
        if (!pers.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            pers.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
        }
        CompoundNBT d = pers.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        if (!d.contains(RAIZ, 10)) {
            d.put(RAIZ, new CompoundNBT());
        }
        return d.getCompound(RAIZ);
    }

    /** Los desafíos de hoy de ese jugador (si cambió el día, se sortean otros). */
    private static CompoundNBT deHoy(PlayerEntity p) {
        CompoundNBT d = datos(p);
        long hoy = hoy();
        if (d.getLong("Dia") != hoy || d.getIntArray("Retos").length != 3) {
            // si ayer no completó los tres, la racha se corta
            if (d.getLong("Dia") != hoy - 1 || !d.getBoolean("Completo")) {
                if (d.getLong("Dia") != hoy) JugadorData.set(p, RACHA, 0);
            }
            Random r = new Random(hoy * 1_000_003L ^ p.getUUID().getLeastSignificantBits() ^ p.getUUID().getMostSignificantBits());
            int[] retos = new int[3];
            boolean dificil = false;
            for (int i = 0; i < 3; i++) {
                int x;
                do {
                    x = r.nextInt(TEXTO.length);
                } while (contiene(retos, i, x) || (DIFICIL[x] && dificil));
                dificil |= DIFICIL[x];
                retos[i] = x;
            }
            d.putLong("Dia", hoy);
            d.putIntArray("Retos", retos);
            d.putIntArray("Avance", new int[3]);
            d.putBoolean("Completo", false);
        }
        return d;
    }

    private static boolean contiene(int[] a, int hasta, int x) {
        for (int i = 0; i < hasta; i++) if (a[i] == x) return true;
        return false;
    }

    /** Pasó algo que puede contar para un desafío de hoy. */
    public static void evento(ServerPlayerEntity p, int tipo) {
        CompoundNBT d = deHoy(p);
        int[] retos = d.getIntArray("Retos");
        int[] avance = d.getIntArray("Avance");
        for (int i = 0; i < 3; i++) {
            if (retos[i] != tipo || avance[i] >= META[tipo]) continue;
            avance[i]++;
            d.putIntArray("Avance", avance);
            if (avance[i] >= META[tipo]) {
                cumplido(p, d, tipo);
            } else {
                p.displayClientMessage(new StringTextComponent("Desafío del día: " + TEXTO[tipo] + " (" + avance[i] + "/"
                        + META[tipo] + ")").withStyle(TextFormatting.YELLOW), true);
            }
            return;
        }
    }

    private static void cumplido(ServerPlayerEntity p, CompoundNBT d, int tipo) {
        dar(p, new ItemStack(Items.EMERALD, 2));
        JugadorData.addPrestigio(p, 25, "Desafío del día");
        p.sendMessage(new StringTextComponent("★ Desafío del día cumplido: " + TEXTO[tipo] + "  (+2 esmeraldas)")
                .withStyle(TextFormatting.GOLD), p.getUUID());
        p.level.playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8F, 0.8F);
        int[] retos = d.getIntArray("Retos");
        int[] avance = d.getIntArray("Avance");
        for (int i = 0; i < 3; i++) {
            if (avance[i] < META[retos[i]]) return;
        }
        // los tres
        d.putBoolean("Completo", true);
        int racha = JugadorData.increment(p, RACHA);
        if (racha > JugadorData.get(p, MEJOR)) JugadorData.set(p, MEJOR, racha);
        JugadorData.increment(p, COMPLETOS);
        dar(p, new ItemStack(Items.EMERALD, 5));
        JugadorData.addPrestigio(p, 40, "Los tres desafíos del día");
        p.sendMessage(new StringTextComponent("★★★ ¡Los tres desafíos de hoy! +5 esmeraldas. Racha: " + racha
                + (racha == 1 ? " día." : " días seguidos.")).withStyle(TextFormatting.GOLD, TextFormatting.BOLD), p.getUUID());
        p.level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8F, 1.2F);
        if (racha >= 7) {
            Logros.award(p, Logros.SIETE_DIAS);
        }
    }

    private static void dar(ServerPlayerEntity p, ItemStack st) {
        if (!p.inventory.add(st)) {
            p.drop(st, false);
        }
    }

    /** Para la Libreta: "texto\navance\nmeta" de cada uno. */
    public static List<String> paraLibreta(ServerPlayerEntity p) {
        CompoundNBT d = deHoy(p);
        int[] retos = d.getIntArray("Retos");
        int[] avance = d.getIntArray("Avance");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            out.add(TEXTO[retos[i]] + "\n" + Math.min(avance[i], META[retos[i]]) + "\n" + META[retos[i]]);
        }
        return out;
    }

    public static int racha(ServerPlayerEntity p) {
        deHoy(p);
        return JugadorData.get(p, RACHA);
    }

    public static int mejorRacha(PlayerEntity p) {
        return JugadorData.get(p, MEJOR);
    }

    public static int completos(PlayerEntity p) {
        return JugadorData.get(p, COMPLETOS);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
        boolean nuevo = datos(p).getLong("Dia") != hoy();
        deHoy(p);
        if (nuevo) {
            p.sendMessage(new StringTextComponent("Hay desafíos del día nuevos: miralos en la Libreta (pestaña Pedidos).")
                    .withStyle(TextFormatting.YELLOW), p.getUUID());
        }
    }
}
