package com.trucoya.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: empieza la cinemática del Viejo Vizcacha (id de la entidad, o -1 si no hay). */
public class CinematicaPacket {

    public final int vizcachaId;
    /** 0: el encuentro con el Vizcacha · 1: la cinemática final con los Fundadores. */
    public final int tipo;

    public CinematicaPacket(int vizcachaId) {
        this(vizcachaId, 0);
    }

    public CinematicaPacket(int vizcachaId, int tipo) {
        this.vizcachaId = vizcachaId;
        this.tipo = tipo;
    }

    public static void encode(CinematicaPacket msg, PacketBuffer buf) {
        buf.writeInt(msg.vizcachaId);
        buf.writeByte(msg.tipo);
    }

    public static CinematicaPacket decode(PacketBuffer buf) {
        return new CinematicaPacket(buf.readInt(), buf.readByte());
    }

    public static void handle(CinematicaPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleCinematica(msg.vizcachaId, msg.tipo)));
        ctx.get().setPacketHandled(true);
    }
}
