package com.trucoya.registry;

import com.trucoya.TrucoYa;
import com.trucoya.fase5.ConfraternidadEffect;
import net.minecraft.potion.Effect;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModEffects {

    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, TrucoYa.MOD_ID);

    /** El efecto del mate compartido. */
    public static final RegistryObject<Effect> CONFRATERNIDAD = EFFECTS.register("confraternidad", ConfraternidadEffect::new);

    private ModEffects() {
    }
}
