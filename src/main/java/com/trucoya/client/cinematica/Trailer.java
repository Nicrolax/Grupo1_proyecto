package com.trucoya.client.cinematica;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.trucoya.TrucoYa;
import com.trucoya.network.CinematicaFinPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModSounds;
import com.trucoya.trailer.Camino;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * El modo tráiler, del lado de la pantalla: la cámara de cine recorre las marcas con franjas, viñeta, carteles
 * que cuentan el mod y la milonga de fondo. Termina con el título y los nombres de los cuatro Fundadores.
 * Pensado para grabar la pantalla (Win + Alt + R en Windows) y armar el tráiler.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Trailer {

    /** Los carteles del medio: título y bajada. */
    private static final String[][] CARTELES = {
            {"CUATRO AMIGOS", "y una baraja española"},
            {"UN MUNDO PARA JUGAR AL TRUCO", "pulperías en cada bioma, paisanos que te desafían"},
            {"MATE, PICADA Y PAYADA", "como en el fogón de siempre"},
            {"LA RUTA DE LAS PULPERÍAS", "seis sellos, un mapa y una estancia"},
            {"EL VIEJO VIZCACHA TE ESPERA", "setenta años sin perder una mano"},
            {"Y CUANDO CAE LA NOCHE...", "la Salamanca abre sus puertas"},
    };
    private static final double FINAL = 8.0D;   // segundos del cartel final (la cámara sigue quieta)

    @Nullable
    private static Trailer actual;

    private final Minecraft mc;
    private final ClientWorld level;
    private final Camino camino;
    private final CamaraEntity cam;
    private final PointOfView vistaAntes;
    private final boolean hudAntes;
    private final double recorrido;
    private final double duracion;
    @Nullable
    private ISound musica;
    private double t;
    private long ultimo;
    private boolean terminado;

    public static void iniciar(@Nullable Camino camino) {
        Minecraft mc = Minecraft.getInstance();
        if (camino == null) {
            if (actual != null) actual.terminar(false);
            return;
        }
        if (actual != null || mc.level == null || mc.player == null || camino.marcas.isEmpty()
                || Cinematica.activa() || EscenaViva.activa()) {
            if (actual == null) avisarFin();   // que el servidor no siga llevando al jugador sin cámara
            return;
        }
        try {
            if (mc.screen != null) mc.setScreen(null);
            actual = new Trailer(mc, camino);
        } catch (Exception e) {
            TrucoYa.LOGGER.error("Truco Ya: no se pudo empezar el tráiler", e);
            actual = null;
            avisarFin();
        }
    }

    public static boolean activo() {
        return actual != null;
    }

    private Trailer(Minecraft mc, Camino camino) {
        this.mc = mc;
        this.level = mc.level;
        this.camino = camino;
        this.recorrido = camino.duracion();
        this.duracion = recorrido + FINAL;
        this.vistaAntes = mc.options.getCameraType();
        this.hudAntes = mc.options.hideGui;
        this.cam = new CamaraEntity(level);
        mc.getMusicManager().stopPlaying();
        musica = new SimpleSound(ModSounds.DISCO_MILONGA.get().getLocation(), SoundCategory.MASTER, 0.7F, 1.0F, false, 0,
                ISound.AttenuationType.NONE, 0.0D, 0.0D, 0.0D, true);
        mc.getSoundManager().play(musica);
        mc.setCameraEntity(cam);
        ultimo = Util.getNanos();
        frame();
    }

    private void frame() {
        if (mc.level != level || mc.player == null) {
            terminar(true);
            return;
        }
        long ahora = Util.getNanos();
        double dt = Math.min(0.25D, (ahora - ultimo) / 1.0E9D);
        ultimo = ahora;
        if (!mc.isPaused()) {
            t += dt;
        }
        if (t >= duracion) {
            terminar(true);
            return;
        }
        mc.options.hideGui = true;
        mc.options.setCameraType(PointOfView.FIRST_PERSON);
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
        double tc = Math.min(t, recorrido);
        Vector3d pos = camino.posicion(tc);
        Vector3d mira = camino.mira(tc);
        Vector3d d = mira.subtract(pos);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(d.y, horiz) * (180.0D / Math.PI)));
        cam.apuntar(pos.x, pos.y, pos.z, yaw, pitch);
    }

    private void terminar(boolean avisar) {
        if (terminado) return;
        terminado = true;
        try {
            if (musica != null) mc.getSoundManager().stop(musica);
        } catch (Exception e) {
            com.trucoya.TrucoYa.LOGGER.debug("Truco Ya: no se pudo cortar la música del tráiler", e);
        }
        mc.options.hideGui = hudAntes;
        mc.options.setCameraType(vistaAntes);
        if (mc.player != null) mc.setCameraEntity(mc.player);
        actual = null;
        if (avisar) avisarFin();
    }

    private static void avisarFin() {
        try {
            if (Minecraft.getInstance().getConnection() != null) {
                TrucoNetwork.CHANNEL.sendToServer(new CinematicaFinPacket());
            }
        } catch (Exception e) {
            com.trucoya.TrucoYa.LOGGER.debug("Truco Ya: sin conexión para avisar el fin del tráiler", e);
        }
    }

    // ------------------------------------------------------------------ lo que se dibuja encima

    private void dibujar() {
        if (mc.screen != null) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        MatrixStack ms = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Pantalla.vineta(mc, w, h, 0.5F);
        double barra = Pantalla.barra(w, h) * Toma.suave(Math.min(1.0D, t / 1.2D));
        Pantalla.franjas(ms, w, h, barra);
        // fundidos: entra de negro, y al final baja a casi negro detrás del título
        double negro = Math.max(t < 1.5D ? 1.0D - Toma.suave(t / 1.5D) : 0.0D,
                0.72D * Pantalla.tramo(t, recorrido - 0.5D, recorrido + 1.5D, duracion, duracion + 1.0D));
        negro = Math.max(negro, Pantalla.tramo(t, duracion - 1.2D, duracion, duracion, duracion + 1.0D));
        if (negro > 0.003D) AbstractGui.fill(ms, 0, 0, w, h, Pantalla.argb(negro, 0x000000));
        FontRenderer font = mc.font;
        // carteles repartidos a lo largo del recorrido
        int n = Math.max(1, Math.min(CARTELES.length, (int) Math.floor((recorrido - 2.0D) / 4.5D)));
        double espacio = (recorrido - 2.0D) / n;
        for (int i = 0; i < n; i++) {
            String[] c = CARTELES[(int) Math.round(i * (CARTELES.length - 1) / (double) Math.max(1, n - 1))];
            if (n == 1) c = CARTELES[0];
            double a0 = 1.5D + i * espacio, a1 = a0 + Math.min(4.2D, espacio * 0.85D);
            double a = Pantalla.tramo(t, a0, a0 + 0.9D, a1 - 0.9D, a1);
            if (a > 0.01D) {
                float escala = Math.max(1.6F, h / 130.0F);
                double y = h - barra - 9 * escala - 22;
                Pantalla.espaciado(ms, font, c[0], w / 2.0D, y, escala, escala, Pantalla.argb(a, 0xEBD7A4));
                Pantalla.centrado(ms, font, c[1], w / 2.0D, y + 9 * escala + 4, Math.max(1.0F, escala * 0.5F),
                        Pantalla.argb(a * 0.9D, 0xD8C69C));
            }
        }
        // el cierre: título, bajada y los Fundadores
        double af = Pantalla.tramo(t, recorrido + 0.3D, recorrido + 1.8D, duracion - 1.0D, duracion);
        if (af > 0.01D) {
            float grande = Math.max(4.0F, h / 45.0F);
            double y = h * 0.30D;
            Pantalla.espaciado(ms, font, "TRUCO YA", w / 2.0D, y, grande, grande * 1.6F, Pantalla.argb(af, 0xEBD7A4));
            AbstractGui.fill(ms, (int) (w * 0.36D), (int) (y + 9 * grande + 6), (int) (w * 0.64D), (int) (y + 9 * grande + 7),
                    Pantalla.argb(af * 0.8D, 0xB8860B));
            Pantalla.centrado(ms, font, "Truco uruguayo para Minecraft 1.16.5", w / 2.0D, y + 9 * grande + 14,
                    Math.max(1.0F, grande * 0.28F), Pantalla.argb(af, 0xD8C69C));
            double an = Pantalla.tramo(t, recorrido + 2.4D, recorrido + 3.4D, duracion - 1.0D, duracion);
            if (an > 0.01D) {
                Pantalla.centrado(ms, font, "Nicolás  ·  Agustín  ·  Matías  ·  Thiago", w / 2.0D, y + 9 * grande + 34,
                        Math.max(1.0F, grande * 0.24F), Pantalla.argb(an, 0xF3E7C9));
            }
        }
        RenderSystem.enableDepthTest();
    }

    // ------------------------------------------------------------------ eventos

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Trailer tr = actual;
        if (tr == null) return;
        try {
            if (event.phase == TickEvent.Phase.START) {
                tr.frame();
            } else {
                tr.dibujar();
            }
        } catch (Exception e) {
            TrucoYa.LOGGER.error("Truco Ya: el tráiler falló y se cortó", e);
            tr.terminar(true);
        }
    }

    /** El propio jugador no aparece en cuadro (el servidor lo lleva junto a la cámara). */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (actual != null && event.getPlayer() == actual.mc.player) {
            event.setCanceled(true);
        }
    }

    /** Sin el cartel de "adentro de un bloque" ni el agua o el fuego del jugador (la cámara está en otro lado). */
    @SubscribeEvent
    public static void onOverlay(net.minecraftforge.client.event.RenderBlockOverlayEvent event) {
        if (actual != null) {
            event.setCanceled(true);
        }
    }

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
        if (actual != null) event.setFOV(62.0D);
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
        if (actual != null) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        if (actual != null) actual.terminar(false);
    }
}
