package com.trucoya.fase5;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

/**
 * Confraternidad: el efecto del mate compartido.
 * <ul>
 *   <li>Recupera medio corazón cada 5 segundos (más seguido con más nivel).</li>
 *   <li>En la mesa de truco: las señas son más disimuladas (a los rivales les cuesta el doble verlas)
 *       y el partido ganado da un 20 % más de Prestigio Criollo.</li>
 * </ul>
 */
public class ConfraternidadEffect extends Effect {

    public ConfraternidadEffect() {
        super(EffectType.BENEFICIAL, 0x5E8C31);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int k = 100 >> Math.min(amplifier, 3);
        return k <= 0 || duration % k == 0;
    }
}
