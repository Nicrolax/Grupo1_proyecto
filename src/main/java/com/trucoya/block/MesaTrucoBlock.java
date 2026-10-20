package com.trucoya.block;

import com.trucoya.entity.SeatEntity;
import com.trucoya.registry.ModTileEntities;
import com.trucoya.tileentity.MesaTrucoTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/**
 * Mesa de Truco: aloja la partida (1v1 o 2v2) en su TileEntity. Las sillas se ubican en los
 * cuatro lados; los compañeros quedan enfrentados. Clic derecho sentado abre el tapete.
 */
public class MesaTrucoBlock extends Block {

    public MesaTrucoBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntities.MESA_TRUCO.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        // Con un bloque en la mano (por ejemplo el Anotador) se deja colocar arriba de la mesa
        // en vez de abrir el tapete; sentado en la silla, el clic sigue abriendo el tapete.
        boolean sentado = player.getVehicle() instanceof SeatEntity;
        if (!sentado && player.getItemInHand(hand).getItem() instanceof BlockItem) {
            return ActionResultType.PASS;
        }
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        TileEntity te = world.getBlockEntity(pos);
        if (!(te instanceof MesaTrucoTileEntity) || !(player instanceof ServerPlayerEntity)) {
            return ActionResultType.PASS;
        }
        MesaTrucoTileEntity table = (MesaTrucoTileEntity) te;
        if (player.getVehicle() instanceof SeatEntity && table.isSeatedHere((ServerPlayerEntity) player)) {
            table.openFor((ServerPlayerEntity) player);
        } else {
            player.displayClientMessage(new StringTextComponent(
                    "Sentate en una Silla Criolla junto a la mesa para jugar."), true);
        }
        return ActionResultType.CONSUME;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof MesaTrucoTileEntity) {
                ((MesaTrucoTileEntity) te).abort("Se levantó la mesa.");
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
