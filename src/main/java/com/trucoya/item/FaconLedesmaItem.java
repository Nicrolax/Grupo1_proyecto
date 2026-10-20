package com.trucoya.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** El facón que el Negro Ledesma le ganó al Vizcacha en el Paso de las Ánimas: el premio de la Salamanca. */
public class FaconLedesmaItem extends SwordItem {

    public FaconLedesmaItem(IItemTier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.trucoya.facon_ledesma.pista")
                .withStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
    }
}
