package com.trucoya.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: aviso discreto de una seña (del compañero, o descubierta a un rival). */
public class SenaNoticePacket {

    private static final int MAX_LEN = 160;
    /** Máximo en bytes al codificar: los acentos y la ñ ocupan más de un byte (UTF-8). */
    private static final int MAX_BYTES = 1024;

    public final String text;
    public final boolean fromPartner;

    public SenaNoticePacket(String text, boolean fromPartner) {
        this.text = text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text;
        this.fromPartner = fromPartner;
    }

    public static void encode(SenaNoticePacket msg, PacketBuffer buf) {
        buf.writeUtf(msg.text, MAX_BYTES);
        buf.writeBoolean(msg.fromPartner);
    }

    public static SenaNoticePacket decode(PacketBuffer buf) {
        return new SenaNoticePacket(buf.readUtf(MAX_BYTES), buf.readBoolean());
    }

    public static void handle(SenaNoticePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleSena(msg.text, msg.fromPartner)));
        ctx.get().setPacketHandled(true);
    }
}
