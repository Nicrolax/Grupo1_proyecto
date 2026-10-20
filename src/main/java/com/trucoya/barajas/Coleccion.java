package com.trucoya.barajas;

import com.trucoya.progreso.JugadorData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

/** Las barajas que tiene cada jugador (en sus datos persistentes) y con cuál juega. */
public final class Coleccion {

    public static final String TIENE = "barajas";
    public static final String ELEGIDA = "baraja_elegida";

    private Coleccion() {
    }

    public static int bits(PlayerEntity p) {
        return JugadorData.get(p, TIENE) | Baraja.CRIOLLA.bit();
    }

    public static boolean tiene(PlayerEntity p, Baraja b) {
        return (bits(p) & b.bit()) != 0;
    }

    public static int cantidad(PlayerEntity p) {
        return Integer.bitCount(bits(p));
    }

    /** La baraja con la que juega (si dejó de tenerla, la criolla). */
    public static Baraja elegida(PlayerEntity p) {
        Baraja b = Baraja.de(JugadorData.get(p, ELEGIDA));
        return tiene(p, b) ? b : Baraja.CRIOLLA;
    }

    /** Suma una baraja a la colección. Devuelve true si es nueva. */
    public static boolean sumar(ServerPlayerEntity p, Baraja b) {
        if (tiene(p, b)) {
            return false;
        }
        JugadorData.set(p, TIENE, bits(p) | b.bit());
        if (cantidad(p) >= 6) {
            com.trucoya.progreso.Logros.award(p, com.trucoya.progreso.Logros.COLECCIONISTA);
        }
        p.sendMessage(new StringTextComponent("✦ Nueva baraja para tu colección: " + b.nombre + " ✦ ("
                + cantidad(p) + " de " + Baraja.values().length + ")").withStyle(TextFormatting.GOLD), p.getUUID());
        p.level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    /** La gana por una hazaña: entra a la colección y, si es nueva, le llega el mazo en la mano. */
    public static void ganar(ServerPlayerEntity p, Baraja b) {
        if (sumar(p, b) && b != Baraja.ORO) {   // la de Oro ya la entrega el Vizcacha
            net.minecraft.item.ItemStack mazo = new net.minecraft.item.ItemStack(com.trucoya.registry.ModItems.baraja(b));
            if (!p.inventory.add(mazo)) {
                p.drop(mazo, false);
            }
            p.sendMessage(new StringTextComponent("Elegila en la Libreta (pestaña Barajas) o con clic derecho en el mazo.")
                    .withStyle(TextFormatting.GRAY), p.getUUID());
        }
    }

    /** Para partidas empezadas antes de que existieran las barajas: lo ya hecho cuenta. */
    public static void ponerAlDia(ServerPlayerEntity p) {
        int antes = bits(p);
        int b = antes;
        if (com.trucoya.progreso.JugadorData.get(p, com.trucoya.progreso.Historia.VIZCACHA_VENCIDO) != 0) b |= Baraja.ORO.bit();
        if (com.trucoya.progreso.JugadorData.get(p, com.trucoya.progreso.Salamanca.ETAPA) == com.trucoya.progreso.Salamanca.LIBRE) {
            b |= Baraja.MANDINGA.bit();
        }
        if (com.trucoya.progreso.JugadorData.get(p, com.trucoya.progreso.Historia.FINAL_VISTO) != 0) b |= Baraja.FUNDADORES.bit();
        for (com.trucoya.progreso.Historia.Region r : com.trucoya.progreso.Historia.Region.values()) {
            if (com.trucoya.progreso.Historia.tieneSello(p, r)) b |= Baraja.deRegion(r).bit();
        }
        // lo ganado antes de 1.6.0 (o en una partida que se cortó) también llega en mano
        for (Baraja x : Baraja.values()) {
            if ((b & x.bit()) != 0 && (antes & x.bit()) == 0) {
                ganar(p, x);
            }
        }
    }

    public static void elegir(ServerPlayerEntity p, Baraja b) {
        sumar(p, b);
        JugadorData.set(p, ELEGIDA, b.ordinal());
        p.displayClientMessage(new StringTextComponent("Ahora jugás con la " + b.nombre + ".")
                .withStyle(TextFormatting.GOLD), true);
    }
}
