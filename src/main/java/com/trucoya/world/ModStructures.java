package com.trucoya.world;

import com.google.common.collect.ImmutableMap;
import com.trucoya.TrucoYa;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Estructuras del mundo:
 *  - trucoya:pulperia         pulperías sueltas, una arquitectura distinta por bioma
 *  - trucoya:pulperia_aldea   una pulpería al lado de cada aldea (con el estilo del bioma de la aldea)
 *  - trucoya:guarida          bar clandestino subterráneo con guardias
 *  - trucoya:estancia         la Estancia del Caudillo: UNA sola por mundo
 *  - trucoya:paso_animas      el Paso de las Ánimas (la tapera en ruinas donde está la Salamanca): UNO por mundo
 */
public final class ModStructures {

    private ModStructures() {
    }

    public static final DeferredRegister<Structure<?>> STRUCTURES =
            DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, TrucoYa.MOD_ID);

    // ------------------------------------------------------------ plantillas (nombre, suelo, huella, desnivel)
    private static TrucoStructure.Plantilla pul(String estilo, int sx, int sz) {
        return new TrucoStructure.Plantilla("pulperia/" + estilo, 2, 0, 0, sx - 1, sz - 1, 5);
    }

    private static Map<Biome.Category, TrucoStructure.Plantilla> pulperiasPorBioma() {
        return ImmutableMap.<Biome.Category, TrucoStructure.Plantilla>builder()
                .put(Biome.Category.PLAINS, pul("llanura", 26, 25))
                .put(Biome.Category.SAVANNA, pul("sabana", 25, 24))
                .put(Biome.Category.DESERT, pul("desierto", 27, 25))
                .put(Biome.Category.MESA, pul("mesa", 27, 25))
                .put(Biome.Category.TAIGA, pul("taiga", 24, 22))
                .put(Biome.Category.FOREST, pul("bosque", 24, 22))
                .put(Biome.Category.ICY, pul("nevada", 24, 22))
                .put(Biome.Category.SWAMP, pul("pantano", 23, 23))
                .put(Biome.Category.JUNGLE, pul("selva", 23, 23))
                .put(Biome.Category.EXTREME_HILLS, new TrucoStructure.Plantilla("pulperia/serrana", 2, 0, 0, 25, 23, 7))
                .build();
    }

    /** Biomas con pulpería suelta. */
    public static final Biome.Category[] BIOMAS_PULPERIA = {Biome.Category.PLAINS, Biome.Category.SAVANNA,
            Biome.Category.DESERT, Biome.Category.MESA, Biome.Category.TAIGA, Biome.Category.FOREST,
            Biome.Category.ICY, Biome.Category.SWAMP, Biome.Category.JUNGLE, Biome.Category.EXTREME_HILLS};

    public static final RegistryObject<Structure<NoFeatureConfig>> PULPERIA = STRUCTURES.register("pulperia",
            () -> new TrucoStructure(TrucoStructure.Modo.NORMAL, pulperiasPorBioma(), null, 4));
    public static final RegistryObject<Structure<NoFeatureConfig>> PULPERIA_ALDEA = STRUCTURES.register("pulperia_aldea",
            () -> new TrucoStructure(TrucoStructure.Modo.ALDEA, pulperiasPorBioma(), pul("llanura", 26, 25), 0));
    public static final RegistryObject<Structure<NoFeatureConfig>> GUARIDA = STRUCTURES.register("guarida",
            () -> new TrucoStructure(TrucoStructure.Modo.NORMAL, ImmutableMap.of(),
                    new TrucoStructure.Plantilla("guarida", 20, 18, 0, 26, 7, 4), 3));
    public static final RegistryObject<Structure<NoFeatureConfig>> ESTANCIA = STRUCTURES.register("estancia",
            () -> new TrucoStructure(TrucoStructure.Modo.UNICA, ImmutableMap.of(),
                    new TrucoStructure.Plantilla("estancia", 2, 0, 0, 63, 63, 12), 0));

    public static final RegistryObject<Structure<NoFeatureConfig>> PASO_ANIMAS = STRUCTURES.register("paso_animas",
            () -> new TrucoStructure(TrucoStructure.Modo.PASO, ImmutableMap.of(),
                    new TrucoStructure.Plantilla("paso_animas", 2, 0, 0, TrucoStructure.Paso.TAM - 1,
                            TrucoStructure.Paso.TAM - 1, 8), 0));

    /** Separación (espaciado promedio, mínimo, sal) en chunks. La de la aldea se copia de las aldeas del mundo. */
    public static final Map<String, StructureSeparationSettings> SEPARACION = ImmutableMap.of(
            "pulperia", new StructureSeparationSettings(22, 8, 1946271803),
            "guarida", new StructureSeparationSettings(28, 10, 1868230817),
            // la estancia se decide por semilla (una sola): se revisa cada chunk, pero solo uno da "sí"
            "estancia", new StructureSeparationSettings(1, 0, 1730152457),
            "paso_animas", new StructureSeparationSettings(1, 0, 1597463007));

    public static StructureFeature<?, ?> CONFIGURED_PULPERIA;
    public static StructureFeature<?, ?> CONFIGURED_PULPERIA_ALDEA;
    public static StructureFeature<?, ?> CONFIGURED_GUARIDA;
    public static StructureFeature<?, ?> CONFIGURED_ESTANCIA;
    public static StructureFeature<?, ?> CONFIGURED_PASO;

    public static IStructurePieceType CIMIENTO;

    /** Se llama en FMLCommonSetupEvent#enqueueWork (hilo principal). */
    public static void setup() {
        CIMIENTO = IStructurePieceType.setPieceId(CimientoPiece::new, "trucoya:cimiento");
        CONFIGURED_PULPERIA = registrar(PULPERIA.get(), "pulperia");
        CONFIGURED_PULPERIA_ALDEA = registrar(PULPERIA_ALDEA.get(), "pulperia_aldea");
        CONFIGURED_GUARIDA = registrar(GUARIDA.get(), "guarida");
        CONFIGURED_ESTANCIA = registrar(ESTANCIA.get(), "estancia");
        CONFIGURED_PASO = registrar(PASO_ANIMAS.get(), "paso_animas");
    }

    private static StructureFeature<?, ?> registrar(Structure<NoFeatureConfig> structure, String nombre) {
        // necesario para que el juego pueda volver a cargar la estructura al abrir el mundo
        Structure.STRUCTURES_REGISTRY.put(TrucoYa.MOD_ID + ":" + nombre, structure);
        StructureFeature<?, ?> configured = structure.configured(IFeatureConfig.NONE);
        Registry.register(WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE,
                new ResourceLocation(TrucoYa.MOD_ID, "configured_" + nombre), configured);
        return configured;
    }

    /** Separación para este mundo; la pulpería de aldea usa exactamente la grilla de las aldeas. */
    @Nullable
    public static StructureSeparationSettings separacion(Structure<?> s, @Nullable StructureSeparationSettings aldeas) {
        ResourceLocation id = s.getRegistryName();
        if (id == null) {
            return null;
        }
        if ("pulperia_aldea".equals(id.getPath())) {
            return aldeas == null ? null
                    : new StructureSeparationSettings(aldeas.spacing(), aldeas.separation(), aldeas.salt());
        }
        return SEPARACION.get(id.getPath());
    }
}
