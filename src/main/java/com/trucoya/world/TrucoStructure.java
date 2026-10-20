package com.trucoya.world;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.MarginedStructureStart;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Estructuras de una sola plantilla del mod. Tres modos:
 * <ul>
 *   <li>NORMAL: aparece suelta en el campo (pulperías, guarida). La plantilla depende del bioma.</li>
 *   <li>ALDEA: una pulpería al lado de CADA aldea. Usa la misma grilla que las aldeas, rearma la aldea
 *       (es determinística) y se ubica junto a la casa más de afuera, sin pisar nada, mirando al pueblo.</li>
 *   <li>UNICA: la Estancia del Caudillo, una sola por mundo, en una llanura o sabana plana a unos
 *       600-1200 bloques del origen. /locate la encuentra directamente.</li>
 * </ul>
 * Altura: se mide el terreno en toda la huella; si hay agua o es muy quebrado no se genera (o se prueba
 * otro lugar). El piso va sobre el punto más alto (en la estancia, sobre la mediana: la plantilla trae aire
 * para recortar lomas) y {@link CimientoPiece} rellena lo que falte abajo.
 */
public class TrucoStructure extends Structure<NoFeatureConfig> {

    public enum Modo { NORMAL, ALDEA, UNICA, PASO }

    static String clave(long seed, ChunkGenerator gen) {
        return seed + ":" + gen.getClass().getName() + ":" + gen.getBiomeSource().getClass().getName();
    }

    /** El chunk elegido para una estructura única (la estancia o el Paso de las Ánimas) en este mundo. */
    @Nullable
    public static ChunkPos elegidoUnico(Modo modo, long seed, ChunkGenerator gen) {
        return modo == Modo.PASO ? Paso.elegido(seed, gen) : modo == Modo.UNICA ? Estancia.elegido(seed, gen) : null;
    }

    /** Una plantilla concreta y cómo apoyarla en el terreno. */
    public static final class Plantilla {
        final String nombre;
        final int nivelSuelo;
        final int x0, z0, x1, z1;
        final int desnivelMax;
        final JigsawPiece pieza;
        final JigsawPattern patron;

        public Plantilla(String nombre, int nivelSuelo, int x0, int z0, int x1, int z1, int desnivelMax) {
            this.nombre = nombre;
            this.nivelSuelo = nivelSuelo;
            this.x0 = x0;
            this.z0 = z0;
            this.x1 = x1;
            this.z1 = z1;
            this.desnivelMax = desnivelMax;
            this.pieza = JigsawPiece.single("trucoya:" + nombre).apply(JigsawPattern.PlacementBehaviour.RIGID);
            this.patron = new JigsawPattern(new ResourceLocation("trucoya", nombre + "/start_pool"),
                    new ResourceLocation("minecraft", "empty"),
                    ImmutableList.of(Pair.of(JigsawPiece.single("trucoya:" + nombre), 1)),
                    JigsawPattern.PlacementBehaviour.RIGID);
        }
    }

    private final Modo modo;
    private final Map<Biome.Category, Plantilla> porBioma;
    @Nullable
    private final Plantilla porDefecto;
    private final int lejosDeAldeas;

    public TrucoStructure(Modo modo, Map<Biome.Category, Plantilla> porBioma, @Nullable Plantilla porDefecto,
                          int lejosDeAldeas) {
        super(NoFeatureConfig.CODEC);
        this.modo = modo;
        this.porBioma = new EnumMap<>(Biome.Category.class);
        this.porBioma.putAll(porBioma);
        this.porDefecto = porDefecto;
        this.lejosDeAldeas = lejosDeAldeas;
    }

    public Modo getModo() {
        return modo;
    }

    @Nullable
    Plantilla plantillaPara(Biome biome) {
        Plantilla p = porBioma.get(biome.getBiomeCategory());
        return p != null ? p : porDefecto;
    }

    @Override
    public GenerationStage.Decoration step() {
        return GenerationStage.Decoration.SURFACE_STRUCTURES;
    }

    @Override
    public IStartFactory<NoFeatureConfig> getStartFactory() {
        return Start::new;
    }

    // ------------------------------------------------------------------ dónde puede ir

    @Override
    protected boolean isFeatureChunk(ChunkGenerator gen, BiomeProvider biomes, long seed, SharedSeedRandom random,
                                     int chunkX, int chunkZ, Biome biome, ChunkPos chunkPos, NoFeatureConfig config) {
        switch (modo) {
            case UNICA:
            case PASO: {
                ChunkPos elegido = elegidoUnico(modo, seed, gen);
                return elegido != null && elegido.x == chunkX && elegido.z == chunkZ;
            }
            case ALDEA:
                // misma grilla que las aldeas: hay aldea si el bioma la admite
                return biome.getGenerationSettings().isValidStart(Structure.VILLAGE);
            default: {
                int x = chunkX * 16 + 8;
                int z = chunkZ * 16 + 8;
                int surface = gen.getBaseHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG);
                int floor = gen.getBaseHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG);
                if (surface != floor || surface <= gen.getSeaLevel()) {
                    return false;
                }
                if (lejosDeAldeas > 0) {
                    StructureSeparationSettings aldeas = gen.getSettings().getConfig(Structure.VILLAGE);
                    if (aldeas != null) {
                        for (int cx = chunkX - lejosDeAldeas; cx <= chunkX + lejosDeAldeas; cx++) {
                            for (int cz = chunkZ - lejosDeAldeas; cz <= chunkZ + lejosDeAldeas; cz++) {
                                ChunkPos p = Structure.VILLAGE.getPotentialFeatureChunk(aldeas, seed, random, cx, cz);
                                if (p.x == cx && p.z == cz) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }
    }

    /** La estancia es una sola: /locate va directo a ella (sin recorrer chunks). */
    @Override
    public BlockPos getNearestGeneratedFeature(IWorldReader world, StructureManager manager, BlockPos pos, int radius,
                                               boolean skipExisting, long seed, StructureSeparationSettings settings) {
        if (modo == Modo.UNICA || modo == Modo.PASO) {
            if (world instanceof ServerWorld) {
                ChunkGenerator gen = ((ServerWorld) world).getChunkSource().getGenerator();
                ChunkPos c = elegidoUnico(modo, seed, gen);
                int medio = modo == Modo.PASO ? Paso.TAM / 2 : 32;
                return c == null ? null : new BlockPos(c.x * 16 + medio, 64, c.z * 16 + medio);
            }
            return null;
        }
        return super.getNearestGeneratedFeature(world, manager, pos, radius, skipExisting, seed, settings);
    }

    // ------------------------------------------------------------------ armado

    /** Mide el terreno en un rectángulo: {min, max, mediana} o null si hay agua. */
    @Nullable
    static int[] medir(ChunkGenerator gen, int hx0, int hz0, int hx1, int hz1, int paso, boolean ignorarAgua) {
        List<Integer> alturas = new ArrayList<>();
        for (int x = hx0; x <= hx1 + paso - 1; x += paso) {
            for (int z = hz0; z <= hz1 + paso - 1; z += paso) {
                int cx = Math.min(x, hx1);
                int cz = Math.min(z, hz1);
                int surface = gen.getFirstFreeHeight(cx, cz, Heightmap.Type.WORLD_SURFACE_WG) - 1;
                int floor = gen.getFirstFreeHeight(cx, cz, Heightmap.Type.OCEAN_FLOOR_WG) - 1;
                if (!ignorarAgua && (surface != floor || surface < gen.getSeaLevel())) {
                    return null;
                }
                alturas.add(ignorarAgua ? Math.max(floor, gen.getSeaLevel()) : surface);
            }
        }
        Integer[] a = alturas.toArray(new Integer[0]);
        Arrays.sort(a);
        return new int[]{a[0], a[a.length - 1], a[a.length / 2]};
    }

    public static class Start extends MarginedStructureStart<NoFeatureConfig> {

        private final long semilla;

        public Start(Structure<NoFeatureConfig> structure, int chunkX, int chunkZ, MutableBoundingBox box,
                     int references, long seed) {
            super(structure, chunkX, chunkZ, box, references, seed);
            this.semilla = seed;
        }

        @Override
        public void generatePieces(DynamicRegistries registries, ChunkGenerator gen, TemplateManager templates,
                                   int chunkX, int chunkZ, Biome biome, NoFeatureConfig config) {
            TrucoStructure self = (TrucoStructure) this.getFeature();
            Plantilla p = self.plantillaPara(biome);
            if (p == null) {
                return;
            }
            switch (self.modo) {
                case ALDEA:
                    alLadoDeLaAldea(registries, gen, templates, chunkX, chunkZ, biome, p);
                    break;
                case UNICA:
                case PASO:
                    colocar(gen, templates, p, new BlockPos(chunkX * 16, 0, chunkZ * 16), Rotation.NONE, true);
                    break;
                default:
                    Rotation rot = Rotation.getRandom(this.random);
                    colocar(gen, templates, p, new BlockPos(chunkX * 16, 0, chunkZ * 16), rot, false);
            }
            if (!this.pieces.isEmpty()) {
                this.calculateBoundingBox();
            }
        }

        /** Coloca la plantilla con su esquina rotada en 'esquina'; devuelve false si el terreno no sirve. */
        private boolean colocar(ChunkGenerator gen, TemplateManager templates, Plantilla p, BlockPos esquina,
                                Rotation rot, boolean mediana) {
            MutableBoundingBox caja0 = p.pieza.getBoundingBox(templates, BlockPos.ZERO, rot);
            BlockPos pos = new BlockPos(esquina.getX() - caja0.x0, 0, esquina.getZ() - caja0.z0);
            MutableBoundingBox caja = p.pieza.getBoundingBox(templates, pos, rot);
            // huella rotada
            BlockPos a = pos.offset(net.minecraft.world.gen.feature.template.Template.calculateRelativePosition(
                    new net.minecraft.world.gen.feature.template.PlacementSettings().setRotation(rot),
                    new BlockPos(p.x0, 0, p.z0)));
            BlockPos b = pos.offset(net.minecraft.world.gen.feature.template.Template.calculateRelativePosition(
                    new net.minecraft.world.gen.feature.template.PlacementSettings().setRotation(rot),
                    new BlockPos(p.x1, 0, p.z1)));
            int hx0 = Math.min(a.getX(), b.getX());
            int hx1 = Math.max(a.getX(), b.getX());
            int hz0 = Math.min(a.getZ(), b.getZ());
            int hz1 = Math.max(a.getZ(), b.getZ());
            int[] m = medir(gen, hx0, hz0, hx1, hz1, mediana ? 8 : 3, mediana);
            if (m == null || (!mediana && m[1] - m[0] > p.desnivelMax)) {
                return false;
            }
            int suelo = mediana ? m[2] : m[1];
            AbstractVillagePiece pieza = new AbstractVillagePiece(templates, p.pieza, pos,
                    p.pieza.getGroundLevelDelta(), rot, caja);
            pieza.move(0, (suelo - p.nivelSuelo) - pieza.getBoundingBox().y0, 0);
            this.pieces.add(pieza);
            int fondo = pieza.getBoundingBox().y0;
            this.pieces.add(new CimientoPiece(hx0, hz0, hx1, hz1, fondo + (p.nivelSuelo > 4 ? p.nivelSuelo : 0)));
            return true;
        }

        /** Rearma la aldea de este chunk y pone la pulpería pegada a una de sus casas de las orillas. */
        private void alLadoDeLaAldea(DynamicRegistries registries, ChunkGenerator gen, TemplateManager templates,
                                     int chunkX, int chunkZ, Biome biome, Plantilla p) {
            StructureFeature<?, ?> aldea = null;
            for (Supplier<StructureFeature<?, ?>> s : biome.getGenerationSettings().structures()) {
                if (s.get().feature == Structure.VILLAGE) {
                    aldea = s.get();
                    break;
                }
            }
            StructureSeparationSettings sep = gen.getSettings().getConfig(Structure.VILLAGE);
            if (aldea == null || sep == null) {
                return;
            }
            StructureStart<?> inicio = aldea.generate(registries, gen, gen.getBiomeSource(), templates, semilla,
                    new ChunkPos(chunkX, chunkZ), biome, 0, sep);
            if (inicio == null || !inicio.isValid()) {
                return;
            }
            List<MutableBoundingBox> ocupado = new ArrayList<>();
            List<StructurePiece> casas = new ArrayList<>();
            for (StructurePiece sp : inicio.getPieces()) {
                ocupado.add(sp.getBoundingBox());
                if (sp instanceof AbstractVillagePiece
                        && ((AbstractVillagePiece) sp).getElement().toString().contains("houses")) {
                    casas.add(sp);
                }
            }
            if (casas.isEmpty()) {
                casas.addAll(inicio.getPieces());
            }
            MutableBoundingBox todo = inicio.getBoundingBox();
            double cx = (todo.x0 + todo.x1) / 2.0;
            double cz = (todo.z0 + todo.z1) / 2.0;
            // las casas más alejadas del centro primero
            casas.sort((u, v) -> Double.compare(dist2(v.getBoundingBox(), cx, cz), dist2(u.getBoundingBox(), cx, cz)));
            int origenX = chunkX * 16;
            int origenZ = chunkZ * 16;
            for (StructurePiece casa : casas) {
                MutableBoundingBox hb = casa.getBoundingBox();
                double dx = (hb.x0 + hb.x1) / 2.0 - cx;
                double dz = (hb.z0 + hb.z1) / 2.0 - cz;
                int[][] lados;
                // probar primero el lado que da hacia afuera del pueblo
                if (Math.abs(dx) >= Math.abs(dz)) {
                    lados = dx >= 0 ? new int[][]{{1, 0}, {0, 1}, {0, -1}} : new int[][]{{-1, 0}, {0, 1}, {0, -1}};
                } else {
                    lados = dz >= 0 ? new int[][]{{0, 1}, {1, 0}, {-1, 0}} : new int[][]{{0, -1}, {1, 0}, {-1, 0}};
                }
                for (int[] lado : lados) {
                    // el frente de la plantilla (norte) tiene que mirar hacia la casa / el pueblo
                    Rotation rot = lado[0] == 1 ? Rotation.COUNTERCLOCKWISE_90 : lado[0] == -1 ? Rotation.CLOCKWISE_90
                            : lado[1] == 1 ? Rotation.NONE : Rotation.CLOCKWISE_180;
                    MutableBoundingBox c0 = p.pieza.getBoundingBox(templates, BlockPos.ZERO, rot);
                    int w = c0.x1 - c0.x0;
                    int d = c0.z1 - c0.z0;
                    int ex, ez;
                    int gap = 5;
                    if (lado[0] == 1) {
                        ex = hb.x1 + gap;
                        ez = (hb.z0 + hb.z1) / 2 - d / 2;
                    } else if (lado[0] == -1) {
                        ex = hb.x0 - gap - w;
                        ez = (hb.z0 + hb.z1) / 2 - d / 2;
                    } else if (lado[1] == 1) {
                        ex = (hb.x0 + hb.x1) / 2 - w / 2;
                        ez = hb.z1 + gap;
                    } else {
                        ex = (hb.x0 + hb.x1) / 2 - w / 2;
                        ez = hb.z0 - gap - d;
                    }
                    // dentro del alcance de la estructura (7 chunks) y sin pisar la aldea
                    if (Math.abs(ex - origenX) > 112 || Math.abs(ex + w - origenX) > 112
                            || Math.abs(ez - origenZ) > 112 || Math.abs(ez + d - origenZ) > 112) {
                        continue;
                    }
                    MutableBoundingBox mia = new MutableBoundingBox(ex - 2, 0, ez - 2, ex + w + 2, 255, ez + d + 2);
                    boolean choca = false;
                    for (MutableBoundingBox o : ocupado) {
                        if (o.x0 <= mia.x1 && o.x1 >= mia.x0 && o.z0 <= mia.z1 && o.z1 >= mia.z0) {
                            choca = true;
                            break;
                        }
                    }
                    if (choca) {
                        continue;
                    }
                    if (colocar(gen, templates, p, new BlockPos(ex, 0, ez), rot, false)) {
                        return;
                    }
                }
            }
        }

        private static double dist2(MutableBoundingBox b, double cx, double cz) {
            double x = (b.x0 + b.x1) / 2.0 - cx;
            double z = (b.z0 + b.z1) / 2.0 - cz;
            return x * x + z * z;
        }
    }

    // ------------------------------------------------------------------ la estancia única

    /** Elige, una vez por mundo, el lugar de la Estancia del Caudillo. */
    static final class Estancia {
        private static final Map<String, Optional> CACHE = new ConcurrentHashMap<>();
        private static final int TAM = 64;

        /** Envoltorio porque ConcurrentHashMap no admite valores nulos. */
        private static final class Optional {
            final ChunkPos pos;

            Optional(ChunkPos pos) {
                this.pos = pos;
            }
        }

        @Nullable
        static ChunkPos elegido(long seed, ChunkGenerator gen) {
            // la clave incluye el tipo de generador: preguntar desde el Nether no puede ensuciar la del mundo de arriba
            return CACHE.computeIfAbsent(clave(seed, gen), k -> new Optional(buscar(seed, gen))).pos;
        }

        /**
         * Dónde va la estancia en este mundo. Siempre da un lugar:
         * <ol>
         *   <li>como siempre: un llano parejo (pradera o sabana) a 600-1200 bloques del centro, en un rumbo
         *       que sale de la semilla (los mundos que ya la tienen la siguen teniendo en el mismo lugar);</li>
         *   <li>si por ahí no hay, se prueba en los otros siete rumbos;</li>
         *   <li>si el mundo casi no tiene praderas, sirve cualquier tierra firme y pareja (bosque, desierto...);</li>
         *   <li>y en el peor caso, el lugar más parejo que se haya visto.</li>
         * </ol>
         */
        @Nullable
        private static ChunkPos buscar(long seed, ChunkGenerator gen) {
            Random r = new Random(seed ^ 0x45535441L);
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 600 + r.nextDouble() * 600;
            ChunkPos p = espiral(gen, ang, dist, 48, 2, true, 12);
            if (p != null) {
                return p;
            }
            for (int k = 1; k < 8 && p == null; k++) {
                p = espiral(gen, ang + k * Math.PI / 4, dist, 40, 4, true, 12);
            }
            for (int k = 0; k < 8 && p == null; k++) {
                p = espiral(gen, ang + k * Math.PI / 4, dist, 40, 4, false, 12);
            }
            for (int k = 0; k < 8 && p == null; k++) {
                p = espiral(gen, ang + k * Math.PI / 4, dist * 1.6D, 48, 4, false, 20);
            }
            if (p == null) {
                com.trucoya.TrucoYa.LOGGER.warn("Truco Ya: no hay un llano para la estancia; va en el lugar de siempre");
                p = new ChunkPos((int) Math.round(Math.cos(ang) * dist) >> 4, (int) Math.round(Math.sin(ang) * dist) >> 4);
            }
            return p;
        }

        /** Busca en espiral alrededor de un punto: el primer lugar parejo (≤ 6) o el más parejo (≤ tope). */
        @Nullable
        private static ChunkPos espiral(ChunkGenerator gen, double ang, double dist, int radioMax, int paso,
                                        boolean soloLlanos, int tope) {
            int c0x = (int) Math.round(Math.cos(ang) * dist) >> 4;
            int c0z = (int) Math.round(Math.sin(ang) * dist) >> 4;
            ChunkPos mejor = null;
            int mejorDesnivel = Integer.MAX_VALUE;
            for (int radio = 0; radio <= radioMax; radio += paso) {
                for (int dx = -radio; dx <= radio; dx += paso) {
                    for (int dz = -radio; dz <= radio; dz += paso) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radio) {
                            continue;
                        }
                        int cx = c0x + dx;
                        int cz = c0z + dz;
                        Biome b = gen.getBiomeSource().getNoiseBiome((cx << 2) + 2, 0, (cz << 2) + 2);
                        if (!sirve(b.getBiomeCategory(), soloLlanos)) {
                            continue;
                        }
                        int[] m = medir(gen, cx * 16, cz * 16, cx * 16 + TAM - 1, cz * 16 + TAM - 1, 16, false);
                        if (m == null) {
                            continue;
                        }
                        int desnivel = m[1] - m[0];
                        if (desnivel <= 6) {
                            return new ChunkPos(cx, cz);
                        }
                        if (desnivel < mejorDesnivel) {
                            mejorDesnivel = desnivel;
                            mejor = new ChunkPos(cx, cz);
                        }
                    }
                }
            }
            return mejorDesnivel <= tope ? mejor : null;
        }

        static boolean sirve(Biome.Category cat, boolean soloLlanos) {
            if (cat == Biome.Category.PLAINS || cat == Biome.Category.SAVANNA) {
                return true;
            }
            if (soloLlanos) {
                return false;
            }
            switch (cat) {
                case OCEAN: case RIVER: case BEACH: case NONE: case MUSHROOM: case NETHER: case THEEND:
                    return false;
                default:
                    return true;
            }
        }
    }

    // ------------------------------------------------------------------ el Paso de las Ánimas

    /**
     * Elige, una vez por mundo, el lugar del Paso de las Ánimas: a 520-900 bloques de la estancia, en tierra
     * firme y pareja, y si se puede a la orilla de un río (es un paso de río: ahí fue la batalla).
     */
    public static final class Paso {
        private static final Map<String, ChunkPos> CACHE = new ConcurrentHashMap<>();
        public static final int TAM = 34;

        private Paso() {
        }

        public static ChunkPos elegido(long seed, ChunkGenerator gen) {
            return CACHE.computeIfAbsent(clave(seed, gen), k -> buscar(seed, gen));
        }

        private static ChunkPos buscar(long seed, ChunkGenerator gen) {
            ChunkPos est = Estancia.elegido(seed, gen);
            int ex = est == null ? 0 : est.x * 16 + 32;
            int ez = est == null ? 0 : est.z * 16 + 32;
            Random r = new Random(seed ^ 0x50415353L);
            double ang0 = r.nextDouble() * Math.PI * 2;
            ChunkPos mejor = null;
            int mejorNota = Integer.MAX_VALUE;
            int medidas = 0;
            double[] distancias = {700, 560, 850, 620, 780};
            for (double dist : distancias) {
                for (int k = 0; k < 12; k++) {
                    double ang = ang0 + k * Math.PI * 2 / 12;
                    int c0x = (int) Math.round(ex + Math.cos(ang) * dist) >> 4;
                    int c0z = (int) Math.round(ez + Math.sin(ang) * dist) >> 4;
                    for (int radio = 0; radio <= 6; radio += 2) {
                        for (int dx = -radio; dx <= radio; dx += 2) {
                            for (int dz = -radio; dz <= radio; dz += 2) {
                                if (Math.max(Math.abs(dx), Math.abs(dz)) != radio) continue;
                                int cx = c0x + dx, cz = c0z + dz;
                                Biome b = gen.getBiomeSource().getNoiseBiome((cx << 2) + 2, 0, (cz << 2) + 2);
                                if (!Estancia.sirve(b.getBiomeCategory(), false)) continue;
                                if (++medidas > 700) return mejor != null ? mejor : new ChunkPos(c0x, c0z);
                                int[] m = medir(gen, cx * 16, cz * 16, cx * 16 + TAM - 1, cz * 16 + TAM - 1, 8, false);
                                if (m == null) continue;
                                int desnivel = m[1] - m[0];
                                if (desnivel > 8) continue;
                                boolean rio = rioCerca(gen, cx * 16 + TAM / 2, cz * 16 + TAM / 2);
                                if (rio && desnivel <= 4) return new ChunkPos(cx, cz);
                                int nota = desnivel + (rio ? 0 : 10);
                                if (nota < mejorNota) {
                                    mejorNota = nota;
                                    mejor = new ChunkPos(cx, cz);
                                }
                            }
                        }
                    }
                }
                if (mejor != null && mejorNota <= 4) return mejor;
            }
            if (mejor != null) return mejor;
            return new ChunkPos((ex + 700) >> 4, ez >> 4);
        }

        /** ¿Hay un río (o agua) a 24-56 bloques? */
        private static boolean rioCerca(ChunkGenerator gen, int x, int z) {
            for (int d = 24; d <= 56; d += 16) {
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    int bx = x + (int) Math.round(Math.cos(a) * d);
                    int bz = z + (int) Math.round(Math.sin(a) * d);
                    Biome b = gen.getBiomeSource().getNoiseBiome(bx >> 2, 0, bz >> 2);
                    if (b.getBiomeCategory() == Biome.Category.RIVER) return true;
                }
            }
            return false;
        }
    }
}
