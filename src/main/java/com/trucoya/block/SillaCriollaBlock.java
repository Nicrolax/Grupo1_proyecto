package com.trucoya.block;

import com.trucoya.entity.SeatEntity;
import com.trucoya.tileentity.MesaTrucoTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Silla Criolla: clic derecho para sentarse. Se orienta sola hacia una Mesa de Truco vecina
 * y, al sentarse, vincula al jugador con esa mesa.
 */
public class SillaCriollaBlock extends HorizontalBlock {

    private static final VoxelShape SEAT = Block.box(1.5D, 0.0D, 1.5D, 14.5D, 10.0D, 14.5D);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // El respaldo queda del lado opuesto a hacia donde mira la silla.
        SHAPES.put(Direction.NORTH, VoxelShapes.or(SEAT, Block.box(2.0D, 10.0D, 12.0D, 14.0D, 22.0D, 14.0D)));
        SHAPES.put(Direction.SOUTH, VoxelShapes.or(SEAT, Block.box(2.0D, 10.0D, 2.0D, 14.0D, 22.0D, 4.0D)));
        SHAPES.put(Direction.EAST, VoxelShapes.or(SEAT, Block.box(2.0D, 10.0D, 2.0D, 4.0D, 22.0D, 14.0D)));
        SHAPES.put(Direction.WEST, VoxelShapes.or(SEAT, Block.box(12.0D, 10.0D, 2.0D, 14.0D, 22.0D, 14.0D)));
    }

    public SillaCriollaBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction looking = context.getHorizontalDirection();
        // Preferir mirar hacia una mesa vecina (primero la que está adelante del jugador).
        if (world.getBlockState(pos.relative(looking)).getBlock() instanceof MesaTrucoBlock) {
            return this.defaultBlockState().setValue(FACING, looking);
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (world.getBlockState(pos.relative(d)).getBlock() instanceof MesaTrucoBlock) {
                return this.defaultBlockState().setValue(FACING, d);
            }
        }
        return this.defaultBlockState().setValue(FACING, looking);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (player.isPassenger() || player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }
        if (!SeatEntity.sit(player, pos)) {
            player.displayClientMessage(new StringTextComponent("La silla está ocupada."), true);
            return ActionResultType.CONSUME;
        }
        if (player instanceof ServerPlayerEntity) {
            com.trucoya.progreso.Logros.sentarse((ServerPlayerEntity) player);
        }
        MesaTrucoTileEntity table = findTable(world, pos, state);
        if (table != null && player instanceof ServerPlayerEntity) {
            table.onPlayerSat((ServerPlayerEntity) player);
        }
        return ActionResultType.CONSUME;
    }

    /** Mesa vinculada a la silla: la que tiene enfrente o, si no, cualquier vecina. */
    @Nullable
    public static MesaTrucoTileEntity findTable(World world, BlockPos chairPos, BlockState chairState) {
        Direction facing = chairState.getBlock() instanceof SillaCriollaBlock
                ? chairState.getValue(FACING) : Direction.NORTH;
        TileEntity te = world.getBlockEntity(chairPos.relative(facing));
        if (te instanceof MesaTrucoTileEntity) {
            return (MesaTrucoTileEntity) te;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            te = world.getBlockEntity(chairPos.relative(d));
            if (te instanceof MesaTrucoTileEntity) {
                return (MesaTrucoTileEntity) te;
            }
        }
        return null;
    }
}
