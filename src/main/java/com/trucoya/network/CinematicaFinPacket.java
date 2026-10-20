package com.trucoya.network;

import com.trucoya.progreso.CinematicaServidor;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente → servidor: la cinemática terminó (o se salteó). */
public class CinematicaFinPacket {

    public static void encode(CinematicaFinPacket msg, PacketBuffer buf) {
    }

    public static CinematicaFinPacket decode(PacketBuffer buf) {
        return new CinematicaFinPacket();
    }

    public static void handle(CinematicaFinPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity p = ctx.get().getSender();
        if (p != null) {
            ctx.get().enqueueWork(() -> {
                CinematicaServidor.terminar(p);
                com.trucoya.trailer.TrailerServidor.alTerminarCliente(p);
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
