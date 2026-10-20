package com.trucoya.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Baraja de Oro: trofeo por vencer al Viejo Vizcacha. Indestructible (no se quema, no
 * desaparece del suelo, no recibe daño) y con brillo y partículas doradas al tenerla en la mano.
 */
public class BarajaDeOroItem extends BarajaItem {

    public BarajaDeOroItem(Properties properties) {
        super(com.trucoya.barajas.Baraja.ORO, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        ItemEntity entity = new ItemEntity(world, location.getX(), location.getY(), location.getZ(), stack.copy());
        entity.setDeltaMovement(location.getDeltaMovement());
        entity.setPickUpDelay(40);
        entity.setExtendedLifetime();
        entity.setInvulnerable(true);
        return entity;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Trofeo de quien le ganó al Viejo Vizcacha").withStyle(TextFormatting.GOLD));
        tooltip.add(new StringTextComponent("Indestructible").withStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
        super.appendHoverText(stack, world, tooltip, flag);
    }
}
