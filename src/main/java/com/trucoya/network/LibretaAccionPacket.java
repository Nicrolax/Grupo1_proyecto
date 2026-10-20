package com.trucoya.network;

import com.trucoya.barajas.Baraja;
import com.trucoya.barajas.Coleccion;
import com.trucoya.progreso.Historia;
import com.trucoya.progreso.Pedidos;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente → servidor: algo que se hizo en la Libreta (elegir baraja, cancelar un pedido). Todo se valida acá. */
public class LibretaAccionPacket {

    public static final int ELEGIR_BARAJA = 0;
    public static final int CANCELAR_PEDIDO = 1;

    public final int accion;
    public final int valor;

    public LibretaAccionPacket(int accion, int valor) {
        this.accion = accion;
        this.valor = valor;
    }

    public static void encode(LibretaAccionPacket m, PacketBuffer b) {
        b.writeVarInt(m.accion);
        b.writeVarInt(m.valor);
    }

    public static LibretaAccionPacket decode(PacketBuffer b) {
        return new LibretaAccionPacket(b.readVarInt(), b.readVarInt());
    }

    public static void handle(LibretaAccionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity p = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (p == null) {
                return;
            }
            if (m.accion == ELEGIR_BARAJA && m.valor >= 0 && m.valor < Baraja.values().length) {
                Baraja b = Baraja.de(m.valor);
                if (Coleccion.tiene(p, b)) {
                    Coleccion.elegir(p, b);
                }
            } else if (m.accion == CANCELAR_PEDIDO) {
                Pedidos.cancelarId(p, m.valor);   // por Id: la lista pudo cambiar mientras la Libreta estaba abierta
            } else {
                return;
            }
            Historia.abrirLibreta(p);
        });
        ctx.get().setPacketHandled(true);
    }
}
