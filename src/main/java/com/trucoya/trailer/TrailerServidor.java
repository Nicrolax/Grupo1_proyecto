package com.trucoya.trailer;

import com.trucoya.TrucoYa;
import com.trucoya.network.TrailerPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.progreso.CinematicaServidor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El modo tráiler: el jugador marca lugares (/truco trailer marcar) y después la cámara de cine los recorre
 * con carteles y música, para grabar la pantalla y armar el tráiler del mod. Mientras dura, el servidor lleva
 * al jugador (invisible para su propia cámara) por el mismo camino, así el mundo se carga donde está la cámara,
 * y al final lo devuelve al lugar donde empezó.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailerServidor {

    private static final String MARCAS = "trucoya_trailer";

    private static final class EnCurso {
        final Camino camino;
        final long inicio;
        final Vector3d volver;
        final float yaw, pitch;
        final RegistryKey<World> mundo;

        EnCurso(Camino camino, long inicio, ServerPlayerEntity p) {
            this.camino = camino;
            this.inicio = inicio;
            this.volver = p.position();
            this.yaw = p.yRot;
            this.pitch = p.xRot;
            this.mundo = p.level.dimension();
        }
    }

    private static final Map<UUID, EnCurso> EN_CURSO = new HashMap<>();

    private TrailerServidor() {
    }

    private static ListNBT lista(PlayerEntity p) {
        CompoundNBT pers = p.getPersistentData();
        if (!pers.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            pers.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
        }
        CompoundNBT d = pers.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        if (!d.contains(MARCAS, 9)) {
            d.put(MARCAS, new ListNBT());
        }
        return d.getList(MARCAS, 10);
    }

    public static int marcar(ServerPlayerEntity p) {
        ListNBT l = lista(p);
        if (l.size() >= Camino.MAXIMO) {
            p.sendMessage(new StringTextComponent("Ya hay " + Camino.MAXIMO + " marcas (el máximo). Borralas con /truco trailer borrar.")
                    .withStyle(TextFormatting.RED), p.getUUID());
            return 0;
        }
        if (l.size() > 0 && !l.getCompound(0).getString("Mundo").equals(p.level.dimension().location().toString())) {
            p.sendMessage(new StringTextComponent("Todas las marcas tienen que estar en el mismo mundo (dimensión).")
                    .withStyle(TextFormatting.RED), p.getUUID());
            return 0;
        }
        CompoundNBT m = new CompoundNBT();
        Vector3d ojos = p.getEyePosition(1.0F);
        m.putDouble("X", ojos.x);
        m.putDouble("Y", ojos.y);
        m.putDouble("Z", ojos.z);
        m.putFloat("Yaw", p.yRot);
        m.putFloat("Pitch", p.xRot);
        m.putString("Mundo", p.level.dimension().location().toString());
        l.add(m);
        p.sendMessage(new StringTextComponent("🎬 Marca " + l.size() + " guardada: la cámara va a pasar por acá mirando hacia donde mirás.")
                .withStyle(TextFormatting.YELLOW), p.getUUID());
        return l.size();
    }

    public static void borrar(ServerPlayerEntity p) {
        lista(p).clear();
        p.sendMessage(new StringTextComponent("Marcas del tráiler borradas.").withStyle(TextFormatting.GRAY), p.getUUID());
    }

    public static boolean activo(PlayerEntity p) {
        return EN_CURSO.containsKey(p.getUUID());
    }

    /** Arranca el recorrido. Sin marcas suficientes, la cámara da una vuelta alrededor de donde estás. */
    public static String empezar(ServerPlayerEntity p, float segundos) {
        if (activo(p) || CinematicaServidor.enCurso(p)) {
            return "Ya hay una escena en curso.";
        }
        if (p.isSleeping()) {
            return "Primero despertate.";
        }
        if (p.isPassenger()) {
            p.stopRiding();   // sentado o a caballo no se puede llevar al jugador con la cámara
        }
        ListNBT l = lista(p);
        List<Camino.Marca> marcas = new ArrayList<>();
        if (l.size() >= 2) {
            if (!l.getCompound(0).getString("Mundo").equals(p.level.dimension().location().toString())) {
                return "Las marcas son de otro mundo (dimensión). Andá allá o borralas.";
            }
            for (int i = 0; i < l.size(); i++) {
                CompoundNBT m = l.getCompound(i);
                marcas.add(new Camino.Marca(m.getDouble("X"), m.getDouble("Y"), m.getDouble("Z"), m.getFloat("Yaw"), m.getFloat("Pitch")));
            }
        } else {
            // una vuelta lenta alrededor del jugador, de arriba, mirando al centro
            Vector3d c = p.position().add(0, 1.0D, 0);
            for (int i = 0; i <= 8; i++) {
                double a = Math.toRadians(p.yRot + 90.0D + i * 45.0D);
                double r = 10.0D, alto = 5.0D - i * 0.3D;
                double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r, y = c.y + alto;
                double dx = c.x - x, dy = c.y - y, dz = c.z - z;
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                marcas.add(new Camino.Marca(x, y, z, yaw, pitch));
            }
            segundos = Math.max(segundos, 4.0F);
            p.sendMessage(new StringTextComponent("Sin marcas: la cámara da una vuelta alrededor tuyo. Para armar tu recorrido: "
                    + "/truco trailer marcar en cada lugar.").withStyle(TextFormatting.GRAY), p.getUUID());
        }
        Camino camino = new Camino(marcas, segundos);
        MinecraftServer server = p.getServer();
        if (server == null) {
            return "Sin servidor.";
        }
        EN_CURSO.put(p.getUUID(), new EnCurso(camino, server.getTickCount(), p));
        p.setNoGravity(true);   // entre un traslado y otro el jugador no cae
        int ticks = (int) Math.ceil(camino.duracion() * 20.0D) + 20 * 12;
        CinematicaServidor.proteger(p, ticks);
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new TrailerPacket(camino));
        return null;
    }

    public static void parar(ServerPlayerEntity p) {
        EnCurso e = EN_CURSO.remove(p.getUUID());
        if (e != null) {
            devolver(p, e);
            CinematicaServidor.terminar(p);
            TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new TrailerPacket(null));
        }
    }

    private static void devolver(ServerPlayerEntity p, EnCurso e) {
        p.setNoGravity(false);
        if (p.level.dimension() == e.mundo) {
            p.connection.teleport(e.volver.x, e.volver.y, e.volver.z, e.yaw, e.pitch);
        }
        p.fallDistance = 0.0F;
        p.setDeltaMovement(Vector3d.ZERO);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || EN_CURSO.isEmpty()) {
            return;
        }
        MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Iterator<Map.Entry<UUID, EnCurso>> it = EN_CURSO.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EnCurso> en = it.next();
            ServerPlayerEntity p = server.getPlayerList().getPlayer(en.getKey());
            EnCurso e = en.getValue();
            if (p == null) {
                it.remove();
                continue;
            }
            double t = (server.getTickCount() - e.inicio) / 20.0D;
            if (p.level.dimension() != e.mundo || t > e.camino.duracion() + 9.0D) {   // otro mundo, o ya pasó el final
                it.remove();
                devolver(p, e);
                CinematicaServidor.terminar(p);
                TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new TrailerPacket(null));
                continue;
            }
            p.fallDistance = 0.0F;
            if ((server.getTickCount() - e.inicio) % 10 == 0) {
                // el jugador va con la cámara (un poco más abajo): así se carga el mundo que se filma
                Vector3d c = e.camino.posicion(Math.min(t, e.camino.duracion()));
                p.connection.teleport(c.x, c.y - 3.0D, c.z, p.yRot, p.xRot);
                p.setDeltaMovement(Vector3d.ZERO);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EnCurso e = EN_CURSO.remove(event.getPlayer().getUUID());
        if (e != null) {
            event.getPlayer().setNoGravity(false);
            if (event.getPlayer().level.dimension() == e.mundo) {
                event.getPlayer().teleportTo(e.volver.x, e.volver.y, e.volver.z);   // que no quede en el aire al volver
                event.getPlayer().yRot = e.yaw;
                event.getPlayer().xRot = e.pitch;
            }
        }
    }

    /** Por si el juego se cortó en medio de un tráiler: nadie queda flotando. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!activo(event.getPlayer()) && event.getPlayer().isNoGravity()) {
            event.getPlayer().setNoGravity(false);
        }
    }

    public static void alTerminarCliente(ServerPlayerEntity p) {
        EnCurso e = EN_CURSO.remove(p.getUUID());
        if (e != null) {
            devolver(p, e);
        }
    }
}
