package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los Fundadores: los cuatro creadores del mod, guías del Camino del Truco. En cada hito de la historia
 * uno de ellos llega a caballo hasta el jugador, le habla, le deja algo y se va:
 * <ul>
 *   <li>al entrar por primera vez: la bienvenida;</li>
 *   <li>con el primer sello: esmeraldas para el primer torneo;</li>
 *   <li>con el tercer sello: el banderín de cuadrera;</li>
 *   <li>con los seis sellos y Nivel 4: el Mapa del Baqueano.</li>
 * </ul>
 * Si el jugador está bajo techo o lejos de un lugar donde se pueda llegar a caballo, se espera un rato;
 * si no se puede, el mensaje y el regalo llegan igual (por chat).
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Fundadores {

    /** Un fundador: nombre, si es mujer (voz) y el aspecto (variante de la textura de Fundador). */
    public static final class Fundador {
        public final String nombre;
        public final boolean mujer;
        public final int variante;

        Fundador(String nombre, boolean mujer, int variante) {
            this.nombre = nombre;
            this.mujer = mujer;
            this.variante = variante;
        }
    }

    /** Los cuatro creadores del mod. */
    public static final Fundador[] FUNDADORES = {
        new Fundador("Nicolás Bentancour", false, 0),
        new Fundador("Agustín Perdomo", false, 1),
        new Fundador("Matías Piñeyro", false, 2),
        new Fundador("Thiago Rodríguez", false, 3),
    };

    private static final class Visita {
        final int fundador;
        final List<String> lineas;
        final ItemStack regalo;
        int intentos;
        int noAntesDe;

        Visita(int fundador, List<String> lineas, ItemStack regalo, int noAntesDe) {
            this.fundador = fundador;
            this.lineas = lineas;
            this.regalo = regalo;
            this.noAntesDe = noAntesDe;
        }
    }

    private static final Map<UUID, Deque<Visita>> PENDIENTES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> EN_CAMINO = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static int reloj;

    private Fundadores() {
    }

    // ------------------------------------------------------------------ hitos de la historia

    public static void bienvenida(ServerPlayerEntity p) {
        encolar(p, 0, Arrays.asList(
                "¡Eeeh, qué hacés, bo! Soy Nico, uno de los que armó este pago.",
                "Esa libreta es tu camino: seis regiones, seis pulperías, seis campeones.",
                "Ganale al campeón de cada una y te llevás su sello. Está de más, vas a ver.",
                "Con los seis sellos y buen prestigio, el Viejo Vizcacha te recibe. Tomá un mate, que el camino es largo."),
                new ItemStack(ModItems.MATE.get()), 20 * 8);
    }

    /** Se ganó el sello número {@code n}. */
    public static void hito(ServerPlayerEntity p, int n) {
        if (n == 1) {
            encolar(p, 1, Arrays.asList(
                    "¡Fua, primer sello! Te re cabió, eh.",
                    "Escuchá esta: en las pulperías hay torneos. Tres rivales al hilo, partidos a 20.",
                    "La inscripción son cinco esmeraldas... tomá, te las tiro yo. Después me invitás una picada."),
                    new ItemStack(Items.EMERALD, 5), 20 * 8);
        } else if (n == 3) {
            encolar(p, 2, Arrays.asList(
                    "¡Tres sellos, bo! Ni ahí pensé que llegabas tan rápido.",
                    "Ta, pero no todo es truco: ¿sabés correr una cuadrera?",
                    "Clavá este banderín, subite al pingo y tocalo. Si perdés, yo no te vi, eh."),
                    new ItemStack(ModItems.RAYA_CUADRERA.get()), 20 * 8);
        }
    }

    public static void entregarMapa(ServerPlayerEntity p, ItemStack mapa) {
        encolar(p, 3, Arrays.asList(
                "Posta que el campo entero habla de vos. Seis sellos y Taita del Truco... tremendo.",
                "El Viejo Vizcacha vive en la Estancia del Caudillo. Casi nadie sabe dónde queda.",
                "Tomá el Mapa del Baqueano: la cruz roja marca la estancia. Ni se te ocurra perderlo.",
                "Y cuando lo veas, escuchalo. Tiene una historia que te va a volar la cabeza."),
                mapa, 20 * 5);
    }

    // ------------------------------------------------------------------ cola de visitas

    private static void encolar(ServerPlayerEntity p, int fundador, List<String> lineas, ItemStack regalo, int espera) {
        PENDIENTES.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>())
                .add(new Visita(fundador, lineas, regalo, reloj + espera));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        reloj++;
        if (reloj % 20 != 0 || PENDIENTES.isEmpty()) {
            return;
        }
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (Map.Entry<UUID, Deque<Visita>> e : PENDIENTES.entrySet()) {
            ServerPlayerEntity p = server.getPlayerList().getPlayer(e.getKey());
            Deque<Visita> cola = e.getValue();
            if (p == null || cola.isEmpty()) {
                continue;
            }
            // de a uno: si todavía hay un fundador en camino, se espera
            UUID enCamino = EN_CAMINO.get(p.getUUID());
            if (enCamino != null) {
                net.minecraft.entity.Entity ent = p.getLevel().getEntity(enCamino);
                if (ent instanceof GauchoEntity && ent.isAlive() && ((GauchoEntity) ent).estaDeVisita()) {
                    continue;
                }
                EN_CAMINO.remove(p.getUUID());
            }
            Visita v = cola.peek();
            if (reloj < v.noAntesDe) {
                continue;
            }
            if (puedeLlegar(p) && llegar(p, v)) {
                cola.poll();
                if (v.fundador == 0) JugadorData.set(p, Historia.BIENVENIDA, 1);
            } else if (++v.intentos > 90) {
                if (v.fundador == 0) JugadorData.set(p, Historia.BIENVENIDA, 1);
                // no hubo forma de llegar a caballo (bajo tierra, en el Nether...): llega igual, por carta
                cola.poll();
                Fundador f = FUNDADORES[v.fundador];
                p.sendMessage(new StringTextComponent("Te llegó una carta de " + f.nombre + ":").withStyle(TextFormatting.GOLD),
                        p.getUUID());
                for (String l : v.lineas) {
                    p.sendMessage(new StringTextComponent("  " + l).withStyle(TextFormatting.ITALIC), p.getUUID());
                }
                if (!v.regalo.isEmpty() && !p.inventory.add(v.regalo.copy())) {
                    p.drop(v.regalo.copy(), false);
                }
            }
        }
    }

    private static boolean puedeLlegar(ServerPlayerEntity p) {
        return p.isAlive() && p.level.dimension() == World.OVERWORLD && !CinematicaServidor.enCurso(p)
                && !Cuadreras.corriendo(p) && !(p.getVehicle() instanceof com.trucoya.entity.SeatEntity);
    }

    /**
     * Hace aparecer al fundador y lo manda a visitar al jugador: a caballo, a unos 12-16 bloques, si el
     * jugador está a cielo abierto; si está bajo techo (una casa, una cueva), llega a pie, más cerca.
     */
    private static boolean llegar(ServerPlayerEntity p, Visita v) {
        ServerWorld sw = p.getLevel();
        if (!sw.canSeeSky(p.blockPosition().above())) {
            return llegarAPie(p, v);
        }
        BlockPos spot = null;
        for (int attempt = 0; attempt < 24 && spot == null; attempt++) {
            double ang = RANDOM.nextDouble() * Math.PI * 2;
            int dist = 12 + RANDOM.nextInt(5);
            int x = p.blockPosition().getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = p.blockPosition().getZ() + (int) Math.round(Math.sin(ang) * dist);
            int y = sw.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos b = new BlockPos(x, y, z);
            if (sw.getFluidState(b.below()).isEmpty() && sw.getBlockState(b.below()).getMaterial().isSolid()
                    && Math.abs(y - p.blockPosition().getY()) < 6) {
                spot = b;
            }
        }
        if (spot == null) {
            return llegarAPie(p, v);
        }
        GauchoEntity g = ModEntities.GAUCHO.get().create(sw);
        HorseEntity horse = EntityType.HORSE.create(sw);
        if (g == null || horse == null) {
            return false;
        }
        Fundador f = FUNDADORES[v.fundador];
        g.setRol(GauchoEntity.Rol.FUNDADOR);
        g.setVariante(f.variante);
        g.setCustomName(new StringTextComponent(f.nombre));
        g.setNivel(5);
        float yaw = RANDOM.nextFloat() * 360F;
        horse.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, yaw, 0F);
        horse.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
        horse.setTamed(true);
        horse.equipSaddle(null);
        // que el caballo pueda "ver" hasta el jugador (por defecto calcula caminos de solo 16 bloques)
        horse.getAttribute(net.minecraft.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(64.0D);
        g.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, yaw, 0F);
        g.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
        g.initDefaults();
        sw.addFreshEntity(horse);
        sw.addFreshEntity(g);
        g.startRiding(horse, true);
        g.setHorse(horse.getUUID());
        g.visitar(p, v.lineas, v.regalo, false);
        EN_CAMINO.put(p.getUUID(), g.getUUID());
        p.displayClientMessage(new StringTextComponent("Se acerca " + f.nombre + " a caballo...").withStyle(TextFormatting.GOLD), true);
        return true;
    }

    /** Llega caminando: aparece en un lugar libre a pocos bloques (dentro de la casa, de la cueva...). */
    private static boolean llegarAPie(ServerPlayerEntity p, Visita v) {
        ServerWorld sw = p.getLevel();
        BlockPos base = p.blockPosition();
        BlockPos spot = null;
        for (int attempt = 0; attempt < 60 && spot == null; attempt++) {
            int dist = 3 + attempt / 10;
            double ang = RANDOM.nextDouble() * Math.PI * 2;
            BlockPos b = base.offset((int) Math.round(Math.cos(ang) * dist), RANDOM.nextInt(3) - 1,
                    (int) Math.round(Math.sin(ang) * dist));
            boolean libre = sw.getBlockState(b).getCollisionShape(sw, b).isEmpty()
                    && sw.getBlockState(b.above()).getCollisionShape(sw, b.above()).isEmpty()
                    && sw.getFluidState(b).isEmpty();
            if (libre && sw.getBlockState(b.below()).isFaceSturdy(sw, b.below(), net.minecraft.util.Direction.UP)) {
                spot = b;
            }
        }
        if (spot == null) {
            return false;
        }
        GauchoEntity g = ModEntities.GAUCHO.get().create(sw);
        if (g == null) {
            return false;
        }
        Fundador f = FUNDADORES[v.fundador];
        g.setRol(GauchoEntity.Rol.FUNDADOR);
        g.setVariante(f.variante);
        g.setCustomName(new StringTextComponent(f.nombre));
        g.setNivel(5);
        g.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, RANDOM.nextFloat() * 360F, 0F);
        g.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
        g.initDefaults();
        sw.addFreshEntity(g);
        sw.sendParticles(net.minecraft.particles.ParticleTypes.POOF, g.getX(), g.getY() + 1, g.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
        g.visitar(p, v.lineas, v.regalo, false);
        EN_CAMINO.put(p.getUUID(), g.getUUID());
        p.displayClientMessage(new StringTextComponent("Llegó " + f.nombre + "...").withStyle(TextFormatting.GOLD), true);
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EN_CAMINO.remove(event.getPlayer().getUUID());
        // las visitas pendientes se pierden al salir solo si no eran importantes: el mapa se rehace al volver
        Deque<Visita> cola = PENDIENTES.remove(event.getPlayer().getUUID());
        if (cola != null && event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
            for (Visita v : cola) {
                if (!v.regalo.isEmpty() && !p.inventory.add(v.regalo.copy())) {
                    p.drop(v.regalo.copy(), false);
                }
            }
        }
    }
}
