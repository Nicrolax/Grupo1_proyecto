package com.trucoya;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Opciones del servidor (se guardan con cada mundo en serverconfig/trucoya-server.toml). En un servidor el
 * administrador las puede cambiar; en un mundo de un jugador conviene dejarlas como vienen.
 */
public final class TrucoConfig {

    public enum Cielo { SIEMPRE, SOLO, NUNCA }

    public static final ForgeConfigSpec SPEC;
    /** Si la Salamanca puede hacer de noche y traer lluvia (eso cambia el cielo de todo el mundo). */
    public static final ForgeConfigSpec.EnumValue<Cielo> SALAMANCA_CIELO;
    /** Los pedidos de los paisanos. */
    public static final ForgeConfigSpec.BooleanValue PEDIDOS;
    /** Cada cuánto (minutos) un jugador que no es operador puede volver a ver la cinemática con /truco cinematica. */
    public static final ForgeConfigSpec.IntValue ESPERA_CINEMATICA;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("salamanca");
        SALAMANCA_CIELO = b.comment(
                "Cuándo La Salamanca puede cambiar la hora y el clima (es para todo el mundo, no solo para quien la juega).",
                "SIEMPRE: siempre. SOLO: solo si no hay otros jugadores lejos en el mundo de arriba (recomendado).",
                "NUNCA: nunca (la historia se juega igual, con el cielo que haya).")
                .defineEnum("cambiaElCielo", Cielo.SOLO);
        b.pop();
        b.push("pedidos");
        PEDIDOS = b.comment("Los paisanos ofrecen pedidos (traer cosas, llevar encomiendas, desafíos).")
                .define("activados", true);
        b.pop();
        b.push("comandos");
        ESPERA_CINEMATICA = b.comment("Minutos entre una y otra vez que alguien (no operador) usa /truco cinematica.")
                .defineInRange("esperaCinematica", 10, 0, 1440);
        b.pop();
        SPEC = b.build();
    }

    private TrucoConfig() {
    }
}
