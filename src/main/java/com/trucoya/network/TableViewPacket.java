package com.trucoya.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: estado de la mesa para ese jugador ({@code TableSnapshot} serializado). */
public class TableViewPacket {

    private static final int MAX_BYTES = 16384;

    public final BlockPos pos;
    public final byte[] data;

    public TableViewPacket(BlockPos pos, byte[] data) {
        this.pos = pos;
        this.data = data;
    }

    public static void encode(TableViewPacket msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeByteArray(msg.data);
    }

    public static TableViewPacket decode(PacketBuffer buf) {
        return new TableViewPacket(buf.readBlockPos(), buf.readByteArray(MAX_BYTES));
    }

    public static void handle(TableViewPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleView(msg.pos, msg.data)));
        ctx.get().setPacketHandled(true);
    }
}
