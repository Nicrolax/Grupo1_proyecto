package com.trucoya.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → clientes cercanos: globo de texto sobre la cabeza de un jugador que cantó. */
public class BubblePacket {

    /** Máximo de caracteres del globo. */
    private static final int MAX_LEN = 200;
    /** Máximo en bytes al codificar: los acentos y la ñ ocupan más de un byte (UTF-8). */
    private static final int MAX_BYTES = 1024;

    public final int entityId;
    public final String text;

    public BubblePacket(int entityId, String text) {
        this.entityId = entityId;
        this.text = text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text;
    }

    public static void encode(BubblePacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.text, MAX_BYTES);
    }

    public static BubblePacket decode(PacketBuffer buf) {
        return new BubblePacket(buf.readVarInt(), buf.readUtf(MAX_BYTES));
    }

    public static void handle(BubblePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleBubble(msg.entityId, msg.text)));
        ctx.get().setPacketHandled(true);
    }
}
