package com.trucoya.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: el avance de la historia, para abrir la Libreta del Truquero. */
public class LibretaPacket {

    public final int sellos;
    public final int prestigio;
    public final int ganados;
    public final boolean mapa;
    public final int dx;
    public final int dz;
    public final boolean vizcacha;
    public final boolean finalVisto;
    public final int torneos;
    public final int cuadreras;
    /** La Salamanca: etapa (ver progreso.Salamanca) y hacia dónde queda el Paso de las Ánimas. */
    public final int salamanca;
    public final int pasoDx;
    public final int pasoDz;
    /** Barajas: las que tiene (bits de Baraja) y con cuál juega. */
    public int barajas;
    public int elegida;
    /** Pedidos: "título\ndetalle\npremio" de cada uno. */
    public java.util.List<String> pedidos = new java.util.ArrayList<>();
    public int[] pedidoIds = new int[0];
    /** Desafíos del día: "texto\navance\nmeta" de cada uno, y la racha de días seguidos. */
    public java.util.List<String> diario = new java.util.ArrayList<>();
    public int diarioRacha;
    /** Los números del truquero (ver Historia.NUMEROS). */
    public int[] numeros = new int[0];

    public LibretaPacket(int sellos, int prestigio, int ganados, boolean mapa, int dx, int dz, boolean vizcacha,
                         boolean finalVisto, int torneos, int cuadreras, int salamanca, int pasoDx, int pasoDz) {
        this.sellos = sellos;
        this.prestigio = prestigio;
        this.ganados = ganados;
        this.mapa = mapa;
        this.dx = dx;
        this.dz = dz;
        this.vizcacha = vizcacha;
        this.finalVisto = finalVisto;
        this.torneos = torneos;
        this.cuadreras = cuadreras;
        this.salamanca = salamanca;
        this.pasoDx = pasoDx;
        this.pasoDz = pasoDz;
    }

    public static void encode(LibretaPacket m, PacketBuffer b) {
        b.writeVarInt(m.sellos);
        b.writeVarInt(m.prestigio);
        b.writeVarInt(m.ganados);
        b.writeBoolean(m.mapa);
        b.writeInt(m.dx);
        b.writeInt(m.dz);
        b.writeBoolean(m.vizcacha);
        b.writeBoolean(m.finalVisto);
        b.writeVarInt(m.torneos);
        b.writeVarInt(m.cuadreras);
        b.writeVarInt(m.salamanca);
        b.writeInt(m.pasoDx);
        b.writeInt(m.pasoDz);
        b.writeVarInt(m.barajas);
        b.writeVarInt(m.elegida);
        b.writeVarInt(m.pedidos.size());
        for (String s : m.pedidos) b.writeUtf(s, 1024);
        b.writeVarIntArray(m.pedidoIds);
        b.writeVarInt(m.diario.size());
        for (String s : m.diario) b.writeUtf(s, 512);
        b.writeVarInt(m.diarioRacha);
        b.writeVarIntArray(m.numeros);
    }

    public static LibretaPacket decode(PacketBuffer b) {
        LibretaPacket m = new LibretaPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readInt(), b.readInt(),
                b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readInt(), b.readInt());
        m.barajas = b.readVarInt();
        m.elegida = b.readVarInt();
        int n = Math.min(b.readVarInt(), 8);
        for (int i = 0; i < n; i++) m.pedidos.add(b.readUtf(1024));
        m.pedidoIds = b.readVarIntArray(8);
        int nd = Math.min(b.readVarInt(), 6);
        for (int i = 0; i < nd; i++) m.diario.add(b.readUtf(512));
        m.diarioRacha = b.readVarInt();
        m.numeros = b.readVarIntArray(32);
        return m;
    }

    public static void handle(LibretaPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.trucoya.client.ClientPacketHandlers.handleLibreta(m)));
        ctx.get().setPacketHandled(true);
    }
}
