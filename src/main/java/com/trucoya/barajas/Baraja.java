package com.trucoya.barajas;

import net.minecraft.util.ResourceLocation;

/**
 * Las barajas coleccionables. Cada una tiene su dibujo completo (las 40 cartas y el dorso):
 * tools/barajas_temas.py las pinta en textures/gui/cartas_&lt;clave&gt;.png (alta resolución). La misma textura,
 * con mipmaps (client.CartasTextura), sirve para el tapete y para las cartas sobre la mesa en el mundo.
 * El orden importa: es el número que se guarda (no cambiarlo, solo agregar al final).
 */
public enum Baraja {
    CRIOLLA("criolla", "Baraja Criolla", "La de siempre. Viene con la Libreta.", 0xB3262C),
    ORO("oro", "Baraja de Oro", "La del Viejo Vizcacha: se gana venciéndolo.", 0xE8B82A),
    MANDINGA("mandinga", "Baraja del Mandinga", "Se gana en la Salamanca, al amanecer.", 0x8A1218),
    LLANURA("llanura", "Baraja de la Llanura", "Torneo de la Pulpería de la Llanura, o el pulpero de allá.", 0x6E9A3A),
    SABANA("sabana", "Baraja de la Sabana", "Torneo de la Pulpería de la Sabana, o el pulpero de allá.", 0xC8862A),
    DESIERTO("desierto", "Baraja del Desierto", "Torneo de la Pulpería del Desierto, o el pulpero de allá.", 0xC2603A),
    MONTE("monte", "Baraja del Monte", "Torneo de la Pulpería del Monte, o el pulpero de allá.", 0x2F6A44),
    NIEVE("nieve", "Baraja de la Nieve", "Torneo de la Pulpería de la Nieve, o el pulpero de allá.", 0x6A9CC8),
    SIERRA("sierra", "Baraja de la Sierra", "Torneo de la Pulpería de la Sierra, o el pulpero de allá.", 0x6A6A7A),
    FUNDADORES("fundadores", "Baraja de los Fundadores", "Regalo de los cuatro Fundadores después del final.", 0x23365A);

    public final String clave;
    public final String nombre;
    public final String comoSeGana;
    public final int color;

    Baraja(String clave, String nombre, String comoSeGana, int color) {
        this.clave = clave;
        this.nombre = nombre;
        this.comoSeGana = comoSeGana;
        this.color = color;
    }

    /** Las de hazaña son personales; las demás se pueden comprar, regalar o cambiar con otros jugadores. */
    public boolean seRegala() {
        return this != ORO && this != MANDINGA && this != FUNDADORES;
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** Atlas grande (1280 x 960, cartas de 128 x 192) para el tapete. */
    public ResourceLocation atlasGui() {
        return new ResourceLocation("trucoya", "textures/gui/cartas_" + clave + ".png");
    }

    public static Baraja de(int i) {
        Baraja[] t = values();
        return i >= 0 && i < t.length ? t[i] : CRIOLLA;
    }

    /** La de la región (para los torneos y los pulperos de cada pulpería). */
    public static Baraja deRegion(com.trucoya.progreso.Historia.Region r) {
        switch (r) {
            case LLANURA: return LLANURA;
            case SABANA: return SABANA;
            case DESIERTO: return DESIERTO;
            case MONTE: return MONTE;
            case NIEVE: return NIEVE;
            default: return SIERRA;
        }
    }
}
