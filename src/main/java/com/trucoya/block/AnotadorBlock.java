package com.trucoya.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;

/**
 * Anotador con Porotos: se coloca sobre la Mesa de Truco y muestra los puntos de cada equipo
 * con porotos agrupados de a 5 (blancos = equipo de quien tocó Repartir, negros = rivales).
 */
public class AnotadorBlock extends Block {

    public static final int MAX = 40;
    public static final IntegerProperty BLANCOS = IntegerProperty.create("blancos", 0, MAX);
    public static final IntegerProperty NEGROS = IntegerProperty.create("negros", 0, MAX);

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.5D, 13.0D);

    public AnotadorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BLANCOS, 0).setValue(NEGROS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BLANCOS, NEGROS);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canSurvive(BlockState state, IWorldReader world, BlockPos pos) {
        BlockPos below = pos.below();
        return world.getBlockState(below).getBlock() instanceof MesaTrucoBlock
                || Block.canSupportCenter(world, below, Direction.UP);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, IWorld world,
                                  BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !this.canSurvive(state, world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, world, pos, neighborPos);
    }

    /** Estado con el puntaje dado (recortado a 0..40). */
    public BlockState withScore(BlockState state, int blancos, int negros) {
        return state.setValue(BLANCOS, clamp(blancos)).setValue(NEGROS, clamp(negros));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(MAX, v));
    }
}
