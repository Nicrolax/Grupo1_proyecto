package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.network.LibretaPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModItems;
import com.trucoya.world.ModStructures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * "El Camino del Truco": la historia del mod.
 *
 * <ol>
 *   <li><b>Primeras manos</b>: ganá tu primer partido.</li>
 *   <li><b>La Ruta de las Pulperías</b>: ganale al campeón de la pulpería de cada una de las 6 regiones
 *       y llevate su sello (Llanura, Sabana, Desierto, Monte, Nieve y Sierra).</li>
 *   <li><b>Taita del Truco</b>: llegá al Nivel 4 de Prestigio Criollo.</li>
 *   <li><b>El Mapa del Baqueano</b>: con los seis sellos y el nivel, te dan el mapa que marca la Estancia del Caudillo.</li>
 *   <li><b>El Último Truco</b>: ganale al Viejo Vizcacha (antes no acepta sentarse con vos).</li>
 *   <li><b>Epílogo</b>: la cinemática final.</li>
 * </ol>
 * El avance se guarda en los datos persistentes del jugador ({@link JugadorData}) y se ve en la Libreta del Truquero.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Historia {

    public static final String SELLOS = "sellos";
    public static final String MAPA = "mapa_baqueano";
    public static final String VIZCACHA_VENCIDO = "vizcacha_vencido";
    public static final String FINAL_VISTO = "final_visto";
    public static final String LIBRETA = "libreta";
    /** Nicolás ya le dio la bienvenida (en persona o, si no hubo forma, por carta). */
    public static final String BIENVENIDA = "bienvenida";
    public static final String TORNEOS = "torneos";
    public static final String CUADRERAS = "cuadreras";
    /** Nivel de Prestigio que pide el Vizcacha (Taita del Truco). */
    public static final int NIVEL_VIZCACHA = 4;

    /** Las seis regiones de la Ruta de las Pulperías. */
    public enum Region {
        LLANURA("la Llanura", 2, 0x7FB04A, "Pulperías de pradera, en campo abierto (y al lado de las aldeas de llanura)."),
        SABANA("la Sabana", 2, 0xC9A23A, "Entre acacias, en la sabana seca."),
        DESIERTO("el Desierto", 3, 0xE3C27A, "Ranchos de adobe y cal en el desierto (y junto a sus aldeas)."),
        MONTE("el Monte", 3, 0x3E7A3A, "En el bosque, la taiga, la selva o el pantano."),
        NIEVE("la Nieve", 4, 0xDCEBF2, "En los campos helados."),
        SIERRA("la Sierra", 4, 0x9A7B5C, "En las sierras y montañas, o en las mesetas de arcilla.");

        public final String nombre;
        /** Nivel con el que juega el campeón de esa región mientras no tengas su sello. */
        public final int nivelCampeon;
        public final int color;
        public final String pista;

        Region(String nombre, int nivelCampeon, int color, String pista) {
            this.nombre = nombre;
            this.nivelCampeon = nivelCampeon;
            this.color = color;
            this.pista = pista;
        }

        public int bit() {
            return 1 << ordinal();
        }

        public static Region de(Biome.Category c) {
            switch (c) {
                case SAVANNA: return SABANA;
                case DESERT: return DESIERTO;
                case FOREST: case TAIGA: case JUNGLE: case SWAMP: return MONTE;
                case ICY: return NIEVE;
                case EXTREME_HILLS: case MESA: return SIERRA;
                default: return LLANURA;
            }
        }
    }

    /** Los números del truquero que muestra la Libreta (mismo orden que LibretaScreen.NUMEROS). */
    public static final String[] NUMEROS = {JugadorData.GANADOS, JugadorData.PERDIDOS, JugadorData.RACHA, JugadorData.FLORES,
            JugadorData.FAROLES, JugadorData.FORASTEROS, "torneos", "cuadreras", Pedidos.CUMPLIDOS, JugadorData.VIZCACHA,
            JugadorData.MEJOR_RACHA, "diario_completos", "diario_mejor_racha"};

    public static final int TODOS_LOS_SELLOS = (1 << Region.values().length) - 1;

    private Historia() {
    }

    // ------------------------------------------------------------------ consultas

    public static int sellos(PlayerEntity p) {
        return JugadorData.get(p, SELLOS) & TODOS_LOS_SELLOS;
    }

    public static boolean tieneSello(PlayerEntity p, Region r) {
        return (sellos(p) & r.bit()) != 0;
    }

    public static int cantidadSellos(PlayerEntity p) {
        return Integer.bitCount(sellos(p));
    }

    /** ¿Puede jugarle al Viejo Vizcacha? (en creativo, siempre, para probar) */
    public static boolean habilitadoVizcacha(PlayerEntity p) {
        if (p.isCreative()) {
            return true;
        }
        return sellos(p) == TODOS_LOS_SELLOS && JugadorData.nivel(p) >= NIVEL_VIZCACHA;
    }

    /** Lo que el Vizcacha le contesta a quien todavía no está listo. */
    public static String negativaVizcacha(PlayerEntity p) {
        int faltan = Region.values().length - cantidadSellos(p);
        if (faltan > 0) {
            return "Todavía no, gurí. Te faltan " + faltan + (faltan == 1 ? " sello" : " sellos") + " de la Ruta.";
        }
        return "Juntaste los sellos... pero volvé cuando seas Taita del Truco.";
    }

    /** Región de la pulpería donde está esta posición, o null si no es una pulpería. */
    @Nullable
    public static Region pulperiaDe(ServerWorld w, BlockPos pos) {
        for (Structure<?> s : new Structure<?>[] {ModStructures.PULPERIA.get(), ModStructures.PULPERIA_ALDEA.get()}) {
            StructureStart<?> start = w.structureFeatureManager().getStructureAt(pos, true, s);
            if (start != null && start.isValid()) {
                // el mismo bioma con el que la estructura eligió su estilo
                Biome b = w.getChunkSource().getGenerator().getBiomeSource()
                        .getNoiseBiome((start.getChunkX() << 2) + 2, 0, (start.getChunkZ() << 2) + 2);
                return Region.de(b.getBiomeCategory());
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ hitos

    /** Ganó un partido contra gauchos en una pulpería de la región {@code r}. */
    public static void ganoEnPulperia(ServerPlayerEntity p, Region r) {
        if (tieneSello(p, r)) {
            return;
        }
        JugadorData.set(p, SELLOS, sellos(p) | r.bit());
        int n = cantidadSellos(p);
        p.sendMessage(new StringTextComponent("✦ Sello de " + r.nombre + " ✦  (" + n + " de 6)")
                .withStyle(TextFormatting.GOLD, TextFormatting.BOLD), p.getUUID());
        p.level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.9F, 1.1F);
        Logros.award(p, Logros.PRIMER_SELLO);
        if (n == Region.values().length) {
            Logros.award(p, Logros.RUTA_COMPLETA);
            p.sendMessage(new StringTextComponent("¡Completaste la Ruta de las Pulperías! Ahora el campo te conoce.")
                    .withStyle(TextFormatting.GOLD), p.getUUID());
        } else {
            p.sendMessage(new StringTextComponent("Mirá en tu Libreta qué regiones te faltan.").withStyle(TextFormatting.GRAY),
                    p.getUUID());
        }
        com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.deRegion(r));
        Fundadores.hito(p, n);
        revisarMapa(p);
    }

    /** Con los seis sellos y el nivel, llega el Mapa del Baqueano. */
    public static void revisarMapa(ServerPlayerEntity p) {
        if (JugadorData.get(p, MAPA) != 0 || sellos(p) != TODOS_LOS_SELLOS || JugadorData.nivel(p) < NIVEL_VIZCACHA) {
            return;
        }
        ItemStack mapa = crearMapa(p);
        if (mapa.isEmpty()) {
            return;
        }
        JugadorData.set(p, MAPA, 1);
        Fundadores.entregarMapa(p, mapa);
        Logros.award(p, Logros.MAPA_BAQUEANO);
    }

    /** Un mapa de explorador con la Estancia del Caudillo marcada con una cruz. */
    public static ItemStack crearMapa(ServerPlayerEntity p) {
        ServerWorld w = p.getLevel();
        BlockPos estancia = ubicarEstancia(w, p.blockPosition());
        if (estancia == null) {
            return ItemStack.EMPTY;
        }
        ItemStack mapa = FilledMapItem.create(w, estancia.getX(), estancia.getZ(), (byte) 2, true, true);
        FilledMapItem.renderBiomePreviewMap(w, mapa);
        net.minecraft.world.storage.MapData.addTargetDecoration(mapa, estancia, "+", MapDecoration.Type.RED_X);
        mapa.setHoverName(new StringTextComponent("Mapa del Baqueano").withStyle(TextFormatting.GOLD));
        return mapa;
    }

    @Nullable
    public static BlockPos ubicarEstancia(ServerWorld w, BlockPos desde) {
        try {
            return w.findNearestMapFeature(ModStructures.ESTANCIA.get(), desde, 200, false);
        } catch (RuntimeException e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo ubicar la Estancia del Caudillo", e);
            return null;
        }
    }

    /** Le ganó al Viejo Vizcacha: queda anotado (la cinemática final la dispara la mesa). */
    public static void vencioAlVizcacha(ServerPlayerEntity p) {
        JugadorData.set(p, VIZCACHA_VENCIDO, 1);
    }

    // ------------------------------------------------------------------ libreta

    public static void abrirLibreta(ServerPlayerEntity p) {
        int dx = 0, dz = 0;
        boolean conMapa = JugadorData.get(p, MAPA) != 0;
        if (conMapa) {
            BlockPos e = ubicarEstancia(p.getLevel(), p.blockPosition());
            if (e != null) {
                dx = e.getX() - p.blockPosition().getX();
                dz = e.getZ() - p.blockPosition().getZ();
            }
        }
        int etapa = Salamanca.etapa(p);
        int pdx = 0, pdz = 0;
        if (etapa == Salamanca.ABIERTA) {
            BlockPos paso = SalamancaSitio.aproximado(p.getLevel());
            pdx = paso.getX() - p.blockPosition().getX();
            pdz = paso.getZ() - p.blockPosition().getZ();
        }
        LibretaPacket pk = new LibretaPacket(sellos(p), JugadorData.prestigio(p), JugadorData.get(p, JugadorData.GANADOS),
                conMapa, dx, dz, JugadorData.get(p, VIZCACHA_VENCIDO) != 0, JugadorData.get(p, FINAL_VISTO) != 0,
                JugadorData.get(p, TORNEOS), JugadorData.get(p, CUADRERAS), etapa, pdx, pdz);
        pk.barajas = com.trucoya.barajas.Coleccion.bits(p);
        pk.elegida = com.trucoya.barajas.Coleccion.elegida(p).ordinal();
        pk.pedidos = Pedidos.paraLibreta(p);
        pk.pedidoIds = Pedidos.ids(p);
        pk.diario = Diario.paraLibreta(p);
        pk.diarioRacha = Diario.racha(p);
        pk.numeros = new int[NUMEROS.length];
        for (int i = 0; i < NUMEROS.length; i++) pk.numeros[i] = JugadorData.get(p, NUMEROS[i]);
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pk);
    }

    /** La primera vez que alguien entra al mundo recibe la Libreta del Truquero. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
        com.trucoya.barajas.Coleccion.ponerAlDia(p);
        if (JugadorData.get(p, LIBRETA) == 0) {
            JugadorData.set(p, LIBRETA, 1);
            ItemStack libreta = new ItemStack(ModItems.LIBRETA.get());
            if (!p.inventory.add(libreta)) {
                p.drop(libreta, false);
            }
            p.sendMessage(new StringTextComponent("Recibiste la Libreta del Truquero: ahí está tu camino hasta el Viejo Vizcacha.")
                    .withStyle(TextFormatting.GOLD), p.getUUID());
        }
        // Nicolás recibe a todos al entrar al mundo (también en mundos que ya existían)
        if (JugadorData.get(p, BIENVENIDA) == 0) {
            Fundadores.bienvenida(p);
        }
    }
}
