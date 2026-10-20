package com.trucoya.item;

import com.trucoya.barajas.Baraja;
import com.trucoya.barajas.Coleccion;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Una baraja coleccionable. Clic derecho: se suma a tu colección (si es de las que se pueden regalar o comprar)
 * y pasás a jugar con ella. Las de hazaña (Oro, Mandinga, Fundadores) solo sirven a quien las ganó.
 */
public class BarajaItem extends Item {

    private final Baraja baraja;

    public BarajaItem(Baraja baraja, Properties properties) {
        super(properties);
        this.baraja = baraja;
    }

    public Baraja baraja() {
        return baraja;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) player;
            if (!Coleccion.tiene(p, baraja) && !baraja.seRegala()) {
                p.displayClientMessage(new StringTextComponent("Esta baraja no es tuya. " + baraja.comoSeGana)
                        .withStyle(TextFormatting.GRAY), true);
                return ActionResult.fail(stack);
            }
            if (!Coleccion.tiene(p, baraja)) {
                // un mazo comprado o regalado: pasa a la colección (y a la Libreta); el mazo se usa
                Coleccion.elegir(p, baraja);
                if (!p.abilities.instabuild) {
                    stack.shrink(1);
                }
                world.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8F, 1.3F);
                return ActionResult.consume(stack);
            }
            if (Coleccion.elegida(p) == baraja) {
                p.displayClientMessage(new StringTextComponent("Ya estás jugando con la " + baraja.nombre + ".")
                        .withStyle(TextFormatting.GOLD), true);
            } else {
                Coleccion.elegir(p, baraja);
                world.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8F, 1.3F);
            }
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent(baraja.comoSeGana).withStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
        tooltip.add(new StringTextComponent(baraja.seRegala() ? "Clic derecho: sumarla a tu colección y jugar con ella"
                : "Clic derecho: jugar con esta baraja").withStyle(TextFormatting.DARK_GREEN));
    }
}
