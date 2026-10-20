package com.trucoya.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor → cliente: empieza una escena de la Salamanca (media cinemática en el mundo de verdad).
 * {@code tipo}: ver {@link com.trucoya.progreso.Escenas}. {@code entidad}: el Vizcacha que guía (o -1).
 * {@code origen}: la esquina del Paso de las Ánimas (o el lugar del jugador en la visita).
 */
public class EscenaPacket {

    public final int tipo;
    public final int entidad;
    public final BlockPos origen;

    public EscenaPacket(int tipo, int entidad, BlockPos origen) {
        this.tipo = tipo;
        this.entidad = entidad;
        this.origen = origen;
    }

    public static void encode(EscenaPacket msg, PacketBuffer buf) {
        buf.writeByte(msg.tipo);
        buf.writeInt(msg.entidad);
        buf.writeBlockPos(msg.origen);
    }

    public static EscenaPacket decode(PacketBuffer buf) {
        return new EscenaPacket(buf.readByte(), buf.readInt(), buf.readBlockPos());
    }

    public static void handle(EscenaPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleEscena(msg.tipo, msg.entidad, msg.origen)));
        ctx.get().setPacketHandled(true);
    }
}
