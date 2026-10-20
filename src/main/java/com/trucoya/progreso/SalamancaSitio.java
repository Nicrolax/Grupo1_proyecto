package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.world.ModStructures;
import com.trucoya.world.TrucoStructure;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

/**
 * El Paso de las Ánimas en el mundo: dónde quedó la plantilla (sin rotar) y el cambio entre la tapera en ruinas
 * (paso_animas.nbt, la que generó el mundo) y la Salamanca de noche (salamanca.nbt). Las coordenadas locales
 * coinciden con tools/estructuras_paso.py.
 */
public final class SalamancaSitio {

    public static final BlockPos MESA = new BlockPos(17, 3, 17);
    public static final BlockPos SILLA_N = new BlockPos(17, 3, 16);   // el jugador (de espaldas a la puerta)
    public static final BlockPos SILLA_S = new BlockPos(17, 3, 18);   // el Vizcacha, su compañero
    public static final BlockPos SILLA_O = new BlockPos(16, 3, 17);   // Ledesma
    public static final BlockPos SILLA_E = new BlockPos(18, 3, 17);   // el Mandinga
    public static final BlockPos PUERTA = new BlockPos(17, 3, 12);
    public static final BlockPos LLEGADA = new BlockPos(17, 3, 3);
    public static final BlockPos CRUZ = new BlockPos(28, 3, 17);
    public static final BlockPos OMBU = new BlockPos(6, 3, 7);
    /** Donde quedan parados al amanecer (ya sin mesa ni sillas). */
    public static final Vector3d PIE_VIZCACHA = new Vector3d(18.6D, 3.0D, 19.5D);
    public static final Vector3d PIE_LEDESMA = new Vector3d(16.0D, 3.0D, 19.5D);
    public static final Vector3d PIE_JUGADOR = new Vector3d(17.5D, 3.0D, 15.0D);
    private static final BlockPos[] FLORES = {new BlockPos(27, 3, 17), new BlockPos(29, 3, 17), new BlockPos(27, 3, 18),
            new BlockPos(28, 3, 18), new BlockPos(29, 3, 18), new BlockPos(28, 3, 19), new BlockPos(27, 3, 19),
            new BlockPos(29, 3, 19)};

    private static final ResourceLocation RUINA = new ResourceLocation(TrucoYa.MOD_ID, "paso_animas");
    private static final ResourceLocation NOCHE = new ResourceLocation(TrucoYa.MOD_ID, "salamanca");
    /** Mandar a los clientes, sin reacciones de los vecinos ni cosas que se caen al cambiar la plantilla. */
    private static final int BANDERAS = 2 | 16 | 32;

    private static long falloEn = Long.MIN_VALUE;

    private SalamancaSitio() {
    }

    public static BlockPos mundo(BlockPos origen, BlockPos local) {
        return origen.offset(local);
    }

    public static Vector3d mundo(BlockPos origen, Vector3d local) {
        return new Vector3d(origen.getX() + local.x, origen.getY() + local.y, origen.getZ() + local.z);
    }

    public static Vector3d centro(BlockPos origen, BlockPos local) {
        return new Vector3d(origen.getX() + local.getX() + 0.5D, origen.getY() + local.getY(), origen.getZ() + local.getZ() + 0.5D);
    }

    /** El chunk donde el mundo puso el Paso (aproximado: sirve para /locate y para el farol). */
    public static ChunkPos chunk(ServerWorld w) {
        ServerWorld over = w.getServer().overworld();   // el Paso está en el mundo de arriba, se pregunte desde donde se pregunte
        return TrucoStructure.Paso.elegido(over.getSeed(), over.getChunkSource().getGenerator());
    }

    /** Centro aproximado del Paso, sin cargar nada. */
    public static BlockPos aproximado(ServerWorld w) {
        ChunkPos c = chunk(w);
        return new BlockPos(c.x * 16 + TrucoStructure.Paso.TAM / 2, 64, c.z * 16 + TrucoStructure.Paso.TAM / 2);
    }

    /**
     * Esquina de la plantilla en el mundo. La primera vez carga el chunk del Paso (lo genera si hace falta) y
     * lo anota en los datos del mundo.
     */
    @Nullable
    public static BlockPos origen(ServerWorld w) {
        w = w.getServer().overworld();
        Datos d = Datos.de(w);
        if (d.origen != null) {
            return d.origen;
        }
        if (falloEn == w.getSeed()) {
            return null;   // ya se probó en esta sesión y no se pudo: no se insiste cada segundo
        }
        falloEn = w.getSeed();
        ChunkPos c = chunk(w);
        try {
            w.getChunk(c.x, c.z);   // asegura que la estructura exista
            Optional<? extends StructureStart<?>> inicio = w.structureFeatureManager()
                    .startsForFeature(SectionPos.of(c, 0), ModStructures.PASO_ANIMAS.get()).findFirst();
            if (inicio.isPresent()) {
                for (StructurePiece pieza : inicio.get().getPieces()) {
                    if (pieza instanceof AbstractVillagePiece) {
                        d.origen = ((AbstractVillagePiece) pieza).getPosition().immutable();
                        d.setDirty();
                        falloEn = Long.MIN_VALUE;
                        return d.origen;
                    }
                }
            }
        } catch (RuntimeException e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo ubicar el Paso de las Ánimas", e);
        }
        // El mundo se generó antes de que existiera el Paso: se levanta ahora, en el mismo lugar.
        try {
            int x0 = c.x * 16, z0 = c.z * 16, tam = TrucoStructure.Paso.TAM;
            for (int cx = x0 >> 4; cx <= (x0 + tam) >> 4; cx++) {
                for (int cz = z0 >> 4; cz <= (z0 + tam) >> 4; cz++) {
                    w.getChunk(cx, cz);
                }
            }
            java.util.List<Integer> alturas = new java.util.ArrayList<>();
            for (int x = x0; x < x0 + tam; x += 4) {
                for (int z = z0; z < z0 + tam; z += 4) {
                    alturas.add(w.getHeight(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
                }
            }
            java.util.Collections.sort(alturas);
            int suelo = Math.max(w.getSeaLevel(), alturas.get(alturas.size() / 2));
            BlockPos o = new BlockPos(x0, suelo - 2, z0);
            if (poner(w, o, false)) {
                d.origen = o;
                d.setDirty();
                falloEn = Long.MIN_VALUE;
                TrucoYa.LOGGER.info("Truco Ya: el Paso de las Ánimas se levantó en {} (mundo viejo)", o);
                return o;
            }
        } catch (RuntimeException e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo levantar el Paso de las Ánimas", e);
        }
        return null;
    }

    /** Pone la Salamanca (de noche) o la tapera en ruinas. */
    public static boolean poner(ServerWorld w, BlockPos origen, boolean noche) {
        try {
            Template t = w.getStructureManager().get(noche ? NOCHE : RUINA);
            if (t == null) {
                TrucoYa.LOGGER.error("Truco Ya: falta la plantilla {}", noche ? NOCHE : RUINA);
                return false;
            }
            PlacementSettings ps = new PlacementSettings().setRotation(Rotation.NONE).setIgnoreEntities(true);
            t.placeInWorld(w, origen, origen, ps, new Random(1840L), BANDERAS);
            Datos d = Datos.de(w);
            d.noche = noche;
            d.setDirty();
            return true;
        } catch (RuntimeException e) {
            TrucoYa.LOGGER.error("Truco Ya: no se pudo cambiar el Paso de las Ánimas", e);
            return false;
        }
    }

    /** Flores sobre la tumba del Negro Ledesma (después de que su alma queda libre). */
    public static void flores(ServerWorld w, BlockPos origen) {
        BlockState[] tipos = {Blocks.POPPY.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
                Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState()};
        for (int i = 0; i < FLORES.length; i++) {
            BlockPos p = mundo(origen, FLORES[i]);
            BlockState aca = w.getBlockState(p);
            if (aca.isAir() || aca.getMaterial().isReplaceable()) {
                BlockState flor = tipos[i % tipos.length];
                if (flor.canSurvive(w, p)) {
                    w.setBlock(p, flor, 3);
                }
            }
        }
    }

    /** Si el juego se cortó con la Salamanca armada, vuelve a ser tapera. */
    public static void arreglarSiQuedoArmada(ServerWorld w) {
        Datos d = Datos.de(w);
        if (d.noche && d.origen != null) {
            TrucoYa.LOGGER.info("Truco Ya: la Salamanca quedó armada de la vez anterior; vuelve a ser tapera.");
            w.getChunk(d.origen.getX() >> 4, d.origen.getZ() >> 4);
            w.getChunk((d.origen.getX() + 33) >> 4, (d.origen.getZ() + 33) >> 4);
            poner(w, d.origen, false);
        }
    }

    // ------------------------------------------------------------------ datos del mundo

    static final class Datos extends WorldSavedData {
        static final String NOMBRE = TrucoYa.MOD_ID + "_salamanca";
        @Nullable
        BlockPos origen;
        boolean noche;

        Datos() {
            super(NOMBRE);
        }

        static Datos de(ServerWorld w) {
            return w.getDataStorage().computeIfAbsent(Datos::new, NOMBRE);
        }

        @Override
        public void load(CompoundNBT nbt) {
            origen = nbt.contains("Origen") ? BlockPos.of(nbt.getLong("Origen")) : null;
            noche = nbt.getBoolean("Noche");
        }

        @Override
        public CompoundNBT save(CompoundNBT nbt) {
            if (origen != null) nbt.putLong("Origen", origen.asLong());
            nbt.putBoolean("Noche", noche);
            return nbt;
        }
    }
}
