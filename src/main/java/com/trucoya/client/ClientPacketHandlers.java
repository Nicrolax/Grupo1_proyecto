package com.trucoya.client;

import com.trucoya.TrucoYa;
import com.trucoya.core.TableSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;

/** Manejo de paquetes en el cliente (esta clase solo se carga del lado cliente). */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {

    public static void handleView(BlockPos pos, byte[] data) {
        TableSnapshot snap;
        try {
            snap = TableSnapshot.fromBytes(data);
        } catch (IOException | RuntimeException e) {
            TrucoYa.LOGGER.warn("Truco Ya: vista de mesa inválida", e);
            return;
        }
        ClientTrucoState.tablePos = pos;
        ClientTrucoState.snapshot = snap;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TapeteScreen) {
            ((TapeteScreen) mc.screen).refresh();
        } else if (snap.open && (mc.screen == null || mc.screen instanceof SenasScreen)) {
            mc.setScreen(new TapeteScreen());
        }
    }

    public static void handleLibreta(com.trucoya.network.LibretaPacket m) {
        Minecraft.getInstance().setScreen(new LibretaScreen(m));
    }

    public static void handleTrailer(@javax.annotation.Nullable com.trucoya.trailer.Camino camino) {
        com.trucoya.client.cinematica.Trailer.iniciar(camino);
    }

    public static void handleCinematica(int vizcachaId, int tipo) {
        com.trucoya.client.cinematica.Cinematica.iniciar(vizcachaId, tipo == 1);
    }

    public static void handleEscena(int tipo, int entidad, net.minecraft.util.math.BlockPos origen) {
        com.trucoya.client.cinematica.EscenaViva.iniciar(tipo, entidad, origen);
    }

    public static void handleBubble(int entityId, String text) {
        ClientTrucoState.addBubble(entityId, text);
    }

    public static void handleSena(String text, boolean fromPartner) {
        ClientTrucoState.setSena(text, fromPartner);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !(mc.screen instanceof TapeteScreen)) {
            mc.player.displayClientMessage(new StringTextComponent(text)
                    .withStyle(fromPartner ? TextFormatting.GRAY : TextFormatting.DARK_GRAY, TextFormatting.ITALIC), true);
        }
    }

    private ClientPacketHandlers() {
    }
}
