package com.trucoya.item;

import com.trucoya.entity.GauchoEntity;
import com.trucoya.registry.ModEntities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Huevo de aparición de un gaucho con un rol fijo (paisano, cantinero, forastero, Vizcacha). */
public class GauchoEggItem extends ForgeSpawnEggItem {

    private final GauchoEntity.Rol rol;

    public GauchoEggItem(GauchoEntity.Rol rol, int primary, int secondary, Properties props) {
        super(ModEntities.GAUCHO, primary, secondary, props);
        this.rol = rol;
    }

    private void tag(ItemStack stack) {
        CompoundNBT entityTag = stack.getOrCreateTagElement("EntityTag");
        entityTag.putString("Rol", rol.name());
        entityTag.putString("id", "trucoya:gaucho");
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        tag(context.getItemInHand());
        return super.useOn(context);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        tag(player.getItemInHand(hand));
        return super.use(world, player, hand);
    }
}
