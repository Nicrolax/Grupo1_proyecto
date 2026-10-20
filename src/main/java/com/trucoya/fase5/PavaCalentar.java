package com.trucoya.fase5;

import com.trucoya.TrucoYa;
import net.minecraft.util.ActionResultType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Clic derecho con la pava de agua fría sobre un horno o una fogata prendidos: se calienta, sin abrir el horno
 * (antes el horno se abría y la pava nunca llegaba a tocar el fuego).
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PavaCalentar {

    private PavaCalentar() {
    }

    @SubscribeEvent
    public static void onClic(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof PavaItem
                && PavaItem.calentar(event.getWorld(), event.getPos(), event.getItemStack(), event.getPlayer())) {
            event.setCanceled(true);
            event.setCancellationResult(ActionResultType.sidedSuccess(event.getWorld().isClientSide));
        }
    }
}
