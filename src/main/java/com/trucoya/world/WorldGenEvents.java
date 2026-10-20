package com.trucoya.world;

import com.trucoya.TrucoYa;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Engancha las estructuras de la Fase 4 en la generación del mundo:
 *  1) las agrega a los biomas correspondientes;
 *  2) les da espaciado en el generador de cada mundo (Overworld, nunca mundos planos);
 *  3) la pulpería de aldea usa la misma grilla que las aldeas del mundo.
 *
 * Los campos privados se ubican por su TIPO (no por nombre) para que funcione igual en
 * desarrollo y en el .jar compilado, donde los nombres internos son distintos.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldGenEvents {

    private WorldGenEvents() {
    }


    // ------------------------------------------------------------------ biomas

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBiomeLoading(BiomeLoadingEvent event) {
        Biome.Category cat = event.getCategory();
        for (Biome.Category c : ModStructures.BIOMAS_PULPERIA) {
            if (c == cat) {
                event.getGeneration().getStructures().add(() -> ModStructures.CONFIGURED_PULPERIA);
            }
        }
        // biomas con aldeas: cada aldea trae su pulpería
        if (cat == Biome.Category.PLAINS || cat == Biome.Category.DESERT || cat == Biome.Category.SAVANNA
                || cat == Biome.Category.TAIGA || cat == Biome.Category.ICY) {
            event.getGeneration().getStructures().add(() -> ModStructures.CONFIGURED_PULPERIA_ALDEA);
        }
        // la estancia es una sola por mundo (la elige TrucoStructure.Estancia); casi siempre cae en una
        // pradera o sabana, pero en mundos sin praderas cerca puede ir en otra tierra firme
        if (TrucoStructure.Estancia.sirve(cat, false)) {
            event.getGeneration().getStructures().add(() -> ModStructures.CONFIGURED_ESTANCIA);
            event.getGeneration().getStructures().add(() -> ModStructures.CONFIGURED_PASO);
        }
        if (cat == Biome.Category.PLAINS || cat == Biome.Category.SAVANNA || cat == Biome.Category.FOREST
                || cat == Biome.Category.DESERT || cat == Biome.Category.TAIGA) {
            event.getGeneration().getStructures().add(() -> ModStructures.CONFIGURED_GUARIDA);
        }
    }

    // ------------------------------------------------------------------ espaciado por mundo

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!(event.getWorld() instanceof ServerWorld)) {
            return;
        }
        ServerWorld world = (ServerWorld) event.getWorld();
        if (world.dimension() != World.OVERWORLD) {
            return;
        }
        ChunkGenerator gen = world.getChunkSource().getGenerator();
        if (gen instanceof FlatChunkGenerator) {
            return;
        }
        DimensionStructuresSettings settings = gen.getSettings();
        try {
            Field campo = campoDeTipo(DimensionStructuresSettings.class, Map.class, false);
            if (campo == null) {
                TrucoYa.LOGGER.error("Truco Ya: no se encontró la tabla de estructuras del generador.");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<Structure<?>, StructureSeparationSettings> actual =
                    (Map<Structure<?>, StructureSeparationSettings>) campo.get(settings);
            Map<Structure<?>, StructureSeparationSettings> nuevo = new HashMap<>(actual);
            StructureSeparationSettings aldeas = actual.get(Structure.VILLAGE);
            agregar(nuevo, ModStructures.PULPERIA.get(), aldeas);
            agregar(nuevo, ModStructures.PULPERIA_ALDEA.get(), aldeas);
            agregar(nuevo, ModStructures.GUARIDA.get(), aldeas);
            agregar(nuevo, ModStructures.ESTANCIA.get(), aldeas);
            agregar(nuevo, ModStructures.PASO_ANIMAS.get(), aldeas);
            campo.set(settings, nuevo);
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrucoYa.LOGGER.error("Truco Ya: no se pudieron activar las estructuras", e);
        }
    }

    private static void agregar(Map<Structure<?>, StructureSeparationSettings> map, Structure<?> s,
                                StructureSeparationSettings aldeas) {
        StructureSeparationSettings sep = ModStructures.separacion(s, aldeas);
        if (sep != null) {
            map.put(s, sep);   // siempre la vigente (un mundo viejo puede tener guardada otra)
        }
    }

    // ------------------------------------------------------------------ reflexión

    private static Field campoDeTipo(Class<?> owner, Class<?> tipo, boolean estatico) {
        for (Field f : owner.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) == estatico && tipo.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

}
