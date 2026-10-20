package com.trucoya.item;

import com.trucoya.progreso.Historia;
import com.trucoya.registry.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/** La encomienda de un pedido LLEVAR: un paquete atado con tiento, para un paisano de otra región. */
public class EncomiendaItem extends Item {

    public EncomiendaItem(Properties properties) {
        super(properties);
    }

    public static ItemStack crear(int pedido, int region, String de) {
        ItemStack st = new ItemStack(ModItems.ENCOMIENDA.get());
        CompoundNBT t = st.getOrCreateTag();
        t.putInt("Pedido", pedido);
        t.putInt("Region", region);
        t.putString("De", de);
        return st;
    }

    /** El casillero donde el jugador tiene la encomienda de ese pedido, o -1. */
    public static int buscar(PlayerEntity p, int pedido) {
        for (int s = 0; s < p.inventory.getContainerSize(); s++) {
            ItemStack st = p.inventory.getItem(s);
            if (st.getItem() == ModItems.ENCOMIENDA.get() && st.hasTag() && st.getTag().getInt("Pedido") == pedido) {
                return s;
            }
        }
        return -1;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        if (!stack.hasTag()) {
            return;
        }
        Historia.Region[] rs = Historia.Region.values();
        Historia.Region r = rs[Math.floorMod(stack.getTag().getInt("Region"), rs.length)];
        tooltip.add(new StringTextComponent("De " + stack.getTag().getString("De") + ", para un paisano de " + r.nombre)
                .withStyle(TextFormatting.GRAY));
    }
}
