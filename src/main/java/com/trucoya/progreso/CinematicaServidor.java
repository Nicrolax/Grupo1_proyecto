package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.network.CinematicaPacket;
import com.trucoya.network.TrucoNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lado servidor de la cinemática del Viejo Vizcacha: decide cuándo se ve (la primera vez que el jugador
 * lo tiene a la vista y cerca) y protege al jugador mientras la mira (nadie le puede pegar).
 * Se ve una sola vez por jugador (queda anotado en sus datos persistentes); /truco cinematica la repite.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CinematicaServidor {

    public static final String VISTA = "cinematica";
    private static final double DISTANCIA = 9.0D;
    /** Tope de seguridad: si el cliente nunca avisa que terminó, la protección se levanta sola. */
    private static final int TOPE_TICKS = 20 * 100;

    private static final Map<UUID, Integer> EN_CURSO = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> VIZCACHA = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> AVISADOS = new ConcurrentHashMap<>();

    private CinematicaServidor() {
    }

    /** Llamado cada segundo por cada Viejo Vizcacha. */
    public static void revisar(GauchoEntity viz) {
        if (!(viz.level instanceof ServerWorld) || !viz.isAlive()) {
            return;
        }
        for (ServerPlayerEntity p : ((ServerWorld) viz.level).players()) {
            if (p.isSpectator() || !p.isAlive() || p.isPassenger() || p.isSleeping()) {
                continue;
            }
            if (p.distanceToSqr(viz) > DISTANCIA * DISTANCIA || JugadorData.get(p, VISTA) != 0) {
                continue;
            }
            if (!viz.canSee(p) || EN_CURSO.containsKey(p.getUUID())) {
                continue;
            }
            if (!Historia.habilitadoVizcacha(p)) {
                // todavía no: se lo dice, sin película (una vez por minuto como mucho)
                Integer ultimo = AVISADOS.get(p.getUUID());
                int ahora = p.getServer() != null ? p.getServer().getTickCount() : 0;
                if (ultimo == null || ahora - ultimo > 1200) {
                    AVISADOS.put(p.getUUID(), ahora);
                    viz.say(Historia.negativaVizcacha(p));
                }
                continue;
            }
            JugadorData.set(p, VISTA, 1);
            iniciar(p, viz);
        }
    }

    public static void iniciar(ServerPlayerEntity p, @Nullable GauchoEntity viz) {
        MinecraftServer server = p.getServer();
        if (server == null) {
            return;
        }
        EN_CURSO.put(p.getUUID(), server.getTickCount() + TOPE_TICKS);
        if (viz != null) {
            VIZCACHA.put(p.getUUID(), viz.getId());
            viz.getNavigation().stop();
        }
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new CinematicaPacket(viz == null ? -1 : viz.getId()));
    }

    /** La cinemática final: después de ganarle al Viejo Vizcacha llegan los cuatro Fundadores. */
    public static void iniciarFinal(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        if (server == null) {
            return;
        }
        GauchoEntity viz = null;
        for (GauchoEntity g : p.getLevel().getEntitiesOfClass(GauchoEntity.class, p.getBoundingBox().inflate(24.0D))) {
            if (g.getRol() == GauchoEntity.Rol.VIZCACHA) viz = g;
        }
        JugadorData.set(p, Historia.FINAL_VISTO, 1);
        Logros.award(p, Logros.EL_FINAL);
        Salamanca.vioElFinal(p);
        EN_CURSO.put(p.getUUID(), server.getTickCount() + TOPE_TICKS);
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new CinematicaPacket(viz == null ? -1 : viz.getId(), 1));
    }

    /** Protege al jugador mientras mira una escena de la Salamanca (con tope de tiempo). */
    public static void proteger(ServerPlayerEntity p, int ticks) {
        MinecraftServer server = p.getServer();
        if (server != null) {
            EN_CURSO.put(p.getUUID(), server.getTickCount() + ticks);
        }
    }

    public static void liberar(ServerPlayerEntity p) {
        EN_CURSO.remove(p.getUUID());
    }

    public static boolean enCurso(PlayerEntity p) {
        if (Salamanca.protegido(p)) {
            return true;
        }
        Integer fin = EN_CURSO.get(p.getUUID());
        if (fin == null) {
            return false;
        }
        MinecraftServer server = p.getServer();
        if (server != null && server.getTickCount() > fin) {
            EN_CURSO.remove(p.getUUID());
            VIZCACHA.remove(p.getUUID());
            return false;
        }
        return true;
    }

    /** El cliente avisó que terminó: el Vizcacha lo invita a la mesa. */
    public static void terminar(ServerPlayerEntity p) {
        if (EN_CURSO.remove(p.getUUID()) == null) {
            return;
        }
        if (JugadorData.get(p, Historia.FINAL_VISTO) != 0) {
            com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.FUNDADORES);   // el regalo de los cuatro
        }
        Integer id = VIZCACHA.remove(p.getUUID());
        if (id != null) {
            Entity e = p.level.getEntity(id);
            if (e instanceof GauchoEntity && e.isAlive() && e.distanceToSqr(p) < 32.0D * 32.0D) {
                ((GauchoEntity) e).say("Sentate, gurí. La mesa te espera.");
            }
        }
    }

    // ------------------------------------------------------------------ protección mientras mira

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayerEntity && enCurso((PlayerEntity) event.getEntityLiving())) {
            event.setCanceled(true);
            return;
        }
        // el que está protegido tampoco puede pegarle a nadie (si no, sería invencible en una pelea)
        net.minecraft.entity.Entity quien = event.getSource().getEntity();
        if (quien instanceof ServerPlayerEntity && enCurso((PlayerEntity) quien)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingSetAttackTargetEvent event) {
        if (event.getTarget() instanceof ServerPlayerEntity && event.getEntityLiving() instanceof MobEntity
                && enCurso((PlayerEntity) event.getTarget())) {
            ((MobEntity) event.getEntityLiving()).setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EN_CURSO.remove(event.getPlayer().getUUID());
        VIZCACHA.remove(event.getPlayer().getUUID());
        AVISADOS.remove(event.getPlayer().getUUID());
        Pedidos.olvidar(event.getPlayer().getUUID());
    }
}
