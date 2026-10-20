package com.trucoya.client.cinematica;

import com.trucoya.TrucoYa;
import com.trucoya.client.ModKeys;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.network.CinematicaFinPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.progreso.Escenas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Las escenas de LA SALAMANCA: "medias cinemáticas" en el mundo de verdad. El servidor mueve a los personajes y
 * cambia el mundo (rayos, la tapera que se rearma, el amanecer); acá se pone la cámara de cine, las franjas, los
 * subtítulos y las voces, con los mismos tiempos (assets/trucoya/cinematica/escena_*.json).
 *
 * La cámara sigue a los personajes en vivo (donde estén ese cuadro), así nunca encuadra un lugar vacío.
 * Nada de esto cambia el mundo: si algo falla, la escena se corta y el juego sigue.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EscenaViva {

    @Nullable
    private static EscenaViva actual;
    private static double salida = -1.0D;
    private static long salidaNanos;

    private final Minecraft mc;
    private final ClientWorld level;
    final int tipo;
    final Guion guion;
    final Escenas.Info info;
    final double duracion;
    private final BlockPos origen;
    private final int guiaId;
    @Nullable
    private final CamaraEntity cam;
    private final PointOfView vistaAntes;
    private final boolean hudAntes;
    private final List<ISound> sonidos = new ArrayList<>();
    private final Set<Integer> sonadas = new HashSet<>();
    private final List<double[]> destellos = new ArrayList<>();
    private int proximoDestello;

    double t;
    double salto;
    double destello;
    private long ultimo;
    private double fov = 60.0D;
    private double tiro = 1.0D;
    private boolean terminada;
    @Nullable
    private GauchoEntity hablando;

    // ------------------------------------------------------------------ arranque

    public static void iniciar(int tipo, int guiaId, BlockPos origen) {
        if (tipo < 0) {
            // el servidor canceló la Salamanca: la escena se corta
            if (actual != null) actual.terminar();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || actual != null || Cinematica.activa()
                || tipo < 0 || tipo >= Escenas.NOMBRES.length) {
            return;
        }
        try {
            if (mc.screen != null) {
                mc.setScreen(null);
            }
            actual = new EscenaViva(mc, tipo, guiaId, origen);
        } catch (Exception e) {
            TrucoYa.LOGGER.error("Truco Ya: no se pudo iniciar la escena", e);
            actual = null;
            avisarFin();
        }
    }

    public static boolean activa() {
        return actual != null;
    }

    private EscenaViva(Minecraft mc, int tipo, int guiaId, BlockPos origen) {
        this.mc = mc;
        this.level = mc.level;
        this.tipo = tipo;
        this.origen = origen;
        this.guiaId = guiaId;
        this.guion = Guion.cargar("escena_" + Escenas.NOMBRES[tipo]);
        this.info = Escenas.de(tipo);
        this.duracion = info.duracion;
        this.vistaAntes = mc.options.getCameraType();
        this.hudAntes = mc.options.hideGui;
        this.cam = tipo == Escenas.VIAJE ? null : new CamaraEntity(level);
        // los fogonazos (rayos que tocan la Salamanca, el amanecer que la deshace)
        String[] claves = {"rayo1", "rayo2", "salamanca", "ruina"};
        for (String k : claves) {
            double m = info.marca(k, -1.0D);
            if (m >= 0.0D && !(tipo == Escenas.DERROTA && k.equals("ruina"))) {
                destellos.add(new double[] {m, k.equals("salamanca") || k.equals("ruina") ? 1.0D : 0.55D});
            }
        }
        destellos.sort((a, b) -> Double.compare(a[0], b[0]));
        mc.getMusicManager().stopPlaying();
        if (cam != null) {
            mc.setCameraEntity(cam);
        }
        ultimo = Util.getNanos();
        frame();
    }

    // ------------------------------------------------------------------ cada cuadro

    private void frame() {
        if (mc.level != level || mc.player == null) {
            abortar();
            return;
        }
        long ahora = Util.getNanos();
        double dt = Math.min(1.0D, (ahora - ultimo) / 1.0E9D);
        ultimo = ahora;
        if (mc.isPaused()) {
            dt = 0.0D;
        }
        t += dt;
        destello = Math.max(0.0D, destello - dt * 2.5D);
        while (proximoDestello < destellos.size() && destellos.get(proximoDestello)[0] <= t) {
            double[] d = destellos.get(proximoDestello++);
            destello = Math.max(destello, d[1]);
            sonar(SoundEvents.LIGHTNING_BOLT_THUNDER.getLocation(), SoundCategory.WEATHER, d[1] >= 1.0D ? 1.0F : 0.7F, 0.8F);
        }
        // voces y gestos
        GauchoEntity quien = null;
        for (int i = 0; i < guion.lineas.size(); i++) {
            Guion.Linea l = guion.lineas.get(i);
            if (t >= l.t && sonadas.add(i)) {
                sonar(new ResourceLocation(TrucoYa.MOD_ID, l.sonido), SoundCategory.VOICE, 1.0F, 1.0F);
            }
            if (t >= l.t && t < l.fin - 0.3D) {
                quien = actor(l.quien);
            }
        }
        if (quien != hablando) {
            if (hablando != null) hablando.gestoCliente(false);
            if (quien != null) quien.gestoCliente(true);
            hablando = quien;
        }
        if (t >= duracion) {
            terminar();
            return;
        }
        mc.options.hideGui = true;
        if (cam != null) {
            mc.options.setCameraType(PointOfView.FIRST_PERSON);
            if (mc.getCameraEntity() != cam) {
                mc.setCameraEntity(cam);
            }
            ubicarCamara(dt);
        }
    }

    /** Fundido a negro de esta escena. */
    double negro(double t) {
        double inicio = t < 0.8D ? 1.0D - Toma.suave(t / 0.8D) : 0.0D;
        switch (tipo) {
            case Escenas.VIAJE:
                return Math.max(Pantalla.tramo(t, 0.0D, 0.8D, duracion, duracion + 5.0D), 0.0D);
            case Escenas.DERROTA: {
                double a = info.marca("negro", 9.0D);
                return Math.max(inicio, Pantalla.tramo(t, a, a + 1.0D, duracion, duracion + 5.0D));
            }
            case Escenas.LLEGADA:
            case Escenas.AMANECER:
            case Escenas.VISITA:
            default:
                return Math.max(inicio, Pantalla.tramo(t, duracion - 1.0D, duracion, duracion, duracion + 5.0D));
        }
    }

    // ------------------------------------------------------------------ cámara

    private Vector3d local(double x, double y, double z) {
        return new Vector3d(origen.getX() + x, origen.getY() + y, origen.getZ() + z);
    }

    @Nullable
    private GauchoEntity actor(String quien) {
        if (quien == null || quien.isEmpty()) {
            return null;
        }
        if (quien.equals("vizcacha")) {
            Entity e = level.getEntity(guiaId);
            if (e instanceof GauchoEntity) return (GauchoEntity) e;
        }
        GauchoEntity.Rol rol = quien.equals("mandinga") ? GauchoEntity.Rol.MANDINGA
                : quien.equals("ledesma") ? GauchoEntity.Rol.LEDESMA : GauchoEntity.Rol.VIZCACHA;
        GauchoEntity mejor = null;
        double d = 64.0D * 64.0D;
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof GauchoEntity && ((GauchoEntity) e).getRol() == rol && e.distanceToSqr(mc.player) < d) {
                d = e.distanceToSqr(mc.player);
                mejor = (GauchoEntity) e;
            }
        }
        return mejor;
    }

    private Vector3d ojos(@Nullable Entity e, Vector3d siNo) {
        if (e == null) return siNo;
        float pt = mc.getFrameTime();
        return e.getEyePosition(pt);
    }

    private void ubicarCamara(double dt) {
        Toma.Pose pose = pose(t);
        Vector3d pos = sinParedes(pose.mira, pose.pos, dt);
        Vector3d d = pose.mira.subtract(pos);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(d.y, horiz) * (180.0D / Math.PI)));
        cam.apuntar(pos.x, pos.y, pos.z, yaw, pitch);
        fov = pose.fov;
    }

    private static Toma.Pose p(Vector3d pos, Vector3d mira, double fov) {
        return new Toma.Pose(pos, mira, fov, 0.0D);
    }

    private static double u(double t, double a, double b) {
        return Toma.suave((t - a) / (b - a));
    }

    private Toma.Pose pose(double t) {
        ClientPlayerEntity pl = mc.player;
        switch (tipo) {
            case Escenas.VISITA: {
                Entity v = level.getEntity(guiaId);
                Vector3d hP = ojos(pl, pl.position().add(0, 1.62D, 0));
                Vector3d hV = ojos(v, hP.add(0, 0, 3));
                Vector3d d = hV.subtract(hP);
                Vector3d un = new Vector3d(d.x, 0, d.z);
                un = un.lengthSqr() < 0.01D ? new Vector3d(0, 0, 1) : un.normalize();
                Vector3d s = new Vector3d(-un.z, 0, un.x);
                Vector3d medio = hP.add(hV).scale(0.5D);
                if (t < 6.0D) return p(hP.subtract(un.scale(2.6D)).add(s.scale(1.1D)).add(0, 0.35D, 0), hV, 62);
                if (t < 17.0D) {
                    double k = u(t, 6.0D, 17.0D);
                    return p(hV.subtract(un.scale(1.75D - 0.3D * k)).add(s.scale(0.6D)).add(0, -0.05D, 0), hV.add(0, -0.05D, 0), 50);
                }
                if (t < 28.0D) return p(hV.add(un.scale(1.3D)).subtract(s.scale(0.7D)).add(0, 0.15D, 0), hP, 55);
                if (t < 39.0D) return p(medio.add(s.scale(3.4D)).add(0, 0.4D, 0).subtract(un.scale(0.3D)), medio, 58);
                if (t < 45.5D) return p(hV.subtract(un.scale(1.3D)).add(s.scale(0.35D)), hV, 44);
                if (t < 58.0D) {
                    double k = u(t, 45.5D, 58.0D);
                    return p(hV.subtract(un.scale(1.9D - 0.85D * k)).add(s.scale(0.5D - 0.25D * k)), hV, 46 - 10 * k);
                }
                if (t < 64.0D) return p(hV.add(un.scale(0.8D)).subtract(s.scale(0.6D)).add(0, 0.1D, 0), hP, 52);
                return p(hP.subtract(un.scale(4.5D)).add(s.scale(2.5D)).add(0, 1.6D, 0), medio, 64);
            }
            case Escenas.LLEGADA: {
                if (t < 5.0D) {
                    double k = u(t, 0.0D, 5.0D);
                    return p(local(17.5D, 9.0D - 0.5D * k, -3.0D + 2.0D * k), local(17.5D, 4.0D, 17.0D), 60);
                }
                if (t < 11.0D) {
                    double k = u(t, 5.0D, 11.0D);
                    return p(local(17.5D, 4.8D - 0.6D * k, 1.0D + 6.5D * k), local(17.5D, 3.8D, 17.0D), 58 - 6 * k);
                }
                if (t < 16.0D) return p(local(16.2D, 4.5D, 15.6D), local(18.5D, 4.2D, 17.5D), 50);
                if (t < 22.0D) return p(local(17.3D, 4.3D, 16.3D), local(18.5D, 4.15D, 17.5D), 45);
                if (t < 27.0D) {
                    Entity v = level.getEntity(guiaId);
                    Vector3d hV = ojos(v, local(17.5D, 4.2D, 18.5D));
                    Vector3d pos = local(17.5D, 4.4D, 15.9D);
                    return p(pos, hV, 48);
                }
                if (t < 32.2D) return p(local(17.0D, 4.4D, 15.8D), local(18.5D, 4.1D, 17.5D), 46);
                if (t < 37.4D) return p(local(17.9D, 4.3D, 16.0D), local(16.5D, 4.1D, 17.5D), 44);
                return p(local(17.5D, 5.6D, 13.2D), local(17.5D, 3.6D, 17.5D), 62);
            }
            case Escenas.AMANECER: {
                if (t < 6.0D) return p(local(17.2D, 4.3D, 16.2D), local(18.5D, 4.15D, 17.5D), 46);
                if (t < 13.0D) return p(local(17.3D, 4.4D, 16.6D), local(17.3D, 4.2D, 19.5D), 55);
                if (t < 17.5D) return p(local(18.9D, 4.55D, 19.1D), local(16.0D, 4.6D, 19.5D), 48);
                if (t < 23.5D) return p(local(17.1D, 4.5D, 19.25D), local(16.0D, 4.6D, 19.5D), 50);
                if (t < 27.5D) return p(local(17.4D, 4.5D, 19.4D), local(18.6D, 4.55D, 19.5D), 50);
                if (t < 31.0D) return p(local(17.9D, 4.8D, 14.4D), local(16.0D, 4.6D, 19.5D), 52);
                double ruina = info.marca("ruina", 33.0D);
                if (t < ruina) return p(local(19.5D, 5.2D, 15.5D), local(16.5D, 4.0D, 19.5D), 58);
                if (t < ruina + 4.0D) {
                    double k = u(t, ruina, ruina + 4.0D);
                    return p(local(4.0D, 6.5D + k, 26.0D), local(24.0D, 6.0D, 12.0D), 64);
                }
                if (t < ruina + 9.0D) return p(local(17.0D, 4.7D, 14.3D), local(18.6D, 4.5D, 19.5D), 52);
                return p(local(17.6D, 4.5D, 18.0D), local(18.6D, 4.55D, 19.5D), 48);
            }
            case Escenas.DERROTA: {
                if (t < 4.6D) return p(local(17.2D, 4.3D, 16.2D), local(18.5D, 4.15D, 17.5D), 46);
                double k = u(t, 4.6D, 9.5D);
                return p(local(17.5D, 5.4D + 0.6D * k, 14.0D - 0.7D * k), local(17.5D, 3.6D, 17.5D), 58 + 6 * k);
            }
            default:
                return p(pl.getEyePosition(1.0F), pl.getEyePosition(1.0F).add(pl.getLookAngle()), 70);
        }
    }

    /** Si entre lo que se mira y la cámara hay una pared, la cámara se acerca (suave). */
    private Vector3d sinParedes(Vector3d objetivo, Vector3d pos, double dt) {
        RayTraceResult r = level.clip(new RayTraceContext(objetivo, pos, RayTraceContext.BlockMode.VISUAL,
                RayTraceContext.FluidMode.NONE, cam));
        double max = 1.0D;
        double largo = pos.distanceTo(objetivo);
        if (r.getType() == RayTraceResult.Type.BLOCK && largo > 0.01D) {
            double hasta = ((BlockRayTraceResult) r).getLocation().distanceTo(objetivo) - 0.3D;
            max = MathHelper.clamp(hasta / largo, 0.08D, 1.0D);
        }
        tiro = max < tiro ? max : Math.min(max, tiro + dt * 1.5D);
        return Toma.lerp(objetivo, pos, tiro);
    }

    private void sonar(ResourceLocation id, SoundCategory cat, float vol, float tono) {
        ISound s = new SimpleSound(id, cat, vol, tono, false, 0, ISound.AttenuationType.NONE, 0.0D, 0.0D, 0.0D, true);
        sonidos.add(s);
        mc.getSoundManager().play(s);
    }

    // ------------------------------------------------------------------ fin

    private void terminar() {
        if (terminada) {
            return;
        }
        terminada = true;
        cerrar();
        actual = null;
        salida = 0.0D;
        salidaNanos = Util.getNanos();
        avisarFin();
    }

    private void abortar() {
        terminada = true;
        cerrar();
        actual = null;
    }

    private void cerrar() {
        try {
            if (hablando != null) hablando.gestoCliente(false);
            for (ISound s : sonidos) {
                mc.getSoundManager().stop(s);
            }
        } catch (Exception e) {
            com.trucoya.TrucoYa.LOGGER.debug("Truco Ya: no se pudieron cortar los sonidos de la escena", e);
        }
        mc.options.hideGui = hudAntes;
        mc.options.setCameraType(vistaAntes);
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
    }

    private static void avisarFin() {
        try {
            if (Minecraft.getInstance().getConnection() != null) {
                TrucoNetwork.CHANNEL.sendToServer(new CinematicaFinPacket());
            }
        } catch (Exception e) {
            com.trucoya.TrucoYa.LOGGER.debug("Truco Ya: sin conexión para avisar el fin de la escena", e);
        }
    }

    private static void fallo(Exception e) {
        TrucoYa.LOGGER.error("Truco Ya: la escena falló y se cortó (el juego sigue normal)", e);
        EscenaViva c = actual;
        if (c != null) {
            try {
                c.terminar();
            } catch (Exception e2) {
                actual = null;
            }
        }
    }

    // ------------------------------------------------------------------ eventos de Forge

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        try {
            if (event.phase == TickEvent.Phase.START) {
                if (actual != null) actual.frame();
                return;
            }
            if (actual != null) {
                Pantalla.dibujarEscena(actual);
            } else if (salida >= 0.0D) {
                salida = (Util.getNanos() - salidaNanos) / 1.0E9D;
                if (salida > 1.4D) {
                    salida = -1.0D;
                } else {
                    Pantalla.dibujarSalida(salida);
                }
            }
        } catch (Exception e) {
            salida = -1.0D;
            fallo(e);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        EscenaViva c = actual;
        if (c == null || event.phase != TickEvent.Phase.START) {
            return;
        }
        try {
            Minecraft mc = c.mc;
            if (mc.isPaused()) {
                return;
            }
            KeyBinding[] bloqueadas = {mc.options.keyInventory, mc.options.keyDrop, mc.options.keySwapOffhand,
                    mc.options.keyTogglePerspective, mc.options.keyPickItem, mc.options.keyAttack, mc.options.keyUse,
                    mc.options.keyAdvancements, ModKeys.SENAS, ModKeys.TAPETE};
            for (KeyBinding k : bloqueadas) {
                while (k.consumeClick()) {
                    // se tragan
                }
            }
            for (KeyBinding k : mc.options.keyHotbarSlots) {
                while (k.consumeClick()) {
                    // ídem
                }
            }
            // estas escenas no se saltean: el mundo cambia al mismo tiempo (lo maneja el servidor)
        } catch (Exception e) {
            fallo(e);
        }
    }

    /** Mientras dura la escena el personaje no camina (la cámara lo tiene encuadrado). */
    @SubscribeEvent
    public static void onInput(InputUpdateEvent event) {
        if (actual != null) {
            event.getMovementInput().leftImpulse = 0.0F;
            event.getMovementInput().forwardImpulse = 0.0F;
            event.getMovementInput().up = false;
            event.getMovementInput().down = false;
            event.getMovementInput().left = false;
            event.getMovementInput().right = false;
            event.getMovementInput().jumping = false;
            event.getMovementInput().shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onClick(InputEvent.ClickInputEvent event) {
        if (actual != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (actual != null && actual.cam != null) {
            event.setFOV(actual.fov);
        }
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        ISound s = event.getSound();
        if (actual != null && s != null && s.getSource() == SoundCategory.MUSIC) {
            event.setResultSound(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onNameplate(RenderNameplateEvent event) {
        if (actual != null) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        if (actual != null) {
            actual.abortar();
        }
        salida = -1.0D;
    }

    @SubscribeEvent
    public static void onUnload(WorldEvent.Unload event) {
        EscenaViva c = actual;
        if (c != null && event.getWorld() == c.level) {
            c.abortar();
        }
    }
}
