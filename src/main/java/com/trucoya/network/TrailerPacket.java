package com.trucoya.network;

import com.trucoya.trailer.Camino;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Servidor → cliente: empezar el recorrido del modo tráiler (o cortarlo, si viene vacío). */
public class TrailerPacket {

    @Nullable
    public final Camino camino;

    public TrailerPacket(@Nullable Camino camino) {
        this.camino = camino;
    }

    public static void encode(TrailerPacket m, PacketBuffer b) {
        b.writeBoolean(m.camino != null);
        if (m.camino != null) m.camino.escribir(b);
    }

    public static TrailerPacket decode(PacketBuffer b) {
        return new TrailerPacket(b.readBoolean() ? Camino.leer(b) : null);
    }

    public static void handle(TrailerPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleTrailer(m.camino)));
        ctx.get().setPacketHandled(true);
    }
}
