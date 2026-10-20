package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.core.Prestigio;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

/**
 * Otorga los logros del árbol "Truco Ya" (data/trucoya/advancements). Los criterios son de tipo
 * {@code minecraft:impossible} y se conceden desde el código cuando ocurre la hazaña.
 */
public final class Logros {

    public static final String TOMA_ASIENTO = "iniciacion/toma_asiento";
    public static final String EL_CEBA = "iniciacion/el_ceba";
    public static final String CARTAS_SOBRE_LA_MESA = "iniciacion/cartas_sobre_la_mesa";
    public static final String PICADA_COMPLETA = "iniciacion/picada_completa";
    public static final String FLOR_DE_VIAJE = "tigre/flor_de_viaje";
    public static final String PURO_CUENTO = "tigre/puro_cuento";
    public static final String CRECIENDO = "tigre/creciendo_en_el_boliche";
    public static final String CONTRAFLOR_AL_RESTO = "tigre/contraflor_al_resto";
    public static final String DESAFIO_FORASTERO = "tigre/el_desafio_del_forastero";
    public static final String NOCHE_CLANDESTINA = "leyenda/noche_clandestina";
    public static final String PESCUEZO_DORADO = "leyenda/pescuezo_dorado";
    public static final String REMONTADA = "leyenda/remontada_historica";
    public static final String DERROTANDO_LEYENDA = "leyenda/derrotando_a_la_leyenda";
    public static final String INVICTO = "leyenda/el_invicto_del_uruguay";
    // El Camino del Truco (historia)
    public static final String PRIMER_SELLO = "historia/primer_sello";
    public static final String RUTA_COMPLETA = "historia/la_ruta_completa";
    public static final String MAPA_BAQUEANO = "historia/el_mapa_del_baqueano";
    public static final String CAMPEON_TORNEO = "historia/campeon_del_torneo";
    public static final String PINGO_GANADOR = "historia/pingo_ganador";
    public static final String EL_FINAL = "historia/el_ultimo_truco";
    // La Salamanca
    public static final String LA_SALAMANCA = "historia/la_salamanca";
    public static final String ALMA_LIBRE = "historia/alma_libre";
    public static final String GAUCHO_DE_PALABRA = "iniciacion/gaucho_de_palabra";
    public static final String COLECCIONISTA = "historia/coleccionista";
    public static final String SIETE_DIAS = "iniciacion/siete_dias_seguidos";

    private Logros() {
    }

    public static void award(ServerPlayerEntity player, String path) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Advancement adv = server.getAdvancements().getAdvancement(new ResourceLocation(TrucoYa.MOD_ID, path));
        if (adv == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(adv).getRemainingCriteria()) {
            player.getAdvancements().award(adv, criterion);
        }
    }

    public static void sentarse(ServerPlayerEntity p) {
        award(p, TOMA_ASIENTO);
    }

    public static void flor(ServerPlayerEntity p) {
        award(p, FLOR_DE_VIAJE);
    }

    public static void farol(ServerPlayerEntity p) {
        award(p, PURO_CUENTO);
    }

    public static void contraflorAlResto(ServerPlayerEntity p) {
        award(p, CONTRAFLOR_AL_RESTO);
    }

    /** Ganó un pozo; en la guarida y con oro de por medio: "Pescuezo Dorado". */
    public static void pozoGanado(ServerPlayerEntity p, boolean clandestinaConOro) {
        if (clandestinaConOro) award(p, PESCUEZO_DORADO);
    }

    public static void partidoGanado(ServerPlayerEntity p, boolean forastero, boolean vizcacha, boolean remontada,
                                     int racha, int nivel, boolean clandestina) {
        award(p, CARTAS_SOBRE_LA_MESA);
        if (forastero) award(p, DESAFIO_FORASTERO);
        if (vizcacha) award(p, DERROTANDO_LEYENDA);
        if (remontada) award(p, REMONTADA);
        if (clandestina) award(p, NOCHE_CLANDESTINA);
        if (nivel >= 3) award(p, CRECIENDO);
        if (racha >= 10 || nivel >= Prestigio.MAX_NIVEL) award(p, INVICTO);
    }
}
