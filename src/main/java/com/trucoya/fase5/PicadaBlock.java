package com.trucoya.fase5;

import com.trucoya.block.MesaTrucoBlock;
import com.trucoya.progreso.Logros;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Tabla de picada: se pone sobre la mesa (o cualquier superficie) y se come de a porciones con clic
 * derecho, como una torta: 4 porciones. Quien se come la última gana "Picada Completa".
 * Cada porción da un ratito de Confraternidad: la picada se comparte.
 */
public class PicadaBlock extends Block {

    public static final IntegerProperty COMIDAS = IntegerProperty.create("comidas", 0, 3);
    private static final VoxelShape FORMA = Block.box(1.0D, 0.0D, 2.0D, 15.0D, 3.0D, 14.0D);

    public PicadaBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(COMIDAS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> b) {
        b.add(COMIDAS);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        return FORMA;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext ctx) {
        return this.defaultBlockState();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                BlockRayTraceResult hit) {
        if (!player.canEat(false)) {
            if (!world.isClientSide) {
                player.displayClientMessage(new StringTextComponent("Estás lleno. Dejale algo a los demás."), true);
            }
            return ActionResultType.sidedSuccess(world.isClientSide);
        }
        world.playSound(player, pos, SoundEvents.GENERIC_EAT, SoundCategory.PLAYERS, 1.0F,
                0.9F + world.random.nextFloat() * 0.2F);
        if (!world.isClientSide) {
            player.getFoodData().eat(4, 0.7F);
            Fase5.confraternidad(player, 20 * 20, 20 * 60 * 6);
            int c = state.getValue(COMIDAS);
            if (c >= 3) {
                world.removeBlock(pos, false);
                if (player instanceof ServerPlayerEntity) {
                    Logros.award((ServerPlayerEntity) player, Logros.PICADA_COMPLETA);
                }
                player.displayClientMessage(new StringTextComponent("¡Se terminó la picada!"), true);
            } else {
                world.setBlock(pos, state.setValue(COMIDAS, c + 1), 3);
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
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
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, IWorld world, BlockPos pos,
                                  BlockPos neighborPos) {
        return dir == Direction.DOWN && !this.canSurvive(state, world, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, world, pos, neighborPos);
    }
}
