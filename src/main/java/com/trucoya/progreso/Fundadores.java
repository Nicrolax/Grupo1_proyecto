package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
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

    // ------------------------------------------------------------------ los cuatro juntos: vencer al Mandinga

    /** Lo que dice cada uno (en el orden en que hablan) y cuánto dura su audio, en segundos. */
    static final String[] FELICITA = {
        "¡Bo, lo lograste! Le ganaste al Mandinga y el Negro Ledesma por fin descansa en paz. ¡Qué partidazo!",
        "Te miramos toda la noche desde el monte. Jugaste con una cabeza bárbara. ¡Tremendo trabajo, de verdad!",
        "Ta, ahora sí: esto se festeja con un asado como la gente. Y esta vez la carne la traigo yo, prometido.",
        "Gracias por llegar hasta el final. Hicimos este juego con mucho cariño, y vos le diste vida. ¡Felicitaciones, campeón!",
    };
    static final double[] DURA = {8.12D, 8.34D, 6.92D, 6.96D};
    public static final String FELICITARON = "fundadores_felicitaron";

    /** Los cuatro, de visita a la vez. */
    private static final class Grupo {
        final UUID jugador;
        final UUID[] gauchos = new UUID[4];
        final UUID[] caballos = new UUID[4];
        int t;
        int fase;            // 0 espera, 1 llegan, 2 hablan, 3 se van
        int[] habla = new int[4];
        Vector3d frente;     // de qué lado del jugador se arman

        Grupo(UUID jugador) {
            this.jugador = jugador;
        }
    }

    private static final Map<UUID, Grupo> GRUPOS = new ConcurrentHashMap<>();

    /**
     * Vencieron al Mandinga: un rato después llegan los cuatro Fundadores juntos, se paran en ronda delante del
     * jugador y cada uno lo felicita (con su voz), le regalan la Baraja de los Fundadores y se van.
     * Una sola vez por jugador ({@code forzar} es para el comando de prueba).
     */
    public static void felicitar(ServerPlayerEntity p, boolean forzar) {
        if (!forzar && JugadorData.get(p, FELICITARON) != 0) return;
        if (GRUPOS.containsKey(p.getUUID())) return;
        JugadorData.set(p, FELICITARON, 1);
        GRUPOS.put(p.getUUID(), new Grupo(p.getUUID()));
    }

    private static void tickGrupos(net.minecraft.server.MinecraftServer server) {
        for (Map.Entry<UUID, Grupo> e : GRUPOS.entrySet()) {
            Grupo gr = e.getValue();
            ServerPlayerEntity p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive() || p.level.dimension() != World.OVERWORLD) {
                if (gr.fase > 0) irse(p != null ? p.getLevel() : server.overworld(), gr);
                GRUPOS.remove(e.getKey());
                if (p == null) continue;
                // se cortó (murió o se fue al Nether): llega por carta, no se pierde
                carta(p);
                continue;
            }
            try {
                tickGrupo(p, gr);
            } catch (RuntimeException ex) {
                TrucoYa.LOGGER.error("Truco Ya: la visita de los Fundadores falló", ex);
                irse(p.getLevel(), gr);
                GRUPOS.remove(e.getKey());
                carta(p);
            }
        }
    }

    private static void carta(ServerPlayerEntity p) {
        p.sendMessage(new StringTextComponent("Te llegó una carta de los cuatro Fundadores:").withStyle(TextFormatting.GOLD),
                p.getUUID());
        for (int i = 0; i < 4; i++) {
            p.sendMessage(new StringTextComponent("  " + FUNDADORES[i].nombre + ": " + FELICITA[i])
                    .withStyle(TextFormatting.ITALIC), p.getUUID());
        }
        regalo(p);
    }

    private static void regalo(ServerPlayerEntity p) {
        ItemStack b = new ItemStack(ModItems.BARAJA_FUNDADORES.get());
        com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.FUNDADORES);
        if (!p.inventory.contains(b)) {
            if (!p.inventory.add(b)) p.drop(b, false);
            p.sendMessage(new StringTextComponent("Los Fundadores te regalaron su baraja.").withStyle(TextFormatting.GOLD),
                    p.getUUID());
        }
    }

    private static void tickGrupo(ServerPlayerEntity p, Grupo gr) {
        ServerWorld sw = p.getLevel();
        gr.t++;
        switch (gr.fase) {
            case 0:     // un respiro después de la escena
                if (gr.t > 20 * 4 && puedeLlegar(p)) {
                    if (aparecer(p, gr)) {
                        gr.fase = 1;
                        gr.t = 0;
                        p.displayClientMessage(new StringTextComponent("Se acercan cuatro jinetes...")
                                .withStyle(TextFormatting.GOLD), true);
                    } else if (gr.t > 20 * 60) {
                        GRUPOS.remove(gr.jugador);
                        carta(p);
                    }
                }
                return;
            case 1: {   // llegan y se acomodan en media ronda delante del jugador
                boolean todos = true;
                for (int i = 0; i < 4; i++) {
                    GauchoEntity g = gaucho(sw, gr.gauchos[i]);
                    if (g == null) continue;
                    Vector3d lugar = lugar(p, gr, i);
                    Entity montura = g.getVehicle();
                    double d = g.position().distanceToSqr(lugar.x, g.getY(), lugar.z);
                    if (d > 1.6D * 1.6D) {
                        todos = false;
                        if (gr.t % 10 == 1) {
                            if (montura instanceof AbstractHorseEntity) {
                                ((AbstractHorseEntity) montura).getNavigation().moveTo(lugar.x, lugar.y, lugar.z, 1.5D);
                            } else {
                                g.getNavigation().moveTo(lugar.x, lugar.y, lugar.z, 0.6D);
                            }
                        }
                    } else if (montura instanceof AbstractHorseEntity) {
                        ((AbstractHorseEntity) montura).getNavigation().stop();
                    } else {
                        g.getNavigation().stop();
                    }
                    g.getLookControl().setLookAt(p, 30.0F, 30.0F);
                }
                if (todos || gr.t > 20 * 25) {
                    // el que no encontró el camino, aparece en su lugar
                    for (int i = 0; i < 4; i++) {
                        GauchoEntity g = gaucho(sw, gr.gauchos[i]);
                        if (g == null) continue;
                        Vector3d lugar = lugar(p, gr, i);
                        Entity montura = g.getVehicle();
                        Entity quien = montura != null ? montura : g;
                        if (quien.position().distanceToSqr(lugar.x, quien.getY(), lugar.z) > 2.5D * 2.5D) {
                            quien.teleportTo(lugar.x, lugar.y, lugar.z);
                            sw.sendParticles(net.minecraft.particles.ParticleTypes.POOF, lugar.x, lugar.y + 1, lugar.z,
                                    8, 0.3, 0.5, 0.3, 0.01);
                        }
                        if (montura instanceof AbstractHorseEntity) ((AbstractHorseEntity) montura).getNavigation().stop();
                        g.getNavigation().stop();
                    }
                    int k = 20;
                    for (int i = 0; i < 4; i++) {
                        gr.habla[i] = k;
                        k += (int) Math.round(DURA[i] * 20.0D) + 14;
                    }
                    gr.fase = 2;
                    gr.t = 0;
                }
                return;
            }
            case 2: {   // cada uno, a su turno, con su voz
                for (int i = 0; i < 4; i++) {
                    GauchoEntity g = gaucho(sw, gr.gauchos[i]);
                    if (g == null) continue;
                    Entity montura = g.getVehicle();
                    if (montura instanceof AbstractHorseEntity) ((AbstractHorseEntity) montura).getNavigation().stop();
                    g.getNavigation().stop();
                    // todos miran al jugador; el que habla también lo mira de frente
                    g.getLookControl().setLookAt(p, 30.0F, 30.0F);
                    if (gr.t == gr.habla[i]) {
                        g.say(FELICITA[i]);
                        sw.playSound(null, g.getX(), g.getEyeY(), g.getZ(),
                                com.trucoya.registry.ModSounds.FELICITA[i].get(),
                                net.minecraft.util.SoundCategory.VOICE, 1.6F, 1.0F);
                        p.sendMessage(new StringTextComponent(FUNDADORES[i].nombre + ": ").withStyle(TextFormatting.GOLD)
                                .append(new StringTextComponent(FELICITA[i]).withStyle(TextFormatting.WHITE)), p.getUUID());
                    }
                }
                int fin = gr.habla[3] + (int) Math.round(DURA[3] * 20.0D);
                if (gr.t == fin + 10) {
                    regalo(p);
                    sw.playSound(null, p.blockPosition(), net.minecraft.util.SoundEvents.PLAYER_LEVELUP,
                            net.minecraft.util.SoundCategory.PLAYERS, 0.7F, 1.0F);
                }
                if (gr.t >= fin + 60) {
                    GauchoEntity g = gaucho(sw, gr.gauchos[0]);
                    if (g != null) g.say("¡Nos vemos en la próxima mano!");
                    irse(sw, gr);
                    GRUPOS.remove(gr.jugador);
                }
                return;
            }
            default:
                GRUPOS.remove(gr.jugador);
        }
    }

    /** Dónde se para cada uno: media ronda de frente al jugador, a unos 3,5 bloques. */
    private static Vector3d lugar(ServerPlayerEntity p, Grupo gr, int i) {
        double base = Math.atan2(gr.frente.z, gr.frente.x);
        double a = base + Math.toRadians((i - 1.5D) * 30.0D);
        double x = p.getX() + Math.cos(a) * 3.6D, z = p.getZ() + Math.sin(a) * 3.6D;
        BlockPos b = new BlockPos(x, p.getY(), z);
        ServerWorld sw = p.getLevel();
        int y = sw.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, b.getX(), b.getZ());
        if (Math.abs(y - p.getY()) > 3) y = (int) Math.floor(p.getY());
        return new Vector3d(x, y, z);
    }

    private static boolean aparecer(ServerPlayerEntity p, Grupo gr) {
        ServerWorld sw = p.getLevel();
        Vector3d mira = Vector3d.directionFromRotation(0.0F, p.yRot);
        gr.frente = new Vector3d(mira.x, 0, mira.z).normalize();
        boolean cielo = sw.canSeeSky(p.blockPosition().above());
        BlockPos spot = null;
        for (int intento = 0; intento < 30 && spot == null; intento++) {
            double ang = Math.atan2(gr.frente.z, gr.frente.x) + (RANDOM.nextDouble() - 0.5D) * 1.6D;
            int dist = cielo ? 16 + RANDOM.nextInt(6) : 4 + RANDOM.nextInt(3);
            int x = p.blockPosition().getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = p.blockPosition().getZ() + (int) Math.round(Math.sin(ang) * dist);
            int y = cielo ? sw.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) : p.blockPosition().getY();
            BlockPos b = new BlockPos(x, y, z);
            if (sw.getFluidState(b.below()).isEmpty() && sw.getBlockState(b.below()).getMaterial().isSolid()
                    && sw.getBlockState(b).getCollisionShape(sw, b).isEmpty()
                    && Math.abs(y - p.blockPosition().getY()) < 6) {
                spot = b;
            }
        }
        if (spot == null) {
            if (!cielo) return false;
            spot = p.blockPosition().offset((int) Math.round(gr.frente.x * 6), 0, (int) Math.round(gr.frente.z * 6));
        }
        int[] pelajes = {3, 0, 4, 2 + 256};
        for (int i = 0; i < 4; i++) {
            Fundador f = FUNDADORES[i];
            GauchoEntity g = ModEntities.GAUCHO.get().create(sw);
            if (g == null) {
                irse(sw, gr);
                return false;
            }
            double lado = (i - 1.5D) * 1.6D;
            double x = spot.getX() + 0.5D - gr.frente.z * lado, z = spot.getZ() + 0.5D + gr.frente.x * lado;
            float yaw = (float) (Math.atan2(-gr.frente.z, -gr.frente.x) * 180.0D / Math.PI) - 90.0F;
            g.setRol(GauchoEntity.Rol.FUNDADOR);
            g.setVariante(f.variante);
            g.setCustomName(new StringTextComponent(f.nombre));
            g.setNivel(5);
            g.moveTo(x, spot.getY(), z, yaw, 0F);
            g.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
            g.initDefaults();
            g.setDeVisitaGrupal(true);
            if (cielo) {
                HorseEntity horse = EntityType.HORSE.create(sw);
                if (horse != null) {
                    horse.moveTo(x, spot.getY(), z, yaw, 0F);
                    horse.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
                    net.minecraft.nbt.CompoundNBT n = horse.saveWithoutId(new net.minecraft.nbt.CompoundNBT());
                    n.putInt("Variant", pelajes[i]);
                    horse.load(n);
                    horse.setTamed(true);   // en pelo: con montura, el caballo espera que el jinete lo maneje y no camina
                    horse.getAttribute(net.minecraft.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(64.0D);
                    sw.addFreshEntity(horse);
                    gr.caballos[i] = horse.getUUID();
                    sw.addFreshEntity(g);
                    g.startRiding(horse, true);
                    g.setHorse(horse.getUUID());
                } else {
                    sw.addFreshEntity(g);
                }
            } else {
                sw.addFreshEntity(g);
                sw.sendParticles(net.minecraft.particles.ParticleTypes.POOF, x, spot.getY() + 1, z, 10, 0.3, 0.5, 0.3, 0.01);
            }
            gr.gauchos[i] = g.getUUID();
        }
        return true;
    }

    private static void irse(ServerWorld sw, Grupo gr) {
        for (UUID id : gr.gauchos) {
            GauchoEntity g = gaucho(sw, id);
            if (g != null) {
                g.setDeVisitaGrupal(false);
                g.despedirse();
            }
        }
    }

    @javax.annotation.Nullable
    private static GauchoEntity gaucho(ServerWorld sw, @javax.annotation.Nullable UUID id) {
        if (id == null) return null;
        Entity e = sw.getEntity(id);
        return e instanceof GauchoEntity && e.isAlive() ? (GauchoEntity) e : null;
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
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (!GRUPOS.isEmpty()) {
            tickGrupos(server);
        }
        if (reloj % 20 != 0 || PENDIENTES.isEmpty()) {
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
        horse.setTamed(true);   // en pelo: con montura, el caballo espera que el jinete lo maneje y no camina
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

    /** Quien ya venció al Mandinga (en una versión anterior, o se fue a mitad de la visita) los recibe al entrar. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
            if (Salamanca.etapa(p) == Salamanca.LIBRE && JugadorData.get(p, FELICITARON) == 0) {
                felicitar(p, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EN_CAMINO.remove(event.getPlayer().getUUID());
        Grupo gr = GRUPOS.remove(event.getPlayer().getUUID());
        if (gr != null && event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
            if (gr.fase > 0) irse(p.getLevel(), gr);
            JugadorData.set(p, FELICITARON, 0);    // vuelven la próxima vez que entre
        }
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
