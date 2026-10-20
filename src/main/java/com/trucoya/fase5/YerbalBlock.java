package com.trucoya.fase5;

import com.trucoya.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

/** Yerbal: plantita de yerba mate. Se siembra en tierra arada; madura da hojas para sapecar. */
public class YerbalBlock extends CropsBlock {

    private static final VoxelShape[] FORMAS = new VoxelShape[8];

    static {
        for (int i = 0; i < 8; i++) {
            FORMAS[i] = box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D + i * 1.6D, 14.0D);
        }
    }

    public YerbalBlock(Properties props) {
        super(props);
    }

    @Override
    protected IItemProvider getBaseSeedId() {
        return ModItems.SEMILLAS_YERBA.get();
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        return FORMAS[state.getValue(this.getAgeProperty())];
    }
}
