package com.trucoya.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.template.TemplateManager;

import java.util.Random;

/**
 * Rellena con tierra el hueco que queda bajo el piso de una construcción cuando el terreno
 * baja (pendientes, barrancas). Solo reemplaza aire, agua y plantas: nunca toca terreno firme.
 */
public class CimientoPiece extends StructurePiece {

    private static final int PROFUNDIDAD = 12;

    public CimientoPiece(int x0, int z0, int x1, int z1, int pisoY) {
        super(ModStructures.CIMIENTO, 0);
        this.boundingBox = new MutableBoundingBox(x0, pisoY - PROFUNDIDAD, z0, x1, pisoY - 1, z1);
    }

    public CimientoPiece(TemplateManager templates, CompoundNBT nbt) {
        super(ModStructures.CIMIENTO, nbt);
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        // la caja (BB) ya la guarda StructurePiece
    }

    @Override
    public boolean postProcess(ISeedReader world, StructureManager structures, ChunkGenerator gen, Random random,
                               MutableBoundingBox chunkBox, ChunkPos chunkPos, BlockPos origin) {
        // tierra, como un terraplén natural (debajo del edificio la plantilla ya trae su cimiento de piedra)
        BlockState piedra = Blocks.DIRT.defaultBlockState();
        BlockState musgo = Blocks.COARSE_DIRT.defaultBlockState();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int x0 = Math.max(boundingBox.x0, chunkBox.x0);
        int x1 = Math.min(boundingBox.x1, chunkBox.x1);
        int z0 = Math.max(boundingBox.z0, chunkBox.z0);
        int z1 = Math.min(boundingBox.z1, chunkBox.z1);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = boundingBox.y1; y >= boundingBox.y0 && y > 0; y--) {
                    pos.set(x, y, z);
                    BlockState actual = world.getBlockState(pos);
                    boolean hueco = actual.isAir() || !actual.getFluidState().isEmpty()
                            || actual.getMaterial().isReplaceable();
                    if (!hueco) {
                        break;
                    }
                    world.setBlock(pos, random.nextInt(5) == 0 ? musgo : piedra, 2);
                }
            }
        }
        return true;
    }
}
