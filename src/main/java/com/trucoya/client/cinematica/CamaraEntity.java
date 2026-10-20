package com.trucoya.client.cinematica;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.Pose;
import com.trucoya.registry.ModEntities;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * La cámara de la cinemática: una entidad que existe solo en el cliente y nunca se agrega al mundo.
 * Minecraft dibuja el mundo "desde sus ojos"; la movemos cuadro a cuadro, sin interpolar entre ticks,
 * así el movimiento es tan fluido como los FPS de la usuaria.
 */
@OnlyIn(Dist.CLIENT)
final class CamaraEntity extends Entity {

    /** La misma altura de ojos que un jugador parado: el cambio de cámara no "salta". */
    static final float OJOS = 1.62F;

    CamaraEntity(World world) {
        super(ModEntities.SEAT.get(), world);   // un tipo propio: ningún otro mod la confunde con algo suyo
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    /** Lleva la cámara a {@code (x, y, z)} (posición del ojo) mirando con {@code yaw} / {@code pitch}. */
    void apuntar(double x, double y, double z, float yaw, float pitch) {
        double pie = y - OJOS;
        this.setPos(x, pie, z);
        this.xo = this.xOld = x;
        this.yo = this.yOld = pie;
        this.zo = this.zOld = z;
        this.yRot = this.yRotO = yaw;
        this.xRot = this.xRotO = pitch;
    }

    @Override
    protected float getEyeHeight(Pose pose, EntitySize size) {
        return OJOS;
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
        throw new UnsupportedOperationException("La cámara de la cinemática no existe en el servidor");
    }
}
