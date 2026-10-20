package com.trucoya.network;

import com.trucoya.TrucoYa;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

/** Canal de red del mod. Cliente y servidor deben tener la misma versión de protocolo. */
public final class TrucoNetwork {

    private static final String PROTOCOL = "7";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TrucoYa.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, TableActionPacket.class, TableActionPacket::encode, TableActionPacket::decode,
                TableActionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, TableViewPacket.class, TableViewPacket::encode, TableViewPacket::decode,
                TableViewPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, BubblePacket.class, BubblePacket::encode, BubblePacket::decode,
                BubblePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SenaNoticePacket.class, SenaNoticePacket::encode, SenaNoticePacket::decode,
                SenaNoticePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, CinematicaPacket.class, CinematicaPacket::encode, CinematicaPacket::decode,
                CinematicaPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, LibretaPacket.class, LibretaPacket::encode, LibretaPacket::decode,
                LibretaPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, CinematicaFinPacket.class, CinematicaFinPacket::encode, CinematicaFinPacket::decode,
                CinematicaFinPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, EscenaPacket.class, EscenaPacket::encode, EscenaPacket::decode,
                EscenaPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, TrailerPacket.class, TrailerPacket::encode, TrailerPacket::decode,
                TrailerPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, LibretaAccionPacket.class, LibretaAccionPacket::encode, LibretaAccionPacket::decode,
                LibretaAccionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private TrucoNetwork() {
    }
}
