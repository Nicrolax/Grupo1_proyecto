package com.trucoya.item;

import com.trucoya.entity.BoleadorasEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

/** Boleadoras: se revolean y se tiran; enredan las patas de lo que alcanzan. Se pueden juntar de nuevo. */
public class BoleadorasItem extends Item {

    public BoleadorasItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW,
                SoundCategory.PLAYERS, 0.6F, 0.6F + world.random.nextFloat() * 0.2F);
        if (!world.isClientSide) {
            BoleadorasEntity b = new BoleadorasEntity(world, player);
            ItemStack lanzada = stack.copy();
            lanzada.setCount(1);
            b.setItem(lanzada);
            b.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 1.6F, 1.0F);
            world.addFreshEntity(b);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.abilities.instabuild) {
            stack.shrink(1);
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }
}
