package com.trucoya.entity;

import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * Boleadoras en vuelo. Al pegarle a un bicho o a un jugador: 3 de daño y queda "enredado"
 * (casi no se puede mover ni saltar por 4 segundos). Después caen al piso y se pueden juntar.
 */
public class BoleadorasEntity extends ProjectileItemEntity {

    public BoleadorasEntity(EntityType<? extends BoleadorasEntity> type, World world) {
        super(type, world);
    }

    public BoleadorasEntity(World world, LivingEntity thrower) {
        super(ModEntities.BOLEADORAS.get(), thrower, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOLEADORAS.get();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult result) {
        super.onHitEntity(result);
        Entity e = result.getEntity();
        Entity owner = this.getOwner();
        e.hurt(DamageSource.thrown(this, owner), 3.0F);
        if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) e;
            le.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 80, 5));
            le.addEffect(new EffectInstance(Effects.JUMP, 80, 200));   // no puede saltar
            if (le instanceof PlayerEntity) {
                ((PlayerEntity) le).displayClientMessage(
                        new net.minecraft.util.text.StringTextComponent("¡Te enredaron las patas!"), true);
            }
        }
    }

    @Override
    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LEASH_KNOT_PLACE,
                    SoundCategory.PLAYERS, 1.0F, 0.8F);
            ItemStack stack = this.getItem().isEmpty() ? new ItemStack(ModItems.BOLEADORAS.get()) : this.getItem().copy();
            ItemEntity caida = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), stack);
            caida.setDefaultPickUpDelay();
            this.level.addFreshEntity(caida);
            this.remove();
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
