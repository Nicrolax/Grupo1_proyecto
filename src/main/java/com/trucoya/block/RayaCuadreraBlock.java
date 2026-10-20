package com.trucoya.block;

import com.trucoya.progreso.Cuadreras;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/**
 * Banderín de cuadrera: marca la raya de largada. Montado a caballo, un clic largá una carrera
 * en línea recta (hacia donde mira el banderín) contra dos jinetes del pago. Si tenés esmeraldas en
 * la mano, las apostás: ganando cobrás el doble.
 */
public class RayaCuadreraBlock extends HorizontalBlock {

    private static final VoxelShape SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    public RayaCuadreraBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.util.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext ctx) {
        // la carrera va hacia donde miraba quien lo puso
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        return SHAPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                BlockRayTraceResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResultType.PASS;
        }
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            if (!(player.getVehicle() instanceof AbstractHorseEntity)) {
                player.displayClientMessage(new StringTextComponent(
                        "Montá tu caballo y tocá el banderín para largar una cuadrera."), true);
            } else {
                Cuadreras.largar((ServerPlayerEntity) player, pos, state.getValue(FACING));
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }
}
