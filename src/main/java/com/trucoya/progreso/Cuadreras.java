package com.trucoya.progreso;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carreras cuadreras: largada desde el banderín, 120 bloques en línea recta contra dos jinetes del pago.
 * Los caballos rivales van a la velocidad del tuyo (uno apenas más lento, otro parejo), así que gana
 * quien mejor corre: derecho, sin chocar y sin largar en falso.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Cuadreras {

    public static final int DISTANCIA = 120;
    private static final int CUENTA = 60;
    private static final int TOPE = 20 * 60;
    /** Bloques por tick de un caballo por cada punto de su atributo de velocidad. */
    private static final double FACTOR_CABALLO = 2.107D;
    private static final String[] JINETES = {"El Pampa Rodríguez", "Ño Cirilo, domador", "El Zaino Benítez",
        "Don Florentino", "El Tordillo Acosta", "El Chueco Maidana", "Patricio el Resero"};

    private static final class Jinete {
        final GauchoEntity gaucho;
        final HorseEntity caballo;
        final double vel;
        final double desvio;
        final double fase;
        int llegada = -1;

        Jinete(GauchoEntity gaucho, HorseEntity caballo, double vel, double desvio, double fase) {
            this.gaucho = gaucho;
            this.caballo = caballo;
            this.vel = vel;
            this.desvio = desvio;
            this.fase = fase;
        }
    }

    private static final class Carrera {
        final UUID jugador;
        final ServerWorld world;
        final Vector3d inicio;
        final Vector3d dir;
        final Vector3d lado;
        final List<Jinete> rivales = new ArrayList<>();
        final ItemStack apuesta;
        int t;
        int llegadaJugador = -1;
        int fin = -1;

        Carrera(ServerPlayerEntity p, Vector3d inicio, Vector3d dir, ItemStack apuesta) {
            this.jugador = p.getUUID();
            this.world = p.getLevel();
            this.inicio = inicio;
            this.dir = dir;
            this.lado = new Vector3d(-dir.z, 0, dir.x);
            this.apuesta = apuesta;
        }

        double avance(Vector3d pos) {
            return (pos.x - inicio.x) * dir.x + (pos.z - inicio.z) * dir.z;
        }
    }

    private static final Map<UUID, Carrera> CARRERAS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private Cuadreras() {
    }

    public static void largar(ServerPlayerEntity p, BlockPos raya, Direction hacia) {
        if (CARRERAS.containsKey(p.getUUID())) {
            p.displayClientMessage(new StringTextComponent("Ya estás corriendo una cuadrera."), true);
            return;
        }
        if (!(p.getVehicle() instanceof AbstractHorseEntity)) {
            return;
        }
        AbstractHorseEntity mio = (AbstractHorseEntity) p.getVehicle();
        Vector3d dir = new Vector3d(hacia.getStepX(), 0, hacia.getStepZ());
        Vector3d inicio = new Vector3d(raya.getX() + 0.5D, raya.getY(), raya.getZ() + 0.5D);
        Carrera c = new Carrera(p, inicio, dir, ItemStack.EMPTY);
        if (p.position().distanceTo(inicio) > 6.0D || Math.abs(c.avance(p.position())) > 3.0D) {
            p.displayClientMessage(new StringTextComponent("Acercate a la raya, al lado del banderín."), true);
            return;
        }
        // la apuesta: las esmeraldas que tengas en la mano
        ItemStack mano = p.getMainHandItem();
        ItemStack apuesta = ItemStack.EMPTY;
        if (mano.getItem() == Items.EMERALD) {
            apuesta = mano.split(Math.min(32, mano.getCount()));
        }
        c = new Carrera(p, inicio, dir, apuesta);
        double velMia = mio.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double[] factores = {0.93D + RANDOM.nextDouble() * 0.03D, 0.98D + RANDOM.nextDouble() * 0.04D};
        double[] lados = {-3.0D, 3.0D};
        List<String> nombres = new ArrayList<>(java.util.Arrays.asList(JINETES));
        java.util.Collections.shuffle(nombres, RANDOM);
        for (int i = 0; i < 2; i++) {
            Vector3d pos = inicio.add(c.lado.scale(lados[i]));
            int y = c.world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(pos.x), MathHelper.floor(pos.z));
            GauchoEntity g = ModEntities.GAUCHO.get().create(c.world);
            HorseEntity h = EntityType.HORSE.create(c.world);
            if (g == null || h == null) {
                continue;
            }
            float yaw = hacia.toYRot();
            BlockPos bp = new BlockPos(pos.x, y, pos.z);
            h.moveTo(pos.x, y, pos.z, yaw, 0F);
            h.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(bp), SpawnReason.EVENT, null, null);
            h.setTamed(true);
            h.equipSaddle(null);
            h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(velMia * factores[i]);
            g.setRol(GauchoEntity.Rol.FORASTERO);
            g.setCustomName(new StringTextComponent(nombres.get(i)));
            g.moveTo(pos.x, y, pos.z, yaw, 0F);
            g.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(bp), SpawnReason.EVENT, null, null);
            g.initDefaults();
            c.world.addFreshEntity(h);
            c.world.addFreshEntity(g);
            g.startRiding(h, true);
            g.setHorse(h.getUUID());
            c.rivales.add(new Jinete(g, h, velMia * factores[i] * FACTOR_CABALLO, lados[i], RANDOM.nextDouble() * 6.0D));
        }
        if (c.rivales.isEmpty()) {
            devolver(p, apuesta);
            return;
        }
        CARRERAS.put(p.getUUID(), c);
        p.sendMessage(new StringTextComponent("¡Cuadrera! " + DISTANCIA + " bloques derecho"
                + (apuesta.isEmpty() ? "" : ", apostando " + apuesta.getCount() + " esmeraldas") + ". No largues antes de tiempo...")
                .withStyle(TextFormatting.GOLD), p.getUUID());
        c.rivales.get(0).gaucho.say("¡A ver ese pingo, paisano!");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CARRERAS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Carrera>> it = CARRERAS.entrySet().iterator();
        while (it.hasNext()) {
            Carrera c = it.next().getValue();
            try {
                if (!tick(c)) {
                    it.remove();
                }
            } catch (RuntimeException e) {
                TrucoYa.LOGGER.warn("Truco Ya: la cuadrera falló y se suspendió", e);
                limpiar(c);
                it.remove();
            }
        }
    }

    /** Devuelve false cuando la carrera terminó. */
    private static boolean tick(Carrera c) {
        c.t++;
        ServerPlayerEntity p = (ServerPlayerEntity) c.world.getPlayerByUUID(c.jugador);
        if (p == null || !p.isAlive()) {
            limpiar(c);
            return false;
        }
        boolean montado = p.getVehicle() instanceof AbstractHorseEntity;
        // cuenta regresiva
        if (c.t <= CUENTA) {
            if (c.t == 1 || c.t == 21 || c.t == 41) {
                titulo(p, String.valueOf(3 - c.t / 20), TextFormatting.WHITE);
                c.world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0F, 0.8F);
            }
            if (c.avance(p.position()) > 2.5D || !montado) {
                titulo(p, "¡Largada en falso!", TextFormatting.RED);
                devolver(p, c.apuesta);
                limpiar(c);
                return false;
            }
            for (Jinete j : c.rivales) {
                j.caballo.getNavigation().stop();
                j.caballo.setDeltaMovement(0, j.caballo.getDeltaMovement().y, 0);
            }
            if (c.t == CUENTA) {
                titulo(p, "¡LARGARON!", TextFormatting.GOLD);
                c.world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0F, 1.6F);
                c.world.playSound(null, p.blockPosition(), SoundEvents.HORSE_ANGRY, SoundCategory.NEUTRAL, 0.8F, 1.0F);
            }
            return true;
        }
        int corrido = c.t - CUENTA;
        // los rivales: derecho a la meta, con un leve vaivén y el remate final
        for (Jinete j : c.rivales) {
            if (!j.caballo.isAlive() || !j.gaucho.isAlive()) {
                continue;
            }
            Vector3d pos = j.caballo.position();
            double av = c.avance(pos);
            if (j.llegada < 0 && av >= DISTANCIA) {
                j.llegada = corrido;
                j.gaucho.say(c.llegadaJugador < 0 ? "¡Primero!" : "¡Llegué!");
            }
            if (j.llegada >= 0) {
                j.caballo.setDeltaMovement(j.caballo.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                continue;
            }
            double v = j.vel * (1.0D + 0.035D * Math.sin(corrido * 0.06D + j.fase));
            if (corrido < 30) {
                v *= 0.45D + 0.55D * corrido / 30.0D;      // la arrancada
            }
            if (av > DISTANCIA * 0.75D) {
                v *= 1.025D;                                 // el remate
            }
            Vector3d meta = c.inicio.add(c.dir.scale(av + 6.0D)).add(c.lado.scale(j.desvio));
            Vector3d hacia = new Vector3d(meta.x - pos.x, 0, meta.z - pos.z).normalize();
            double vy = j.caballo.getDeltaMovement().y;
            if (j.caballo.horizontalCollision && j.caballo.isOnGround()) {
                vy = 0.55D;                                  // salta lo que se le cruza
            }
            j.caballo.getNavigation().stop();
            j.caballo.setDeltaMovement(hacia.x * v, vy, hacia.z * v);
            float yaw = (float) (MathHelper.atan2(hacia.z, hacia.x) * (180D / Math.PI)) - 90.0F;
            j.caballo.yRot = yaw;
            j.caballo.yBodyRot = yaw;
            j.caballo.yHeadRot = yaw;
            if (corrido % 4 == 0) {
                c.world.sendParticles(ParticleTypes.CLOUD, pos.x - c.dir.x, pos.y + 0.2D, pos.z - c.dir.z, 1, 0.2, 0.05, 0.2, 0.01);
            }
        }
        if (c.llegadaJugador < 0) {
            if (!montado) {
                titulo(p, "Te bajaste del caballo", TextFormatting.RED);
                p.sendMessage(new StringTextComponent("Abandonaste la cuadrera" + (c.apuesta.isEmpty() ? "." : ": la apuesta se pierde."))
                        .withStyle(TextFormatting.GRAY), p.getUUID());
                limpiar(c);
                return false;
            }
            double av = c.avance(p.position());
            if (av >= DISTANCIA) {
                c.llegadaJugador = corrido;
                int antes = 0;
                for (Jinete j : c.rivales) {
                    if (j.llegada >= 0 && j.llegada < corrido) antes++;
                }
                resultado(p, c, antes + 1);
                c.fin = c.t + 60;
            } else if (corrido % 20 == 0) {
                int adelante = 0;
                for (Jinete j : c.rivales) {
                    if (j.llegada >= 0 || c.avance(j.caballo.position()) > av) adelante++;
                }
                p.displayClientMessage(new StringTextComponent((adelante + 1) + "° · faltan " + (int) Math.max(0, DISTANCIA - av)
                        + " bloques").withStyle(adelante == 0 ? TextFormatting.GOLD : TextFormatting.WHITE), true);
            }
            if (c.t > TOPE) {
                p.sendMessage(new StringTextComponent("Se hizo tarde: la cuadrera se suspendió."), p.getUUID());
                limpiar(c);
                return false;
            }
        }
        if (c.fin >= 0 && c.t >= c.fin) {
            limpiar(c);
            return false;
        }
        return true;
    }

    private static void resultado(ServerPlayerEntity p, Carrera c, int puesto) {
        if (puesto == 1) {
            titulo(p, "¡GANASTE LA CUADRERA!", TextFormatting.GOLD);
            c.world.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            if (!c.apuesta.isEmpty()) {
                ItemStack premio = new ItemStack(Items.EMERALD, c.apuesta.getCount() * 2);
                devolver(p, premio);
                p.sendMessage(new StringTextComponent("Cobrás " + premio.getCount() + " esmeraldas.").withStyle(TextFormatting.GREEN),
                        p.getUUID());
            }
            JugadorData.increment(p, Historia.CUADRERAS);
            JugadorData.addPrestigio(p, 8, "Cuadrera ganada");
            Logros.award(p, Logros.PINGO_GANADOR);
            Pedidos.evento(p, Pedidos.RETO_CUADRERA, null);
            Diario.evento(p, Diario.CUADRERA);
            Historia.revisarMapa(p);
            for (Jinete j : c.rivales) {
                j.gaucho.say("¡Qué pingo, paisano! Me ganaste bien.");
            }
        } else {
            titulo(p, "Llegaste " + puesto + "°", TextFormatting.WHITE);
            if (!c.apuesta.isEmpty()) {
                p.sendMessage(new StringTextComponent("Perdiste la apuesta.").withStyle(TextFormatting.GRAY), p.getUUID());
            }
        }
    }

    private static void titulo(ServerPlayerEntity p, String texto, TextFormatting color) {
        p.connection.send(new STitlePacket(STitlePacket.Type.TIMES, null, 0, 22, 6));
        p.connection.send(new STitlePacket(STitlePacket.Type.TITLE, new StringTextComponent(texto).withStyle(color)));
    }

    private static void devolver(ServerPlayerEntity p, ItemStack s) {
        if (!s.isEmpty() && !p.inventory.add(s)) {
            p.drop(s, false);
        }
    }

    /** Los rivales se despiden y se van a su pago. */
    private static void limpiar(Carrera c) {
        for (Jinete j : c.rivales) {
            if (j.gaucho.isAlive()) {
                j.gaucho.despedirse();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Carrera c = CARRERAS.remove(event.getPlayer().getUUID());
        if (c != null) {
            if (event.getPlayer() instanceof ServerPlayerEntity && c.t <= CUENTA) {
                devolver((ServerPlayerEntity) event.getPlayer(), c.apuesta);
            }
            limpiar(c);
        }
    }

    public static boolean corriendo(ServerPlayerEntity p) {
        return CARRERAS.containsKey(p.getUUID());
    }
}
