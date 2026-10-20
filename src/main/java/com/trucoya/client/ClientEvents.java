package com.trucoya.client;

import com.trucoya.TrucoYa;
import com.trucoya.network.TableActionPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del juego en el cliente: teclas G/J y globos de diálogo sobre los jugadores. */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // Brillo de la Baraja de Oro en la mano.
        if (mc.level != null && mc.player.tickCount % 3 == 0
                && (mc.player.getMainHandItem().getItem() == ModItems.BARAJA_DE_ORO.get()
                || mc.player.getOffhandItem().getItem() == ModItems.BARAJA_DE_ORO.get())) {
            double a = mc.player.tickCount * 0.3D;
            mc.level.addParticle(ParticleTypes.END_ROD, mc.player.getX() + Math.cos(a) * 0.8D,
                    mc.player.getY() + 1.0D + Math.sin(a * 0.7D) * 0.3D, mc.player.getZ() + Math.sin(a) * 0.8D, 0, 0.01D, 0);
            mc.level.addParticle(ParticleTypes.ENCHANT, mc.player.getX(), mc.player.getY() + 1.6D, mc.player.getZ(),
                    (mc.level.random.nextDouble() - 0.5D) * 2, -0.5D, (mc.level.random.nextDouble() - 0.5D) * 2);
        }
        while (ModKeys.SENAS.consumeClick()) {
            if (mc.screen == null && ClientTrucoState.tablePos != null && ClientTrucoState.snapshot != null
                    && ClientTrucoState.snapshot.view != null) {
                mc.setScreen(new SenasScreen(null));
            }
        }
        while (ModKeys.TAPETE.consumeClick()) {
            if (mc.screen == null && ClientTrucoState.tablePos != null) {
                TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.open(ClientTrucoState.tablePos));
            }
        }
    }

    @SubscribeEvent
    public static void onNameplate(RenderNameplateEvent event) {
        ClientTrucoState.Bubble bubble = ClientTrucoState.bubbleFor(event.getEntity().getId());
        if (bubble != null) {
            event.setContent(new StringTextComponent("« " + bubble.text + " »").withStyle(TextFormatting.YELLOW));
            event.setResult(Event.Result.ALLOW);
        }
    }

    private ClientEvents() {
    }
}
