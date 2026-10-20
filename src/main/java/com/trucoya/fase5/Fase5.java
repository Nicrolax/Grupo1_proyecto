package com.trucoya.fase5;

import com.trucoya.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;

/** Utilidades comunes de la Fase 5. */
public final class Fase5 {

    private Fase5() {
    }

    /** Suma tiempo de Confraternidad (se acumula hasta un tope). */
    public static void confraternidad(LivingEntity e, int ticks, int topeTicks) {
        EffectInstance actual = e.getEffect(ModEffects.CONFRATERNIDAD.get());
        int dur = Math.min(topeTicks, ticks + (actual != null ? actual.getDuration() : 0));
        e.addEffect(new EffectInstance(ModEffects.CONFRATERNIDAD.get(), dur, 0, false, true, true));
    }

    public static boolean tieneConfraternidad(LivingEntity e) {
        return e.hasEffect(ModEffects.CONFRATERNIDAD.get());
    }
}
