package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.network.EscenaPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;

/**
 * Capítulo 7, LA SALAMANCA. Después de ganarle al Viejo Vizcacha:
 * <ol>
 *   <li>al atardecer del día siguiente el Vizcacha viene a buscar al jugador (donde esté), con un caballo de tiro;</li>
 *   <li>media cinemática: le cuenta la verdad sobre el Negro Ledesma y el Mandinga y le da el Farol de las Ánimas;</li>
 *   <li>cabalgan juntos al Paso de las Ánimas: cae la noche, se larga la tormenta, rayos y luces malas
 *       (si es muy lejos, "esa noche" la película salta al último tramo);</li>
 *   <li>al llegar, entre rayos, la tapera se rearma: la Salamanca, con el Mandinga y el fantasma de Ledesma a la mesa;</li>
 *   <li>la partida: el jugador y el Vizcacha contra el Mandinga y Ledesma, a 30
 *       (las señas de Ledesma y su "con flor me achico" los maneja la mesa);</li>
 *   <li>si ganan, el amanecer: el Mandinga se esfuma, Ledesma se despide y queda libre, la Salamanca vuelve a ser
 *       tapera y el jugador recibe el Facón de Ledesma. Si pierden, otra noche, con el farol, se vuelve a intentar.</li>
 * </ol>
 * Una sola por servidor a la vez (la noche y la tormenta son de todo el mundo). Nada queda a medias: si el jugador
 * se va, muere o se cae el servidor, la tapera vuelve a ser tapera y los personajes se van.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Salamanca {

    /** Etapa del jugador (en sus datos persistentes). */
    public static final String ETAPA = "salamanca";
    public static final String DIA = "salamanca_dia";
    public static final int NADA = 0, ESPERA = 1, ABIERTA = 2, LIBRE = 3;

    enum Fase { LLEGA, VISITA, MONTAR, CABALGATA, VIAJE, LLEGADA, ESPERA_MESA, PARTIDA, AMANECER, SALIDA, DERROTA }

    private static final class Corrida {
        final UUID jugador;
        Fase fase;
        int t;
        @Nullable UUID guia, caballoGuia, caballoJugador, mandinga, ledesma;
        @Nullable BlockPos origen;
        @Nullable Vector3d congelar;
        boolean viajeHecho;
        boolean nocheArmada;
        boolean ganaron;
        int esperaFin = -1;
        int proximoRayo = 40;
        Vector3d ultimoLugar = Vector3d.ZERO;
        int esperandoDesde = -1;
        @Nullable UUID montadoViaje;

        Corrida(UUID jugador, Fase fase) {
            this.jugador = jugador;
            this.fase = fase;
        }
    }

    @Nullable
    private static Corrida corrida;
    private static final Random RND = new Random();
    /** Al cargar el mundo: revisar (en el primer tick) si la Salamanca quedó armada de una sesión anterior. */
    private static boolean revisarArmada;

    private Salamanca() {
    }

    // ------------------------------------------------------------------ consultas

    public static boolean activa() {
        return corrida != null;
    }

    public static int etapa(PlayerEntity p) {
        return JugadorData.get(p, ETAPA);
    }

    /** Los personajes de la Salamanca existen solo mientras dura la corrida. */
    public static boolean esActor(GauchoEntity g) {
        Corrida c = corrida;
        if (c == null) {
            return false;
        }
        UUID id = g.getUUID();
        return id.equals(c.guia) || id.equals(c.mandinga) || id.equals(c.ledesma);
    }

    /** El jugador está en medio del capítulo: nadie le pega ni lo persigue. */
    public static boolean protegido(PlayerEntity p) {
        Corrida c = corrida;
        return c != null && p.getUUID().equals(c.jugador) && c.fase != Fase.SALIDA;
    }

    /** El dueño de esta Salamanca (solo él puede jugar en la mesa del Mandinga). */
    public static boolean esJugador(PlayerEntity p) {
        Corrida c = corrida;
        return c != null && p.getUUID().equals(c.jugador);
    }

    public static boolean esMesa(World w, BlockPos pos) {
        Corrida c = corrida;
        return c != null && c.origen != null && w.dimension() == World.OVERWORLD
                && SalamancaSitio.mundo(c.origen, SalamancaSitio.MESA).equals(pos);
    }

    /** Nivel del bot para cada uno de la Salamanca (el Mandinga y el Vizcacha, al máximo). */
    public static int nivelBot(GauchoEntity g, int porDefecto) {
        switch (g.getRol()) {
            case MANDINGA:
            case VIZCACHA:
                return 6;
            case LEDESMA:
                return 4;
            default:
                return porDefecto;
        }
    }

    // ------------------------------------------------------------------ hitos que avisan otros

    /** Se vio la cinemática final: al otro día, al atardecer, viene el Vizcacha. */
    public static void vioElFinal(ServerPlayerEntity p) {
        if (etapa(p) == NADA) {
            JugadorData.set(p, ETAPA, ESPERA);
            JugadorData.set(p, DIA, (int) (p.getLevel().getDayTime() / 24000L));
        }
    }

    public static void partidaEmpezo(World w, BlockPos mesa) {
        Corrida c = corrida;
        if (c != null && esMesa(w, mesa) && c.fase == Fase.ESPERA_MESA) {
            cambiar(c, Fase.PARTIDA);
        }
    }

    public static void partidaTermino(World w, BlockPos mesa, boolean ganaron) {
        Corrida c = corrida;
        if (c != null && esMesa(w, mesa) && c.fase == Fase.PARTIDA) {
            c.ganaron = ganaron;
            c.esperaFin = c.t + 70;
        }
    }

    public static void partidaAbandonada(World w, BlockPos mesa) {
        Corrida c = corrida;
        if (c != null && esMesa(w, mesa) && c.fase == Fase.PARTIDA) {
            c.ganaron = false;
            c.esperaFin = c.t + 20;
        }
    }

    /** Clic derecho a uno de la Salamanca. */
    public static void hablarCon(GauchoEntity g, PlayerEntity p) {
        Corrida c = corrida;
        String txt;
        switch (g.getRol()) {
            case MANDINGA:
                txt = c != null && c.fase == Fase.ESPERA_MESA ? "Sentate, si tenés coraje. La silla de enfrente del viejo es tuya."
                        : "No me toques, gurí. Acá se arregla todo con las cartas.";
                break;
            case LEDESMA:
                txt = "...";
                break;
            default:
                txt = c == null ? "Vamos, gurí." : c.fase == Fase.MONTAR ? "Subite al tordillo, que se hace tarde."
                        : c.fase == Fase.ESPERA_MESA ? "Sentate frente a mí. Vos y yo, contra el diablo."
                        : "Seguime, gurí. No te quedes atrás.";
        }
        g.say(txt);
    }

    // ------------------------------------------------------------------ el farol

    /** Cada segundo, con el farol en la mano. */
    public static void farol(ServerPlayerEntity p) {
        ServerWorld w = p.getLevel();
        if (w.dimension() != World.OVERWORLD) {
            return;
        }
        int e = etapa(p);
        if (e != ABIERTA && !(corrida != null && corrida.fase == Fase.CABALGATA)) {
            if (e == LIBRE && p.tickCount % 200 == 0) {
                p.displayClientMessage(new StringTextComponent("El farol ya no tiene luces malas: el Negro descansa en paz.")
                        .withStyle(TextFormatting.GRAY), true);
            }
            return;
        }
        long hora = w.getDayTime() % 24000L;
        boolean noche = hora >= 12800L && hora <= 23200L;
        if (!noche) {
            if (p.tickCount % 120 == 0) {
                p.displayClientMessage(new StringTextComponent("De día las luces malas no se ven. Volvé de noche.")
                        .withStyle(TextFormatting.GRAY), true);
            }
            return;
        }
        Vector3d aprox = Vector3d.atCenterOf(SalamancaSitio.aproximado(w));
        // el lugar exacto (que puede obligar a generar terreno) solo se busca ya cerca
        BlockPos o = horizontal(p.position(), aprox) < 96.0D ? SalamancaSitio.origen(w) : null;
        Vector3d destino = o != null ? SalamancaSitio.centro(o, SalamancaSitio.LLEGADA) : aprox;
        Vector3d d = destino.subtract(p.position());
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        if (corrida == null && o != null && dist < 40.0D) {
            revancha(p, o);
            return;
        }
        Vector3d u = dist < 1.0D ? Vector3d.ZERO : new Vector3d(d.x / dist, 0, d.z / dist);
        for (int i = 0; i < 4; i++) {
            double k = 4.0D + i * 3.0D;
            double lado = Math.sin(p.tickCount * 0.05D + i) * 1.2D;
            Vector3d q = p.position().add(u.scale(k)).add(-u.z * lado, 1.2D + 0.3D * Math.sin(i + p.tickCount * 0.1D), u.x * lado);
            w.sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, q.x, q.y, q.z, 2, 0.08D, 0.08D, 0.08D, 0.004D);
        }
        if (p.tickCount % 60 == 0 && corrida == null) {
            p.displayClientMessage(new StringTextComponent("Las luces malas van hacia el " + rumbo(u) + " · "
                    + Math.round(dist / 100.0D) + " cuadras").withStyle(TextFormatting.AQUA), true);
        }
    }

    static String rumbo(Vector3d u) {
        double ang = Math.toDegrees(Math.atan2(u.x, -u.z));   // 0 = norte (-z), 90 = este (+x)
        String[] r = {"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};
        return r[Math.floorMod((int) Math.round(ang / 45.0D), 8)];
    }

    // ------------------------------------------------------------------ arranques

    /** Viene el Vizcacha a buscar al jugador. Devuelve el motivo si no se pudo. */
    @Nullable
    public static String visita(ServerPlayerEntity p) {
        if (corrida != null) {
            return "Ya hay alguien yendo a la Salamanca en este mundo.";
        }
        ServerWorld w = p.getLevel();
        if (w.dimension() != World.OVERWORLD) {
            return "El Vizcacha solo anda por el mundo de arriba.";
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        Vector3d mira = Vector3d.directionFromRotation(0.0F, p.yRot);
        BlockPos lugar = null;
        for (int intento = 0; intento < 24 && lugar == null; intento++) {
            double ang = Math.atan2(mira.z, mira.x) + (RND.nextDouble() - 0.5D) * 2.2D;
            double dist = intento < 16 ? 30 + RND.nextInt(10) : 10 + RND.nextInt(6);
            lugar = suelo(w, p.getX() + Math.cos(ang) * dist, p.getZ() + Math.sin(ang) * dist, p.getY());
        }
        if (lugar == null) {
            lugar = p.blockPosition().offset(3, 0, 3);
        }
        JugadorData.set(p, DIA, (int) (w.getDayTime() / 24000L));   // una visita por día, pase lo que pase
        Corrida c = new Corrida(p.getUUID(), Fase.LLEGA);
        // el del Vizcacha va en pelo (sin montura: así lo lleva su propio paso); el tordillo del jugador, ensillado
        HorseEntity hg = caballo(w, lugar, 0, null, false);
        HorseEntity hj = caballo(w, lugar.offset(1, 0, 1), 5, null, true);
        GauchoEntity g = gaucho(w, GauchoEntity.Rol.VIZCACHA, Vector3d.atBottomCenterOf(lugar), true);
        if (hg == null || hj == null || g == null) {
            return "No se pudo hacer aparecer al Vizcacha.";
        }
        c.guia = g.getUUID();
        c.caballoGuia = hg.getUUID();
        c.caballoJugador = hj.getUUID();
        corrida = c;
        g.startRiding(hg, true);
        hj.setLeashedTo(hg, true);
        TrucoYa.LOGGER.info("Truco Ya: el Viejo Vizcacha viene a buscar a {} (La Salamanca)", p.getGameProfile().getName());
        return null;
    }

    /** Otra noche en el Paso, con el farol: el Vizcacha ya está esperando en la puerta. */
    private static void revancha(ServerPlayerEntity p, BlockPos o) {
        ServerWorld w = p.getLevel();
        Corrida c = new Corrida(p.getUUID(), Fase.LLEGADA);
        c.origen = o;
        Vector3d pie = SalamancaSitio.centro(o, SalamancaSitio.LLEGADA).add(1.5D, 0, 1.0D);
        GauchoEntity g = gaucho(w, GauchoEntity.Rol.VIZCACHA, pie, true);
        if (g == null) {
            return;
        }
        c.guia = g.getUUID();
        corrida = c;
        tormenta(w, true);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.teleportTo(w, pie.x - 2.0D, pie.y, pie.z - 1.0D, 180.0F, 0.0F);
        g.say("Sabía que ibas a volver, gurí. El Negro nos espera.");
    }

    /** Para probar (/truco salamanca partida): directo a la llegada al Paso, de noche. */
    @Nullable
    public static String irAlPaso(ServerPlayerEntity p) {
        if (corrida != null) {
            return "Ya hay una Salamanca en curso.";
        }
        ServerWorld w = p.getLevel();
        BlockPos o = SalamancaSitio.origen(w);
        if (o == null) {
            return "No se encontró el Paso de las Ánimas en este mundo.";
        }
        if (etapa(p) < ABIERTA) {
            JugadorData.set(p, ETAPA, ABIERTA);
        }
        long horaAhora = w.getDayTime() % 24000L;
        if (horaAhora < 18000L) {
            ponerHora(w, w.getDayTime() - horaAhora + 18000L);
        }
        revancha(p, o);
        return null;
    }

    public static void cancelar(String motivo) {
        abortar(motivo);
    }

    // ------------------------------------------------------------------ ciclo

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerWorld w = server.getLevel(World.OVERWORLD);
        if (w == null) {
            return;
        }
        Corrida c = corrida;
        if (c == null) {
            if (revisarArmada) {
                revisarArmada = false;
                SalamancaSitio.arreglarSiQuedoArmada(w);
            }
            if (server.getTickCount() % 100 == 0) {
                revisarVisitas(w);
            }
            return;
        }
        // la noche de la Salamanca no se termina mientras se juega
        if ((c.fase == Fase.ESPERA_MESA || c.fase == Fase.PARTIDA) && server.getTickCount() % 20 == 0) {
            long hora = w.getDayTime() % 24000L;
            if (hora > 21000L || hora < 13000L) {
                ponerHora(w, w.getDayTime() - hora + 21000L);   // siempre hacia adelante: el día no vuelve atrás
            }
            if (c.origen != null && RND.nextInt(25) == 0) {
                Vector3d m = SalamancaSitio.centro(c.origen, SalamancaSitio.MESA);
                BlockPos r = suelo(w, m.x + (RND.nextBoolean() ? 1 : -1) * (25 + RND.nextInt(25)),
                        m.z + (RND.nextBoolean() ? 1 : -1) * (25 + RND.nextInt(25)), m.y);
                if (r != null) rayo(w, r);
            }
        }
        ServerPlayerEntity p = server.getPlayerList().getPlayer(c.jugador);
        if (p == null || !p.isAlive() || p.level != w) {
            abortar("el jugador ya no está");
            return;
        }
        try {
            paso(w, p, c);
        } catch (RuntimeException e) {
            TrucoYa.LOGGER.error("Truco Ya: la Salamanca falló y se cancela (el mundo queda como estaba)", e);
            abortar("error");
            return;
        }
        if (corrida == c) {
            c.t++;
            if (c.congelar != null && !p.isPassenger() && p.position().distanceToSqr(c.congelar) > 0.09D) {
                p.teleportTo(c.congelar.x, c.congelar.y, c.congelar.z);
                p.setDeltaMovement(Vector3d.ZERO);
            }
        }
    }

    private static void revisarVisitas(ServerWorld w) {
        long hora = w.getDayTime() % 24000L;
        if (hora < 11600L || hora > 12900L) {
            return;
        }
        int dia = (int) (w.getDayTime() / 24000L);
        for (ServerPlayerEntity p : w.players()) {
            if (etapa(p) != ESPERA || JugadorData.get(p, DIA) >= dia || !p.isAlive() || p.isSpectator()
                    || p.isSleeping() || CinematicaServidor.enCurso(p)
                    || p.getVehicle() instanceof com.trucoya.entity.SeatEntity
                    || !w.canSeeSky(p.blockPosition().above())) {
                continue;   // bajo techo o en una cueva: el Vizcacha viene otro atardecer
            }
            if (visita(p) == null) {
                return;
            }
        }
    }

    private static void cambiar(Corrida c, Fase f) {
        c.fase = f;
        c.t = -1;   // el tick de este mismo ciclo lo deja en 0
        c.congelar = null;
    }

    private static void paso(ServerWorld w, ServerPlayerEntity p, Corrida c) {
        GauchoEntity g = gaucho(w, c.guia);
        switch (c.fase) {
            case LLEGA: llega(w, p, c, g); break;
            case VISITA: visitaEscena(w, p, c, g); break;
            case MONTAR: montar(w, p, c, g); break;
            case CABALGATA: cabalgata(w, p, c, g); break;
            case VIAJE: viaje(w, p, c, g); break;
            case LLEGADA: llegada(w, p, c, g); break;
            case ESPERA_MESA: esperaMesa(w, p, c, g); break;
            case PARTIDA:
                if (c.esperaFin >= 0 && c.t >= c.esperaFin) {
                    cambiar(c, c.ganaron ? Fase.AMANECER : Fase.DERROTA);
                }
                break;
            case AMANECER: amanecer(w, p, c, g); break;
            case SALIDA: salida(w, p, c, g); break;
            case DERROTA: derrota(w, p, c, g); break;
            default: break;
        }
    }

    // ---- 1. llega a caballo

    private static void llega(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        Entity hg = w.getEntity(c.caballoGuia);
        if (g == null || !(hg instanceof AbstractHorseEntity)) {
            abortar("el Vizcacha se perdió en el camino");
            return;
        }
        AbstractHorseEntity caballo = (AbstractHorseEntity) hg;
        double d = caballo.distanceTo(p);
        if (c.t % 10 == 0) {
            caballo.getNavigation().moveTo(p, 1.7D);
        }
        if (c.t == 60) {
            p.displayClientMessage(new StringTextComponent("Se acerca un jinete al galope...").withStyle(TextFormatting.GRAY), true);
            w.playSound(null, caballo.blockPosition(), SoundEvents.HORSE_GALLOP, SoundCategory.NEUTRAL, 1.0F, 0.9F);
        }
        if (d < 7.0D || c.t > 20 * 35) {
            if (d >= 7.0D) {
                BlockPos ac = cercaDe(w, p);
                caballo.teleportTo(ac.getX() + 0.5D, ac.getY(), ac.getZ() + 0.5D);
                Entity hj = w.getEntity(c.caballoJugador);
                if (hj != null) hj.teleportTo(ac.getX() + 1.5D, ac.getY(), ac.getZ() + 0.5D);
            }
            caballo.getNavigation().stop();
            cambiar(c, Fase.VISITA);
        }
    }

    // ---- 2. la visita (media cinemática)

    private static void visitaEscena(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        Escenas.Info e = Escenas.de(Escenas.VISITA);
        if (g == null) {
            abortar("el Vizcacha se perdió");
            return;
        }
        if (c.t == 0) {
            if (p.isPassenger()) p.stopRiding();
            c.congelar = p.position();
            escena(p, Escenas.VISITA, g, p.blockPosition(), e);
        }
        if (c.congelar == null) c.congelar = p.position();
        int bajarse = e.tick("bajarse", 1.5D);
        if (c.t == bajarse) {
            g.stopRiding();
        }
        if (c.t > bajarse) {
            Vector3d hacia = g.position().subtract(p.position());
            Vector3d plano = new Vector3d(hacia.x, 0, hacia.z);
            Vector3d u = plano.lengthSqr() < 0.01D ? new Vector3d(0, 0, 1) : plano.normalize();
            Vector3d meta = p.position().add(u.scale(2.4D));
            if (g.position().distanceToSqr(meta) > 0.6D) {
                andar(g, meta, 0.45D);
            } else {
                quieto(g, p.getEyePosition(1.0F));
            }
        }
        if (c.t >= e.ticks()) {
            dar(p, new ItemStack(ModItems.FAROL_ANIMAS.get()), true);
            JugadorData.set(p, ETAPA, Math.max(ABIERTA, etapa(p)));
            Entity hj = w.getEntity(c.caballoJugador);
            if (hj instanceof AbstractHorseEntity) {
                ((AbstractHorseEntity) hj).dropLeash(true, false);
                ((AbstractHorseEntity) hj).tameWithName(p);
            }
            Entity hg = w.getEntity(c.caballoGuia);
            g.setNoAi(false);
            if (hg != null) g.startRiding(hg, true);
            CinematicaServidor.liberar(p);
            cambiar(c, Fase.MONTAR);
        }
    }

    // ---- 3. subirse al caballo

    private static void montar(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        if (p.getVehicle() instanceof AbstractHorseEntity) {
            cambiar(c, Fase.CABALGATA);
            return;
        }
        if (c.t % 40 == 0) {
            p.displayClientMessage(new StringTextComponent("Subite al tordillo: el Vizcacha te espera.")
                    .withStyle(TextFormatting.GOLD), true);
        }
        if (c.t > 20 * 15) {
            Entity hj = w.getEntity(c.caballoJugador);
            if (!(hj instanceof AbstractHorseEntity) || hj.distanceTo(p) > 12.0D) {
                BlockPos ac = suelo(w, p.getX() + 1.5D, p.getZ() + 1.5D, p.getY());
                hj = caballo(w, ac != null ? ac : p.blockPosition(), 5, p, true);
                if (hj != null) c.caballoJugador = hj.getUUID();
            }
            if (hj != null) {
                if (g != null) g.say("Dale, que te ayudo a subir.");
                p.startRiding(hj, true);
            }
        }
    }

    // ---- 4. la cabalgata, con la tormenta

    private static void cabalgata(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        if (c.t == 0) {
            if (c.origen == null) {
                c.origen = SalamancaSitio.origen(w);
            }
            if (c.origen == null) {
                if (g != null) g.say("Pucha... no encuentro el camino. Otra noche será.");
                abortar("no se encontró el Paso de las Ánimas");
                return;
            }
            tormenta(w, true);
            long hora = w.getDayTime() % 24000L;
            if (hora < 13000L) {
                ponerHora(w, w.getDayTime() - hora + 13000L);
            }
            if (g != null && !c.viajeHecho) g.say("Seguime, gurí. Y no te asustes de los rayos.");
        }
        Entity hgE = w.getEntity(c.caballoGuia);
        if (g == null || !(hgE instanceof AbstractHorseEntity)) {
            // sin el Vizcacha a caballo: se lo hace aparecer de nuevo al lado
            if (g == null) {
                abortar("el Vizcacha se perdió");
                return;
            }
            HorseEntity nuevo = caballo(w, g.blockPosition(), 0, null, false);
            if (nuevo == null) {
                abortar("sin caballo");
                return;
            }
            c.caballoGuia = nuevo.getUUID();
            g.startRiding(nuevo, true);
            return;
        }
        AbstractHorseEntity hg = (AbstractHorseEntity) hgE;
        if (g.getVehicle() != hg) g.startRiding(hg, true);
        if (c.t > 20 * 60 * 8) {
            g.say("Se hace tarde, gurí. Te espero en el Paso: seguí las luces malas con el farol.");
            p.sendMessage(new StringTextComponent("El Vizcacha siguió solo. Andá de noche al Paso de las Ánimas con el farol.")
                    .withStyle(TextFormatting.GRAY), p.getUUID());
            abortar("la cabalgata se hizo muy larga");
            return;
        }
        Vector3d destino = SalamancaSitio.centro(c.origen, SalamancaSitio.LLEGADA);
        if (!c.viajeHecho && c.t == 20 * 40 && horizontal(p.position(), destino) > 360.0D) {
            cambiar(c, Fase.VIAJE);
            return;
        }
        // rayos y luces malas
        if (c.t >= c.proximoRayo) {
            double ang = RND.nextDouble() * Math.PI * 2;
            double dist = 18 + RND.nextInt(34);
            BlockPos r = suelo(w, p.getX() + Math.cos(ang) * dist, p.getZ() + Math.sin(ang) * dist, p.getY());
            if (r != null) rayo(w, r);
            c.proximoRayo = c.t + 50 + RND.nextInt(110);
        }
        if (c.t % 20 == 0) {
            lucesMalas(w, p, g.position(), destino);
        }
        double dp = g.distanceTo(p);
        if (c.t % 10 == 0) {
            if (dp > 90.0D) {
                Vector3d u = unidad(destino.subtract(p.position()));
                BlockPos ac = suelo(w, p.getX() + u.x * 8, p.getZ() + u.z * 8, p.getY());
                if (ac != null) hg.teleportTo(ac.getX() + 0.5D, ac.getY(), ac.getZ() + 0.5D);
            } else if (dp > 24.0D) {
                hg.getNavigation().stop();
                g.getLookControl().setLookAt(p, 30, 30);
                if (c.esperandoDesde < 0) c.esperandoDesde = c.t;
                if ((c.t - c.esperandoDesde) % 200 == 100) {
                    g.say(RND.nextBoolean() ? "¡Dale, gurí, que nos agarra el agua!" : "¡Arriá ese pingo, que no llegamos!");
                }
            } else {
                c.esperandoDesde = -1;
                Vector3d meta = siguiente(w, g.position(), destino);
                hg.getNavigation().moveTo(meta.x, meta.y, meta.z, 1.8D);
            }
        }
        // trabado (un río, un barranco): salta al próximo tramo
        if (c.t % 60 == 0) {
            if (c.esperandoDesde < 0 && c.t > 0 && hg.position().distanceTo(c.ultimoLugar) < 2.0D
                    && horizontal(hg.position(), destino) > 12.0D) {
                Vector3d meta = siguiente(w, hg.position(), destino);
                if (w.isLoaded(new BlockPos(meta))) hg.teleportTo(meta.x, meta.y, meta.z);
            }
            c.ultimoLugar = hg.position();
        }
        if (horizontal(g.position(), destino) < 10.0D && horizontal(p.position(), destino) < 26.0D) {
            cambiar(c, Fase.LLEGADA);
        }
    }

    // ---- 4b. "Esa noche...": el salto al último tramo cuando el Paso queda lejos

    private static void viaje(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        Escenas.Info e = Escenas.de(Escenas.VIAJE);
        if (c.t == 0) {
            escena(p, Escenas.VIAJE, g, c.origen, e);
        }
        int salto = e.tick("salto", 3.0D);
        Vector3d destino = SalamancaSitio.centro(c.origen, SalamancaSitio.LLEGADA);
        if (c.t == salto) {
            Vector3d u = unidad(p.position().subtract(destino));
            if (u.lengthSqr() < 0.01D) u = new Vector3d(0, 0, -1);
            BlockPos lugar = null;
            for (int k = 0; k < 12 && lugar == null; k++) {
                double giro = (k % 2 == 0 ? 1 : -1) * (k / 2) * 0.35D;
                double cx = Math.cos(giro), sx = Math.sin(giro);
                Vector3d v = new Vector3d(u.x * cx - u.z * sx, 0, u.x * sx + u.z * cx);
                Vector3d q = destino.add(v.scale(55.0D));
                w.getChunk((int) Math.floor(q.x) >> 4, (int) Math.floor(q.z) >> 4);
                lugar = suelo(w, q.x, q.z, destino.y);
            }
            if (lugar == null) lugar = new BlockPos(destino).north(30);
            Entity hj = p.getVehicle();
            c.montadoViaje = hj != null ? hj.getUUID() : null;
            p.stopRiding();
            Vector3d al = Vector3d.atBottomCenterOf(lugar);
            float yaw = (float) (MathHelper.atan2(destino.z - al.z, destino.x - al.x) * (180D / Math.PI)) - 90.0F;
            if (hj != null) hj.teleportTo(al.x, al.y, al.z);
            p.teleportTo(w, al.x, al.y, al.z, yaw, 0.0F);
            Entity hg = w.getEntity(c.caballoGuia);
            Vector3d adelante = unidad(destino.subtract(al)).scale(5.0D);
            BlockPos lg = suelo(w, al.x + adelante.x, al.z + adelante.z, al.y);
            if (hg != null && lg != null) hg.teleportTo(lg.getX() + 0.5D, lg.getY(), lg.getZ() + 0.5D);
            long hora = w.getDayTime() % 24000L;
            if (hora < 18000L) {
                ponerHora(w, w.getDayTime() - hora + 18000L);
            }
        }
        if (c.t == salto + 8) {
            Entity hj = w.getEntity(c.montadoViaje != null ? c.montadoViaje : c.caballoJugador);
            if (hj != null && !p.isPassenger() && hj.distanceTo(p) < 6.0D) {
                p.startRiding(hj, true);
            }
        }
        if (c.t >= e.ticks()) {
            c.viajeHecho = true;
            cambiar(c, Fase.CABALGATA);
            c.t = 1;   // sigue sin volver a arrancar la tormenta
        }
    }

    // ---- 5. la llegada: la Salamanca aparece entre rayos

    private static void llegada(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        Escenas.Info e = Escenas.de(Escenas.LLEGADA);
        BlockPos o = c.origen;
        if (o == null || g == null) {
            abortar("se perdió el camino");
            return;
        }
        if (c.t == 0) {
            // todos afuera, en el sendero (nada puede quedar donde se va a levantar la Salamanca)
            Entity montura = p.getVehicle();
            if (p.isPassenger()) p.stopRiding();
            Entity hg = g.getVehicle();
            if (g.isPassenger()) g.stopRiding();
            Vector3d pie = SalamancaSitio.centro(o, SalamancaSitio.LLEGADA);
            p.teleportTo(w, pie.x, pie.y, pie.z, 0.0F, 0.0F);
            g.teleportTo(pie.x + 1.4D, pie.y, pie.z + 1.2D);
            if (montura != null) montura.teleportTo(pie.x - 3.0D, pie.y, pie.z - 1.0D);
            if (hg != null) hg.teleportTo(pie.x + 4.0D, pie.y, pie.z - 1.0D);
            Entity hj = w.getEntity(c.caballoJugador);
            if (hj != null && hj != montura) hj.teleportTo(pie.x - 4.5D, pie.y, pie.z - 2.0D);
            tormenta(w, true);
            long hora = w.getDayTime() % 24000L;
            if (hora < 13000L || hora > 21000L) {
                ponerHora(w, w.getDayTime() - hora + (hora > 21000L ? 24000L : 0L) + 18000L);
            }
            c.congelar = pie;
            escena(p, Escenas.LLEGADA, g, o, e);
            quieto(g, SalamancaSitio.centro(o, SalamancaSitio.PUERTA).add(0, 1.6D, 0));
        }
        if (c.t == e.tick("rayo1", 3.0D)) {
            rayo(w, SalamancaSitio.mundo(o, SalamancaSitio.OMBU));
        }
        if (c.t == e.tick("rayo2", 4.4D)) {
            rayo(w, SalamancaSitio.mundo(o, new BlockPos(26, 3, 9)));
        }
        if (c.t == e.tick("salamanca", 5.0D)) {
            rayo(w, SalamancaSitio.mundo(o, new BlockPos(17, 3, 10)));
            if (SalamancaSitio.poner(w, o, true)) {
                c.nocheArmada = true;
            }
            Vector3d centro = SalamancaSitio.centro(o, SalamancaSitio.MESA);
            w.sendParticles(ParticleTypes.LARGE_SMOKE, centro.x, centro.y + 2, centro.z, 80, 6.0D, 2.5D, 5.0D, 0.02D);
            w.sendParticles(ParticleTypes.SOUL, centro.x, centro.y + 1, centro.z, 60, 6.0D, 2.0D, 5.0D, 0.03D);
            w.playSound(null, new BlockPos(centro), SoundEvents.WITHER_SPAWN, SoundCategory.HOSTILE, 0.55F, 0.6F);
            GauchoEntity m = gaucho(w, GauchoEntity.Rol.MANDINGA,
                    SalamancaSitio.centro(o, SalamancaSitio.SILLA_E).add(0.9D, 0, 0), false);
            GauchoEntity l = gaucho(w, GauchoEntity.Rol.LEDESMA,
                    SalamancaSitio.centro(o, SalamancaSitio.SILLA_O).add(-0.9D, 0, 0), false);
            BlockPos mesa = SalamancaSitio.mundo(o, SalamancaSitio.MESA);
            if (m != null) {
                c.mandinga = m.getUUID();
                m.goSit(SalamancaSitio.mundo(o, SalamancaSitio.SILLA_E), mesa);
            }
            if (l != null) {
                c.ledesma = l.getUUID();
                l.goSit(SalamancaSitio.mundo(o, SalamancaSitio.SILLA_O), mesa);
            }
            Logros.award(p, Logros.LA_SALAMANCA);
        }
        if (c.t == e.tick("entra", 6.5D)) {
            g.setNoAi(false);
            g.goSit(SalamancaSitio.mundo(o, SalamancaSitio.SILLA_S), SalamancaSitio.mundo(o, SalamancaSitio.MESA));
        }
        if (c.t >= e.ticks()) {
            CinematicaServidor.liberar(p);
            p.displayClientMessage(new StringTextComponent("Sentate a la mesa, frente al Vizcacha.")
                    .withStyle(TextFormatting.GOLD), true);
            cambiar(c, Fase.ESPERA_MESA);
        }
    }

    // ---- 6. esperando que el jugador se siente

    private static void esperaMesa(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        BlockPos o = c.origen;
        if (o == null) {
            abortar("sin Paso");
            return;
        }
        if (c.t % 40 == 0) {
            BlockPos mesa = SalamancaSitio.mundo(o, SalamancaSitio.MESA);
            sentar(gaucho(w, c.mandinga), SalamancaSitio.mundo(o, SalamancaSitio.SILLA_E), mesa);
            sentar(gaucho(w, c.ledesma), SalamancaSitio.mundo(o, SalamancaSitio.SILLA_O), mesa);
            sentar(g, SalamancaSitio.mundo(o, SalamancaSitio.SILLA_S), mesa);
            // los cuatro sentados: se reparte
            net.minecraft.tileentity.TileEntity te = w.getBlockEntity(mesa);
            GauchoEntity m = gaucho(w, c.mandinga), l = gaucho(w, c.ledesma);
            if (te instanceof com.trucoya.tileentity.MesaTrucoTileEntity && m != null && l != null && g != null
                    && m.isPassenger() && l.isPassenger() && g.isPassenger()
                    && ((com.trucoya.tileentity.MesaTrucoTileEntity) te).isSeatedHere(p)) {
                ((com.trucoya.tileentity.MesaTrucoTileEntity) te).requestStart(p, 4, 30, true);
            }
        }
        if (c.t > 0 && c.t % (20 * 60) == 0) {
            p.displayClientMessage(new StringTextComponent("La Salamanca espera: sentate a la mesa, frente al Vizcacha.")
                    .withStyle(TextFormatting.GOLD), true);
        }
        if (c.t > 20 * 60 * 10 || horizontal(p.position(), SalamancaSitio.centro(o, SalamancaSitio.MESA)) > 120.0D) {
            GauchoEntity m = gaucho(w, c.mandinga);
            if (m != null) m.say("¿Te vas? Volvé cuando tengas coraje.");
            cambiar(c, Fase.DERROTA);
        }
    }

    private static void sentar(@Nullable GauchoEntity g, BlockPos silla, BlockPos mesa) {
        if (g != null && !g.isPassenger() && !g.yendoASentarse()) {
            g.setNoAi(false);
            g.goSit(silla, mesa);
        }
    }

    // ---- 7. el amanecer: Ledesma queda libre

    private static void amanecer(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        Escenas.Info e = Escenas.de(Escenas.AMANECER);
        BlockPos o = c.origen;
        GauchoEntity m = gaucho(w, c.mandinga);
        GauchoEntity l = gaucho(w, c.ledesma);
        if (o == null) {
            abortar("sin Paso");
            return;
        }
        Vector3d pieV = SalamancaSitio.mundo(o, SalamancaSitio.PIE_VIZCACHA);
        Vector3d pieL = SalamancaSitio.mundo(o, SalamancaSitio.PIE_LEDESMA);
        if (c.t == 0) {
            if (p.isPassenger()) p.stopRiding();
            Vector3d pj = SalamancaSitio.mundo(o, SalamancaSitio.PIE_JUGADOR);
            p.teleportTo(w, pj.x, pj.y, pj.z, 0.0F, 0.0F);
            c.congelar = pj;
            if (g != null) {
                g.standUp();
                g.teleportTo(pieV.x, pieV.y, pieV.z);
            }
            if (l != null) l.standUp();
            tormenta(w, false);
            long hora = w.getDayTime() % 24000L;
            if (hora < 22600L) {
                ponerHora(w, w.getDayTime() - hora + 22600L);
            }
            escena(p, Escenas.AMANECER, g, o, e);
        }
        // el Mandinga se esfuma en humo y fuego
        int ma = e.tick("mandinga_a", 3.0D), mb = e.tick("mandinga_b", 6.0D);
        if (m != null && c.t >= ma) {
            float f = MathHelper.clamp((c.t - ma) / (float) Math.max(1, mb - ma), 0.0F, 1.0F);
            m.setDesvanecido(f);
            if (c.t % 3 == 0) {
                w.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + 1.0D, m.getZ(), 6, 0.3D, 0.6D, 0.3D, 0.01D);
                w.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1.2D, m.getZ(), 4, 0.3D, 0.6D, 0.3D, 0.01D);
            }
            if (c.t == ma) {
                w.playSound(null, m.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundCategory.HOSTILE, 0.8F, 0.5F);
            }
            if (c.t >= mb) {
                w.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1.0D, m.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.04D);
                w.playSound(null, m.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1.0F, 0.6F);
                m.remove();
                c.mandinga = null;
            }
        }
        // Ledesma se levanta y va hasta el Vizcacha
        int camina = e.tick("ledesma_camina", 5.5D);
        if (l != null) {
            if (c.t == camina) {
                l.standUp();
                andar(l, pieL, 0.35D);
            } else if (c.t > camina && l.position().distanceToSqr(pieL) < 1.0D) {
                quieto(l, g != null ? g.getEyePosition(1.0F) : pieV.add(0, 1.6D, 0));
            }
        }
        if (g != null && c.t > 5) {
            quieto(g, l != null ? l.getEyePosition(1.0F) : p.getEyePosition(1.0F));
        }
        // y se va con el sol
        int la = e.tick("ledesma_a", 31.0D), lb = e.tick("ledesma_b", 35.0D);
        if (l != null && c.t >= la) {
            l.setDesvanecido(MathHelper.clamp((c.t - la) / (float) Math.max(1, lb - la), 0.0F, 1.0F));
            if (c.t % 4 == 0) {
                w.sendParticles(ParticleTypes.SOUL, l.getX(), l.getY() + 1.0D, l.getZ(), 3, 0.3D, 0.7D, 0.3D, 0.02D);
            }
            if (c.t >= lb) {
                w.sendParticles(ParticleTypes.SOUL, l.getX(), l.getY() + 1.0D, l.getZ(), 30, 0.3D, 1.0D, 0.3D, 0.05D);
                w.playSound(null, l.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                l.remove();
                c.ledesma = null;
            }
        }
        if (c.t == e.tick("ruina", 33.0D) && c.nocheArmada) {
            SalamancaSitio.poner(w, o, false);
            SalamancaSitio.flores(w, o);
            c.nocheArmada = false;
        }
        if (c.t == e.tick("facon", 37.0D)) {
            dar(p, new ItemStack(ModItems.FACON_LEDESMA.get()), false);
            p.sendMessage(new StringTextComponent("El Viejo Vizcacha te dio el Facón de Ledesma.").withStyle(TextFormatting.GOLD),
                    p.getUUID());
            JugadorData.set(p, ETAPA, LIBRE);
            JugadorData.addPrestigio(p, 150, "El alma del Negro Ledesma es libre");
            Logros.award(p, Logros.ALMA_LIBRE);
            p.sendMessage(new StringTextComponent("En el suelo, donde estaba la mesa, quedó la baraja del Mandinga.")
                    .withStyle(TextFormatting.DARK_RED), p.getUUID());
            com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.MANDINGA);
        }
        if (c.t >= e.ticks()) {
            CinematicaServidor.liberar(p);
            cambiar(c, Fase.SALIDA);
        }
    }

    // ---- 8. el Vizcacha se va

    private static void salida(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        if (c.t == 0) {
            p.sendMessage(new StringTextComponent("El tordillo que te trajo el Vizcacha es tuyo.").withStyle(TextFormatting.GRAY),
                    p.getUUID());
            if (g != null && c.origen != null) {
                g.say("Vamos, gurí. Te convido unos mates en la estancia.");
                andar(g, SalamancaSitio.centro(c.origen, new BlockPos(17, 3, -10)), 0.45D);
            }
        }
        if (g != null && c.t % 20 == 0 && c.origen != null) {
            andar(g, SalamancaSitio.centro(c.origen, new BlockPos(17, 3, -10)), 0.45D);
        }
        if (c.t >= 20 * 9) {
            if (g != null) {
                w.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 1, g.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.01D);
            }
            terminar(w);
        }
    }

    // ---- 7b. la derrota

    private static void derrota(ServerWorld w, ServerPlayerEntity p, Corrida c, @Nullable GauchoEntity g) {
        Escenas.Info e = Escenas.de(Escenas.DERROTA);
        BlockPos o = c.origen;
        if (c.t == 0) {
            if (p.isPassenger()) p.stopRiding();
            c.congelar = p.position();
            if (g != null) g.standUp();
            GauchoEntity l = gaucho(w, c.ledesma);
            if (l != null) l.standUp();
            escena(p, Escenas.DERROTA, g, o != null ? o : p.blockPosition(), e);
        }
        if (c.t == e.tick("ruina", 10.5D)) {
            if (o != null && c.nocheArmada) {
                SalamancaSitio.poner(w, o, false);
                c.nocheArmada = false;
            }
            quitar(w, c.mandinga);
            quitar(w, c.ledesma);
            c.mandinga = c.ledesma = null;
            tormenta(w, false);
            long hora = w.getDayTime() % 24000L;
            if (hora > 12000L) {
                ponerHora(w, w.getDayTime() - hora + 24000L + 300L);   // la mañana siguiente
            }
            if (o != null) {
                Vector3d pie = SalamancaSitio.centro(o, SalamancaSitio.LLEGADA);
                p.teleportTo(w, pie.x, pie.y, pie.z, 180.0F, 0.0F);
                c.congelar = pie;
            }
        }
        if (c.t >= e.ticks()) {
            p.sendMessage(new StringTextComponent("El Vizcacha te espera en el Paso de las Ánimas. Volvé otra noche, con el farol.")
                    .withStyle(TextFormatting.GRAY), p.getUUID());
            terminar(w);
        }
    }

    // ------------------------------------------------------------------ fin y cancelación

    private static void terminar(ServerWorld w) {
        Corrida c = corrida;
        if (c == null) {
            return;
        }
        quitar(w, c.guia);
        quitar(w, c.mandinga);
        quitar(w, c.ledesma);
        quitar(w, c.caballoGuia);
        ServerPlayerEntity p = w.getServer().getPlayerList().getPlayer(c.jugador);
        if (p != null) CinematicaServidor.liberar(p);
        corrida = null;
    }

    private static void abortar(String motivo) {
        Corrida c = corrida;
        if (c == null) {
            return;
        }
        TrucoYa.LOGGER.info("Truco Ya: la Salamanca se cancela ({})", motivo);
        MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        ServerWorld w = server != null ? server.getLevel(World.OVERWORLD) : null;
        if (w != null) {
            if (c.nocheArmada && c.origen != null) {
                SalamancaSitio.poner(w, c.origen, false);
            }
            tormenta(w, false);
            Entity hj = w.getEntity(c.caballoJugador);
            if (hj instanceof AbstractHorseEntity && ((AbstractHorseEntity) hj).getOwnerUUID() == null) {
                hj.remove();
            }
            ServerPlayerEntity p = w.getServer().getPlayerList().getPlayer(c.jugador);
            if (p != null) {
                TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new EscenaPacket(-1, -1, BlockPos.ZERO));
            }
            terminar(w);
        }
        corrida = null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Corrida c = corrida;
        if (c != null && event.getPlayer().getUUID().equals(c.jugador)) {
            abortar("el jugador se fue");
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Corrida c = corrida;
        if (c != null && event.getEntityLiving() instanceof ServerPlayerEntity
                && event.getEntityLiving().getUUID().equals(c.jugador)) {
            abortar("el jugador murió");
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Corrida c = corrida;
        if (c != null && event.getPlayer().getUUID().equals(c.jugador)) {
            abortar("el jugador cambió de mundo");
        }
    }

    @SubscribeEvent
    public static void onStopping(FMLServerStoppingEvent event) {
        abortar("se cierra el servidor");
        tormentaPuesta = false;
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld && ((ServerWorld) event.getWorld()).dimension() == World.OVERWORLD) {
            revisarArmada = true;   // en el primer tick, con el mundo ya andando
        }
    }

    /** Las primeras veces que alguien entra después de ver el final (mundos de antes): queda esperando la visita. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) event.getPlayer();
            if (etapa(p) == NADA && JugadorData.get(p, Historia.FINAL_VISTO) != 0) {
                JugadorData.set(p, ETAPA, ESPERA);
                JugadorData.set(p, DIA, (int) (p.getLevel().getDayTime() / 24000L) - 1);
            }
        }
    }

    /** En plena Salamanca no se duerme (la noche no se saltea). */
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (protegido(event.getPlayer())) {
            event.setResult(PlayerEntity.SleepResult.OTHER_PROBLEM);
            event.getPlayer().displayClientMessage(new StringTextComponent("Esta noche no se duerme, gurí."), true);
        }
    }

    /** Mientras dura, no aparecen monstruos cerca del jugador ni del Paso. */
    @SubscribeEvent
    public static void onSpawn(LivingSpawnEvent.CheckSpawn event) {
        Corrida c = corrida;
        if (c == null || !(event.getEntityLiving() instanceof IMob) || !(event.getWorld() instanceof ServerWorld)) {
            return;
        }
        ServerWorld w = (ServerWorld) event.getWorld();
        ServerPlayerEntity p = w.getServer().getPlayerList().getPlayer(c.jugador);
        Vector3d donde = new Vector3d(event.getX(), event.getY(), event.getZ());
        boolean cerca = p != null && p.position().distanceToSqr(donde) < 40 * 40;
        if (!cerca && c.origen != null) {
            cerca = SalamancaSitio.centro(c.origen, SalamancaSitio.MESA).distanceToSqr(donde) < 48 * 48;
        }
        if (cerca) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ------------------------------------------------------------------ ayudas

    private static void escena(ServerPlayerEntity p, int tipo, @Nullable GauchoEntity g, BlockPos origen, Escenas.Info e) {
        CinematicaServidor.proteger(p, e.ticks() + 20 * 10);
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new EscenaPacket(tipo, g == null ? -1 : g.getId(), origen));
    }

    /** Si se puede tocar el cielo de este mundo (ver TrucoConfig: en un servidor, solo si nadie más lo sufre). */
    static boolean cieloLibre(ServerWorld w) {
        switch (com.trucoya.TrucoConfig.SALAMANCA_CIELO.get()) {
            case NUNCA:
                return false;
            case SIEMPRE:
                return true;
            default:
                Corrida c = corrida;
                ServerPlayerEntity p = c == null ? null : w.getServer().getPlayerList().getPlayer(c.jugador);
                for (ServerPlayerEntity o : w.players()) {
                    if (p == null || (o != p && o.distanceToSqr(p) > 128.0D * 128.0D)) {
                        return false;
                    }
                }
                return true;
        }
    }

    private static void ponerHora(ServerWorld w, long t) {
        if (t > w.getDayTime() - 24000L && cieloLibre(w)) {   // nunca se retrocede un día entero
            w.setDayTime(t);
        }
    }

    /** El clima que había antes de la tormenta de la Salamanca (para dejarlo como estaba). */
    private static boolean tormentaPuesta;
    private static boolean llovia;
    private static boolean tronaba;

    private static void tormenta(ServerWorld w, boolean si) {
        if (si) {
            if (tormentaPuesta || !cieloLibre(w)) {
                return;
            }
            tormentaPuesta = true;
            llovia = w.isRaining();
            tronaba = w.isThundering();
            // lluvia sí, tormenta eléctrica de verdad no (sus rayos queman): los rayos los pone la historia
            w.setWeatherParameters(0, 20 * 60 * 20, true, false);
        } else if (tormentaPuesta) {
            tormentaPuesta = false;
            if (llovia) {
                w.setWeatherParameters(0, 20 * 60 * 5, true, tronaba);
            } else {
                w.setWeatherParameters(20 * 60 * 10, 0, false, false);
            }
        }
    }

    private static void rayo(ServerWorld w, BlockPos pos) {
        LightningBoltEntity b = EntityType.LIGHTNING_BOLT.create(w);
        if (b != null) {
            b.moveTo(Vector3d.atBottomCenterOf(pos));
            b.setVisualOnly(true);
            w.addFreshEntity(b);
        }
    }

    private static void lucesMalas(ServerWorld w, ServerPlayerEntity p, Vector3d desde, Vector3d destino) {
        Vector3d u = unidad(destino.subtract(desde));
        for (int i = 0; i < 5; i++) {
            double k = 6.0D + i * 4.0D + RND.nextDouble() * 2.0D;
            double lado = (RND.nextBoolean() ? 1 : -1) * (3.0D + RND.nextDouble() * 3.0D);
            double x = desde.x + u.x * k - u.z * lado;
            double z = desde.z + u.z * k + u.x * lado;
            BlockPos s = suelo(w, x, z, desde.y);
            double y = (s != null ? s.getY() : desde.y) + 1.0D + RND.nextDouble();
            w.sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, x, y, z, 3, 0.1D, 0.15D, 0.1D, 0.005D);
        }
    }

    /** Próximo tramo del camino: 16 bloques hacia el destino, esquivando el agua. */
    private static Vector3d siguiente(ServerWorld w, Vector3d desde, Vector3d destino) {
        Vector3d d = destino.subtract(desde);
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        if (dist < 18.0D) {
            return destino;
        }
        Vector3d u = new Vector3d(d.x / dist, 0, d.z / dist);
        for (double giro : new double[] {0, 0.5, -0.5, 1.0, -1.0, 1.5, -1.5}) {
            double cx = Math.cos(giro), sx = Math.sin(giro);
            Vector3d v = new Vector3d(u.x * cx - u.z * sx, 0, u.x * sx + u.z * cx);
            BlockPos s = suelo(w, desde.x + v.x * 16.0D, desde.z + v.z * 16.0D, desde.y);
            if (s != null) {
                return Vector3d.atBottomCenterOf(s);
            }
        }
        return desde.add(u.scale(16.0D));
    }

    /** Un lugar parado en tierra firme (sin agua) en (x, z), o null. */
    @Nullable
    private static BlockPos suelo(ServerWorld w, double x, double z, double cerca) {
        BlockPos col = new BlockPos(x, 0, z);
        if (!w.isLoaded(col)) {
            return null;
        }
        int y = w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ());
        BlockPos p = new BlockPos(col.getX(), y, col.getZ());
        if (!w.getFluidState(p.below()).isEmpty() || !w.getFluidState(p).isEmpty()) {
            return null;
        }
        if (!w.getBlockState(p.below()).getMaterial().isSolid() || Math.abs(y - cerca) > 40) {
            return null;
        }
        return p;
    }

    /** Un lugar libre a 3-5 bloques del jugador, a su misma altura (para cuando el caballo no encuentra el camino). */
    private static BlockPos cercaDe(ServerWorld w, ServerPlayerEntity p) {
        BlockPos base = p.blockPosition();
        for (int r = 3; r <= 5; r++) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos q = base.offset((int) Math.round(Math.cos(a) * r), dy, (int) Math.round(Math.sin(a) * r));
                    if (w.isEmptyBlock(q) && w.isEmptyBlock(q.above()) && w.isEmptyBlock(q.above(2))
                            && w.getBlockState(q.below()).getMaterial().isSolid() && w.getFluidState(q).isEmpty()) {
                        return q;
                    }
                }
            }
        }
        return base;
    }

    private static double horizontal(Vector3d a, Vector3d b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vector3d unidad(Vector3d v) {
        double l = Math.sqrt(v.x * v.x + v.z * v.z);
        return l < 1.0E-4D ? Vector3d.ZERO : new Vector3d(v.x / l, 0, v.z / l);
    }

    private static void andar(GauchoEntity g, Vector3d a, double velocidad) {
        g.setNoAi(false);
        if (g.isPassenger()) return;
        g.getNavigation().moveTo(a.x, a.y, a.z, velocidad);
    }

    /** Parado, mirando a un punto (sin que la IA lo mueva). */
    private static void quieto(GauchoEntity g, Vector3d mirar) {
        g.getNavigation().stop();
        g.setNoAi(true);
        double dx = mirar.x - g.getX(), dz = mirar.z - g.getZ();
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        g.yRot = yaw;
        g.yBodyRot = yaw;
        g.yHeadRot = yaw;
    }

    @Nullable
    private static GauchoEntity gaucho(ServerWorld w, @Nullable UUID id) {
        if (id == null) return null;
        Entity e = w.getEntity(id);
        return e instanceof GauchoEntity && e.isAlive() ? (GauchoEntity) e : null;
    }

    private static void quitar(ServerWorld w, @Nullable UUID id) {
        if (id == null) return;
        Entity e = w.getEntity(id);
        if (e != null) {
            e.ejectPassengers();
            e.stopRiding();
            e.remove();
        }
    }

    @Nullable
    private static GauchoEntity gaucho(ServerWorld w, GauchoEntity.Rol rol, Vector3d pos, boolean guia) {
        GauchoEntity g = ModEntities.GAUCHO.get().create(w);
        if (g == null) {
            return null;
        }
        g.setRol(rol);
        g.setVariante(0);
        g.guiaSalamanca = guia;
        g.moveTo(pos.x, pos.y, pos.z, RND.nextFloat() * 360.0F, 0.0F);
        g.finalizeSpawn(w, w.getCurrentDifficultyAt(new BlockPos(pos)), SpawnReason.EVENT, null, null);
        g.initDefaults();
        w.addFreshEntity(g);
        return g;
    }

    @Nullable
    private static HorseEntity caballo(ServerWorld w, BlockPos pos, int pelaje, @Nullable ServerPlayerEntity dueno,
                                       boolean ensillado) {
        HorseEntity h = EntityType.HORSE.create(w);
        if (h == null) {
            return null;
        }
        h.finalizeSpawn(w, w.getCurrentDifficultyAt(pos), SpawnReason.EVENT, null, null);
        CompoundNBT n = h.saveWithoutId(new CompoundNBT());
        n.putInt("Variant", pelaje);
        h.load(n);
        h.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, RND.nextFloat() * 360.0F, 0.0F);
        if (ensillado) {
            h.equipSaddle(null);
        }
        h.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0D);
        h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        if (dueno != null) {
            h.tameWithName(dueno);
        }
        w.addFreshEntity(h);
        return h;
    }

    private static void dar(ServerPlayerEntity p, ItemStack stack, boolean unico) {
        if (unico && p.inventory.contains(stack)) {
            return;
        }
        if (!p.inventory.add(stack)) {
            p.drop(stack, false);
        }
    }
}
