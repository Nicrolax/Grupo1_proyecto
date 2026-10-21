package com.trucoya.registry;

import com.trucoya.TrucoYa;
import com.trucoya.core.ActionType;
import com.trucoya.core.Voz;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.Map;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TrucoYa.MOD_ID);

    /** voz.<voz>.<canto>: cinco voces (hombre, gaucho, viejo, mujer, paisana) × 13 cantos. */
    private static final Map<Voz, Map<ActionType, RegistryObject<SoundEvent>>> VOCES = new EnumMap<>(Voz.class);

    static {
        for (Voz v : Voz.values()) {
            Map<ActionType, RegistryObject<SoundEvent>> m = new EnumMap<>(ActionType.class);
            for (ActionType a : ActionType.values()) {
                if (a.getSoundKey() != null) {
                    m.put(a, register("voz." + v.clave() + "." + a.getSoundKey()));
                }
            }
            VOCES.put(v, m);
        }
    }

    public static final RegistryObject<SoundEvent> CARD_PLAY = register("card_play");
    public static final RegistryObject<SoundEvent> CARD_SHUFFLE = register("card_shuffle");
    public static final RegistryObject<SoundEvent> POROTO = register("poroto");

    // Discos de música criolla (Fase 5)
    public static final RegistryObject<SoundEvent> DISCO_MILONGA = register("disco.milonga");
    public static final RegistryObject<SoundEvent> DISCO_CANDOMBE = register("disco.candombe");
    public static final RegistryObject<SoundEvent> DISCO_VALS = register("disco.vals");
    public static final RegistryObject<SoundEvent> DISCO_PAYADA = register("disco.payada");

    /** Las felicitaciones de los cuatro Fundadores al vencer al Mandinga (sus voces, clonadas de sus grabaciones). */
    public static final String[] FUNDADORES = {"nicolas", "agustin", "matias", "thiago"};
    @SuppressWarnings("unchecked")
    public static final RegistryObject<SoundEvent>[] FELICITA = new RegistryObject[FUNDADORES.length];

    static {
        for (int i = 0; i < FUNDADORES.length; i++) {
            FELICITA[i] = register("fundadores.felicita_" + FUNDADORES[i]);
        }
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(TrucoYa.MOD_ID, name)));
    }

    /** Voz del canto, o null si la acción no tiene voz. */
    public static SoundEvent voice(ActionType action, Voz voz) {
        RegistryObject<SoundEvent> ro = VOCES.get(voz).get(action);
        return ro == null ? null : ro.get();
    }

    private ModSounds() {
    }
}
