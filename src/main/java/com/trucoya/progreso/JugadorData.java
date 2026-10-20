package com.trucoya.progreso;

import com.trucoya.core.Prestigio;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

/**
 * Datos persistentes del jugador (sobreviven a la muerte): Prestigio Criollo y estadísticas
 * que usan los logros. Se guardan en el NBT "PlayerPersisted" que Forge copia al reaparecer.
 */
public final class JugadorData {

    private static final String ROOT = "trucoya";

    public static final String PRESTIGIO = "prestigio";
    public static final String GANADOS = "ganados";
    public static final String PERDIDOS = "perdidos";
    public static final String RACHA = "racha";
    public static final String MEJOR_RACHA = "mejor_racha";
    public static final String FLORES = "flores";
    public static final String FAROLES = "faroles";
    public static final String FORASTEROS = "forasteros";
    public static final String VIZCACHA = "vizcacha";
    /** Voz elegida para los cantos (número de {@link com.trucoya.core.Voz}). */
    public static final String VOZ = "voz";

    private JugadorData() {
    }

    private static CompoundNBT data(PlayerEntity player) {
        CompoundNBT persistent = player.getPersistentData();
        if (!persistent.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            persistent.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
        }
        CompoundNBT persisted = persistent.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, new CompoundNBT());
        }
        return persisted.getCompound(ROOT);
    }

    public static int get(PlayerEntity player, String key) {
        return data(player).getInt(key);
    }

    public static void set(PlayerEntity player, String key, int value) {
        data(player).putInt(key, value);
    }

    public static int increment(PlayerEntity player, String key) {
        int v = get(player, key) + 1;
        set(player, key, v);
        return v;
    }

    public static int prestigio(PlayerEntity player) {
        return get(player, PRESTIGIO);
    }

    public static int nivel(PlayerEntity player) {
        return Prestigio.nivel(prestigio(player));
    }

    /** Suma (o resta) prestigio y avisa si sube o baja de nivel. */
    public static void addPrestigio(ServerPlayerEntity player, int delta, String motivo) {
        int before = prestigio(player);
        int after = Prestigio.aplicar(before, delta);
        set(player, PRESTIGIO, after);
        int nb = Prestigio.nivel(before);
        int na = Prestigio.nivel(after);
        String sign = delta >= 0 ? "+" : "";
        player.displayClientMessage(new StringTextComponent("Prestigio Criollo " + sign + delta + " (" + motivo + ")")
                .withStyle(delta >= 0 ? TextFormatting.GOLD : TextFormatting.GRAY), true);
        if (na > nb) {
            player.sendMessage(new StringTextComponent("¡Subiste a Nivel " + na + ": " + Prestigio.rango(na) + "!")
                    .withStyle(TextFormatting.GOLD, TextFormatting.BOLD), player.getUUID());
            player.level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundCategory.PLAYERS, 0.8F, 1.0F);
        } else if (na < nb) {
            player.sendMessage(new StringTextComponent("Bajaste a Nivel " + na + ": " + Prestigio.rango(na) + ".")
                    .withStyle(TextFormatting.GRAY), player.getUUID());
        }
    }
}
