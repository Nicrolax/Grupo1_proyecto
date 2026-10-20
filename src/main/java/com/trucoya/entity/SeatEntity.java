package com.trucoya.entity;

import com.trucoya.block.SillaCriollaBlock;
import com.trucoya.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

/**
 * Asiento invisible de la Silla Criolla. Se destruye solo cuando el jugador se levanta
 * o cuando la silla deja de existir, así que nunca quedan entidades huérfanas.
 */
public class SeatEntity extends Entity {

    /** Altura de la entidad sobre el bloque para que el jugador quede apoyado en el asiento (10/16). */
    private static final double SEAT_HEIGHT = 0.375D;

    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
    }

    /** Sienta a un jugador o gaucho en la silla de {@code pos}. Devuelve false si está ocupada. */
    public static boolean sit(LivingEntity player, BlockPos pos) {
        World world = player.level;
        List<SeatEntity> seats = world.getEntitiesOfClass(SeatEntity.class, new AxisAlignedBB(pos));
        SeatEntity seat;
        if (!seats.isEmpty()) {
            seat = seats.get(0);
            if (seat.isVehicle()) {
                return false;
            }
        } else {
            seat = new SeatEntity(ModEntities.SEAT.get(), world);
            seat.setPos(pos.getX() + 0.5D, pos.getY() + SEAT_HEIGHT, pos.getZ() + 0.5D);
            world.addFreshEntity(seat);
        }
        return player.startRiding(seat);
    }

    /** Posición de la silla de este asiento. */
    public BlockPos getChairPos() {
        return new BlockPos(getX(), getY() - SEAT_HEIGHT + 0.01D, getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            boolean chairGone = !(this.level.getBlockState(getChairPos()).getBlock() instanceof SillaCriollaBlock);
            if (chairGone || !this.isVehicle()) {
                this.ejectPassengers();
                this.remove();
            }
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
