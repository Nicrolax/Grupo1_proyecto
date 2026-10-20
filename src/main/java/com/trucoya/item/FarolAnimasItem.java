package com.trucoya.item;

import com.trucoya.progreso.Salamanca;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * El Farol de las Ánimas, que da el Viejo Vizcacha. De noche, en la mano, sus luces malas marcan para dónde
 * queda el Paso de las Ánimas (lo resuelve {@link Salamanca#farol}).
 */
public class FarolAnimasItem extends Item {

    public FarolAnimasItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide && entity instanceof ServerPlayerEntity && entity.tickCount % 20 == 0) {
            ServerPlayerEntity p = (ServerPlayerEntity) entity;
            if (selected || p.getOffhandItem() == stack) {
                Salamanca.farol(p);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.trucoya.farol_animas.pista").withStyle(TextFormatting.GRAY));
    }
}
