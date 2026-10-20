package com.trucoya.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

/**
 * Rebenque: pega poco pero empuja lejos. Montado a caballo, clic derecho para apurar al
 * caballo (Velocidad II por 10 segundos; después hay que esperar 15 segundos).
 */
public class RebenqueItem extends SwordItem {

    public RebenqueItem(Properties props) {
        super(ArmasCriollas.CUERO, 1, -2.0F, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float yaw = attacker.yRot * ((float) Math.PI / 180F);
        target.knockback(1.2F, MathHelper.sin(yaw), -MathHelper.cos(yaw));
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getVehicle() instanceof AbstractHorseEntity) {
            if (!world.isClientSide) {
                AbstractHorseEntity horse = (AbstractHorseEntity) player.getVehicle();
                horse.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 200, 1, false, true));
                world.playSound(null, horse.getX(), horse.getY(), horse.getZ(), SoundEvents.HORSE_GALLOP,
                        SoundCategory.PLAYERS, 1.0F, 1.2F);
                player.displayClientMessage(new StringTextComponent("¡Arre, pingo!"), true);
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            player.getCooldowns().addCooldown(this, 300);
            player.swing(hand);
            return ActionResult.sidedSuccess(stack, world.isClientSide);
        }
        return ActionResult.pass(stack);
    }
}
