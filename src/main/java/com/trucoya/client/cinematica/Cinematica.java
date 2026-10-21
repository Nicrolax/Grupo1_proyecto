package com.trucoya.client.cinematica;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.trucoya.TrucoYa;
import com.trucoya.client.ModKeys;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.network.CinematicaFinPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.CloudOption;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
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
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * "EL ÚLTIMO TRUCO": la cinemática que se ve la primera vez que uno se cruza con el Viejo Vizcacha.
 *
 * Se arma con el propio motor del juego, en vivo: cámara que se mueve en curvas suaves cada cuadro,
 * actores, fuego, rayos, niebla y un filtro sepia para el recuerdo; la narración (la voz del Vizcacha)
 * y la música están compuestas a la misma línea de tiempo. Se ve a la resolución y a los FPS de cada uno.
 *
 *  0-13 s   presente: el Vizcacha reconoce al jugador
 * 13-33 s   recuerdo: el fogón la noche antes de la batalla (Paso de las Ánimas, invierno del cuarenta)
 * 33-58 s   recuerdo: la carga al alba, truenos y cámara lenta
 * 58-68 s   recuerdo: cae el Negro Ledesma y le da la Baraja de Oro
 * 68-90 s   presente: "Sentate, gurí... vamos a ver si sos digno"   — EL ÚLTIMO TRUCO
 *
 * Nada de esto toca el servidor ni el mundo guardado: los bloques del recuerdo existen solo en este
 * cliente y se devuelven tal cual; si algo falla, la cinemática se corta sola y el juego sigue.
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Cinematica {

    static final double DURACION = 90.0D;
    /** Segundos que hay que mantener la tecla de salto para saltear la cinemática. */
    static final double SALTO = 1.3D;
    private static final ResourceLocation FILTRO = new ResourceLocation(TrucoYa.MOD_ID, "shaders/post/recuerdo.json");

    @Nullable
    private static Cinematica actual;
    private static double salida = -1.0D;
    private static long salidaNanos;

    // ------------------------------------------------------------------ estado

    private final Minecraft mc;
    private final ClientWorld level;
    private final GauchoEntity viz;
    private final boolean vizFalso;
    private final CamaraEntity cam;
    private final PointOfView vistaAntes;
    private final boolean hudAntes;
    private final CloudOption nubesAntes;
    final Guion guion;
    /** La cinemática final (con los Fundadores) en vez del encuentro con el Vizcacha. */
    final boolean esFinal;
    final double duracion;
    private final List<Toma> tomas = new ArrayList<>();
    private final List<Evento> eventos = new ArrayList<>();
    private final List<ISound> sonidos = new ArrayList<>();
    private final Random rnd = new Random();
    private int proximo;

    // marco del presente
    private final Vector3d v0, adelante, derecha;
    private final double distancia, ojosViz;

    @Nullable
    private Escenario escenario;
    private boolean filtro;
    private long diaAntes;
    private boolean cicloAntes;
    private long diaRecuerdo;
    private int ticksRecuerdo;

    double t;
    private long ultimo;
    double salto;
    double destello;
    private double fov = 70.0D, roll;
    private double tiro = 1.0D;
    private boolean terminada;

    private static final class Evento {
        final double t;
        final Runnable accion;

        Evento(double t, Runnable accion) {
            this.t = t;
            this.accion = accion;
        }
    }

    // ------------------------------------------------------------------ arranque

    /** Llamado por el paquete del servidor. {@code vizcachaId} es -1 si no hay un Vizcacha cerca. */
    public static void iniciar(int vizcachaId) {
        iniciar(vizcachaId, false);
    }

    /** {@code esFinal}: la cinemática final (después de ganarle al Vizcacha), con los Fundadores. */
    public static void iniciar(int vizcachaId, boolean esFinal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || actual != null) {
            return;
        }
        try {
            if (mc.screen != null) {
                mc.setScreen(null);
            }
            actual = new Cinematica(mc, vizcachaId, esFinal);
        } catch (Exception e) {
            TrucoYa.LOGGER.error("Truco Ya: no se pudo iniciar la cinemática", e);
            actual = null;
            avisarFin();
        }
    }

    public static boolean activa() {
        return actual != null;
    }

    private Cinematica(Minecraft mc, int vizcachaId, boolean esFinal) {
        this.mc = mc;
        this.esFinal = esFinal;
        this.level = mc.level;
        ClientPlayerEntity p = mc.player;
        Entity e = level.getEntity(vizcachaId);
        if (e instanceof GauchoEntity) {
            viz = (GauchoEntity) e;
            vizFalso = false;
        } else {
            // Sin Vizcacha a mano (/truco cinematica): aparece uno solo para esta función.
            viz = new GauchoEntity(ModEntities.GAUCHO.get(), level);
            viz.prepararActor(GauchoEntity.Rol.VIZCACHA, 0, false);
            Vector3d mira = Vector3d.directionFromRotation(0.0F, p.yRot);
            Vector3d pos = p.position().add(mira.x * 3.0D, 0, mira.z * 3.0D);
            viz.moveTo(pos.x, pos.y, pos.z, p.yRot + 180.0F, 0.0F);
            viz.yHeadRot = viz.yBodyRot = p.yRot + 180.0F;
            viz.setId(-1_599_999);
            level.putNonPlayerEntity(-1_599_999, viz);
            vizFalso = true;
        }
        Vector3d hacia = p.position().subtract(viz.position());
        Vector3d plano = new Vector3d(hacia.x, 0, hacia.z);
        if (plano.lengthSqr() < 0.25D) {
            Vector3d m = Vector3d.directionFromRotation(0.0F, viz.yBodyRot);
            plano = new Vector3d(m.x, 0, m.z);
        }
        adelante = plano.normalize();
        derecha = new Vector3d(-adelante.z, 0, adelante.x);
        v0 = viz.position();
        distancia = MathHelper.clamp(plano.length(), 1.8D, 9.0D);
        ojosViz = viz.isPassenger() ? 1.15D : 1.6D;

        guion = Guion.cargar(esFinal ? "final" : "vizcacha");
        duracion = esFinal ? guion.duracion : DURACION;
        cam = new CamaraEntity(level);
        vistaAntes = mc.options.getCameraType();
        hudAntes = mc.options.hideGui;
        nubesAntes = mc.options.renderClouds;

        if (esFinal) {
            armarTomasFinal();
            armarEventosFinal();
        } else {
            armarTomas();
            armarEventos();
        }

        mc.getMusicManager().stopPlaying();
        mc.setCameraEntity(cam);
        aplicarAjustes();
        ultimo = Util.getNanos();
        frame();    // primer cuadro ya en posición
    }

    // ------------------------------------------------------------------ planos

    private void armarTomas() {
        final Toma.Marco P = Toma.Marco.PRESENTE, R = Toma.Marco.RECUERDO;
        final double D = distancia, HV = ojosViz, HP = 1.62D;
        // --- presente: el encuentro
        tomas.add(Toma.camino(P, 0.0D, 5.6D,
                pts(D + 3.8, 3.4, 2.4, D + 2.2, 2.4, 1.4, D + 1.0, HP + 0.25, 0.7),
                pts(0, HV, 0, 0, HV, 0), 70, 62, 0.012));
        tomas.add(Toma.camino(P, 5.6D, 13.3D,
                pts(2.6, HV + 0.15, -1.0, 1.25, HV + 0.05, -0.35),
                pts(0, HV - 0.05, 0, 0, HV - 0.05, 0), 58, 42, 0.006));
        // --- recuerdo: el fogón
        Vector3d f = Escenario.FOGON;
        tomas.add(Toma.camino(R, 13.3D, 19.6D,
                rel(f, 13, 8.5, 10, 8, 4.6, 6.4, 4.6, 2.5, 3.9),
                rel(f, 0, 0.5, 0, 0, 0.9, 0), 66, 56, 0.0));
        Vector3d[] orbita = new Vector3d[5];
        for (int i = 0; i < orbita.length; i++) {
            double a = Math.toRadians(35 + 30 * i);
            orbita[i] = f.add(3.7D * Math.cos(a), 1.55D, 3.7D * Math.sin(a));
        }
        tomas.add(Toma.camino(R, 19.6D, 25.0D, orbita, rel(f, 0, 1.05, 0), 52, 50, 0.004));
        tomas.add(Toma.camino(R, 25.0D, 29.4D,
                rel(f, -0.5, 1.45, 0.9, 0.55, 1.6, 0.5),
                rel(f, 2, 1.62, 0, 2, 1.62, 0), 46, 36, 0.003));
        tomas.add(Toma.camino(R, 29.4D, 33.0D,
                rel(f, -10.5, 0.7, -7.5, -9.8, 0.75, -7.0),
                rel(f, 0, 1.0, 0, 0, 1.1, 0), 62, 60, 0.0));
        // --- recuerdo: la carga al alba
        double L = Escenario.LIDER_X0;
        tomas.add(Toma.camino(R, 33.0D, 41.2D,
                pts(L + 3.7, 2.0, -9.5, L + 3.1, 2.25, -3.2, L + 2.4, 2.55, 1.0),
                pts(L - 1.5, 2.3, -6, L - 0.7, 2.45, -2.5, L, 2.6, 0), 56, 44, 0.005));
        tomas.add(Toma.libre(R, 41.2D, 47.0D, t -> {
            double s = Math.max(0.0D, t - Escenario.ARRANQUE);
            Vector3d pos = new Vector3d(-8.0D + 3.0D * s, 0.45D, 1.5D).add(Toma.temblor(t, 0.03D + 0.05D * Escenario.galope(t)));
            Vector3d mira = pos.add(-6.0D, 1.15D, -0.4D + 0.3D * Math.sin(t * 0.7D));
            return new Toma.Pose(pos, mira, 74.0D, 1.2D * Math.sin(t * 2.3D) * Escenario.galope(t));
        }));
        tomas.add(Toma.libre(R, 47.0D, 52.0D, t -> {
            double xl = Escenario.xJinete(L, t), s = t - 47.0D;
            Vector3d pos = new Vector3d(xl - 5.0D + 0.3D * s, 8.5D - 0.5D * s, -15.0D).add(Toma.temblor(t, 0.05D + destello * 0.25D));
            return new Toma.Pose(pos, new Vector3d(xl + 5.0D, 1.0D, 0.0D), 66.0D, 0.0D);
        }));
        tomas.add(Toma.libre(R, 52.0D, 58.0D, t -> {
            double xl = Escenario.xJinete(L, t), s = t - 52.0D;
            Vector3d cara = new Vector3d(xl, 2.55D, 0.0D);
            Vector3d enemigo = new Vector3d(Escenario.xEnemigo(20.0D, t), 2.2D, 0.0D);
            Vector3d mira = Toma.lerp(cara, enemigo, 0.55D * Toma.suave((s - 2.4D) / 1.8D));
            Vector3d pos = new Vector3d(xl + 2.2D, 2.55D + 0.06D * s, 1.6D).add(Toma.temblor(t, 0.015D));
            return new Toma.Pose(pos, mira, 50.0D + 8.0D * Toma.suave(s / 5.9D), 0.0D);
        }));
        // --- recuerdo: la caída del Negro Ledesma
        Vector3d c = Escenario.CAIDO.add(0.9, 0, 0);
        tomas.add(Toma.camino(R, 58.0D, 63.0D,
                rel(c, -2.9, 9.5, 8.0, -1.3, 4.2, 4.5, -0.5, 1.9, 2.5),
                rel(c, 0, 0.2, 0, 0, 0.5, -0.2), 62, 50, 0.004));
        tomas.add(Toma.camino(R, 63.0D, 68.3D,
                rel(c, 0.5, 1.15, 2.6, 0.1, 0.95, 2.0),
                rel(c, 0, 0.55, -0.2, 0, 0.6, -0.25), 42, 38, 0.003));
        // --- presente: el desafío
        tomas.add(Toma.camino(P, 68.3D, 76.8D,
                pts(1.6, HV + 0.1, -0.9, 1.5, HV + 0.05, 0.0, 1.6, HV + 0.1, 0.8),
                pts(0, HV - 0.05, 0, 0, HV - 0.05, 0), 40, 36, 0.004));
        tomas.add(Toma.camino(P, 76.8D, 83.4D,
                pts(D / 2, 1.5, -(D * 0.5 + 2.6), D / 2, 1.85, -(D * 0.5 + 2.0)),
                pts(D / 2, 1.3, 0, D / 2, 1.35, 0), 55, 50, 0.004));
        tomas.add(Toma.camino(P, 83.4D, DURACION + 1.0D,
                pts(1.3, 0.5, 0.7, 3.2, 1.6, 2.4, 5.5, 3.6, 4.0),
                pts(0, HV, 0, 0, HV - 0.1, 0), 50, 66, 0.0));
    }

    private static Vector3d[] pts(double... v) {
        Vector3d[] r = new Vector3d[v.length / 3];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Vector3d(v[3 * i], v[3 * i + 1], v[3 * i + 2]);
        }
        return r;
    }

    private static Vector3d[] concat(Vector3d[] a, Vector3d[] b) {
        Vector3d[] r = new Vector3d[a.length + b.length - 1];      // b[0] repite el último de a
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 1, r, a.length, b.length - 1);
        return r;
    }

    private static Vector3d[] rel(Vector3d o, double... v) {
        Vector3d[] r = pts(v);
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].add(o);
        }
        return r;
    }

    // ------------------------------------------------------------------ línea de tiempo

    private void en(double t, Runnable r) {
        eventos.add(new Evento(t, r));
    }

    private void armarEventos() {
        en(0.0D, () -> sonar(new ResourceLocation(TrucoYa.MOD_ID, "cinematica.musica"), SoundCategory.RECORDS, 1.0F, 1.0F));
        for (Guion.Linea l : guion.lineas) {
            en(l.t, () -> sonar(new ResourceLocation(TrucoYa.MOD_ID, l.sonido), SoundCategory.VOICE, 1.0F, 1.0F));
            if (l.t < 13.0D || l.t > 68.0D) {        // en el presente habla el Vizcacha de verdad
                en(l.t, () -> viz.gestoCliente(true));
                en(l.fin - 0.3D, () -> viz.gestoCliente(false));
            }
        }
        en(guion.grito, () -> sonar(new ResourceLocation(TrucoYa.MOD_ID, "cinematica.carga"), SoundCategory.VOICE, 1.0F, 1.0F));
        // el recuerdo
        en(12.4D, () -> vanilla(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35F, 0.6F));
        en(12.9D, this::abrirRecuerdo);     // bajo el fundido a blanco: el escenario se arma sin que se vea
        en(20.0D, () -> gesto("a", true));
        en(22.6D, () -> gesto("a", false));
        en(22.4D, () -> gesto("b", true));
        en(24.8D, () -> gesto("b", false));
        en(25.2D, () -> gesto("ledesma", true));
        en(26.6D, () -> conEscenario(Escenario::barajaEnElFogon));
        en(26.6D, () -> vanilla(SoundEvents.NOTE_BLOCK_CHIME, 0.45F, 0.75F));
        en(29.2D, () -> gesto("ledesma", false));
        en(29.4D, () -> conEscenario(Escenario::quitarBaraja));
        en(33.0D, () -> conEscenario(e -> {
            e.carga();
            diaRecuerdo = 23250L;
        }));
        en(33.4D, () -> vanilla(SoundEvents.RAID_HORN, 0.45F, 1.0F));
        en(40.6D, () -> vanilla(SoundEvents.RAID_HORN, 0.6F, 1.05F));
        en(40.9D, () -> conEscenario(e -> e.encabritar(true)));
        en(41.5D, () -> vanilla(SoundEvents.HORSE_ANGRY, 0.9F, 0.95F));
        en(42.1D, () -> conEscenario(e -> e.encabritar(false)));
        rayo(47.6D, 14.0D, -10.0D);
        rayo(49.3D, 20.0D, 8.0D);
        rayo(51.0D, 10.0D, -16.0D);
        en(52.0D, () -> conEscenario(Escenario::enemigos));
        en(57.9D, () -> {
            vanilla(SoundEvents.GENERIC_EXPLODE, 0.8F, 0.55F);
            vanilla(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7F, 0.7F);
            conEscenario(e -> e.choque(57.9D));
        });
        en(58.0D, () -> conEscenario(e -> {
            e.secuela();
            diaRecuerdo = 23700L;
        }));
        en(59.0D, () -> gesto("joven", true));
        en(65.0D, () -> gesto("joven", false));
        en(63.4D, () -> conEscenario(e -> e.entregarBaraja(63.4D)));
        en(65.2D, () -> {
            conEscenario(Escenario::destelloBaraja);
            vanilla(SoundEvents.PLAYER_LEVELUP, 0.5F, 0.6F);
        });
        en(68.3D, this::cerrarRecuerdo);
        eventos.sort(Comparator.comparingDouble(ev -> ev.t));
    }

    private void rayo(double cuando, double adelanteX, double z) {
        en(cuando, () -> conEscenario(e -> {
            e.rayo(new Vector3d(Escenario.xJinete(Escenario.LIDER_X0, cuando) + adelanteX, 0, z));
            vanilla(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9F, 0.75F + rnd.nextFloat() * 0.2F);
            destello = 1.0D;
        }));
    }

    private void gesto(String quien, boolean si) {
        conEscenario(e -> e.gesto(quien, si));
    }

    private void conEscenario(java.util.function.Consumer<Escenario> f) {
        if (escenario != null) {
            f.accept(escenario);
        }
    }

    private void abrirRecuerdo() {
        diaAntes = level.getDayTime();
        cicloAntes = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        ticksRecuerdo = 0;
        diaRecuerdo = 13800L;
        escenario = Escenario.crear(level, mc.player.blockPosition());
        escenario.construir();
        escenario.campamento();
        try {
            mc.gameRenderer.loadEffect(FILTRO);
            filtro = true;
        } catch (Exception e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo cargar el filtro sepia", e);
        }
    }

    private void cerrarRecuerdo() {
        if (escenario != null) {
            escenario.restaurar();
            escenario = null;
            long dia = diaAntes + ticksRecuerdo;
            level.setDayTime(cicloAntes ? dia : -dia);
        }
        if (filtro) {
            mc.gameRenderer.shutdownEffect();
            filtro = false;
        }
    }

    boolean enRecuerdo() {
        return escenario != null;
    }

    /** Filtro de película vieja (solo en el recuerdo de la batalla). */
    boolean peliculaVieja() {
        return escenario != null && !esFinal;
    }

    double blanco(double t) {
        return esFinal ? Pantalla.tramo(t, 12.2D, 12.95D, 12.95D, 14.2D) : Pantalla.blanco(t);
    }

    double negro(double t) {
        if (!esFinal) {
            return Pantalla.negro(t);
        }
        double inicio = t < 0.9D ? 1.0D - Toma.suave(t / 0.9D) : 0.0D;
        // después de los créditos queda en negro (alguien baraja en la oscuridad...)
        return Math.max(inicio, Pantalla.tramo(t, TITULOS - 1.4D, TITULOS, duracion + 9.0D, duracion + 10.0D));
    }

    // ------------------------------------------------------------------ la cinemática final

    /** Segundos que tiene cada Fundador para hablar (su primer plano) y cuánto se corre todo lo que viene después. */
    static final double TURNO_FUNDADOR = 9.5D;
    static final double CORRE = 4 * (TURNO_FUNDADOR - 6.0D);
    /** Fin de los créditos (y de la música); después, la escena en negro que abre La Salamanca. */
    static final double TITULOS = 86.0D + CORRE;
    /** El fantasma del Negro Ledesma, entre el humo del fogón, durante la ronda. */
    static final double FANTASMA_DESDE = 56.6D + CORRE, FANTASMA_HASTA = 63.4D + CORRE;

    private void armarTomasFinal() {
        final Toma.Marco P = Toma.Marco.PRESENTE, R = Toma.Marco.RECUERDO;
        final double HV = ojosViz;
        final Vector3d f = Escenario.FOGON;
        // el Vizcacha, de cerca, entrega la baraja
        tomas.add(Toma.camino(P, 0.0D, 13.3D,
                pts(1.8, HV + 0.12, -0.9, 1.5, HV + 0.06, -0.3, 1.3, HV + 0.05, 0.25),
                pts(0, HV - 0.05, 0, 0, HV - 0.05, 0), 46, 38, 0.004));
        // al atardecer, vienen cuatro jinetes
        tomas.add(Toma.libre(R, 13.3D, 20.5D, t -> {
            double xm = 0;
            for (int i = 0; i < 4; i++) xm += Escenario.xLlegada(i, t) / 4.0D;
            Vector3d pos = f.add(-3.6D + 0.08D * (t - 13.3D), 0.9D, 3.0D);
            return new Toma.Pose(pos, new Vector3d(xm, 1.9D, f.z), 60.0D, 0.0D);
        }));
        tomas.add(Toma.libre(R, 20.5D, 27.6D, t -> {
            double xm = 0;
            for (int i = 0; i < 4; i++) xm += Escenario.xLlegada(i, t) / 4.0D;
            Vector3d pos = new Vector3d(xm + 1.5D, 2.3D, f.z - 8.0D).add(Toma.temblor(t, 0.02D));
            return new Toma.Pose(pos, new Vector3d(xm - 2.5D, 1.8D, f.z), 58.0D, 0.0D);
        }));
        // cada fundador, de cerca, con su frase
        for (int i = 0; i < 4; i++) {
            Vector3d cara = Escenario.caraFundador(i);
            Vector3d alFuego = new Vector3d(f.x - cara.x, 0, f.z - cara.z).normalize();
            Vector3d lado = new Vector3d(-alFuego.z, 0, alFuego.x);
            Vector3d a = cara.add(alFuego.scale(1.9D)).add(lado.scale(0.55D)).add(0, -0.08D, 0);
            Vector3d b = cara.add(alFuego.scale(1.55D)).add(lado.scale(0.25D)).add(0, -0.04D, 0);
            double t0 = 27.6D + TURNO_FUNDADOR * i;
            tomas.add(Toma.camino(R, t0, t0 + TURNO_FUNDADOR, new Vector3d[] {a, b}, new Vector3d[] {cara, cara}, 44, 38, 0.003));
        }
        // la ronda alrededor del fuego: media vuelta por detrás de los Fundadores, hacia el lado del Vizcacha
        double finOrbita = FANTASMA_DESDE - 0.6D;
        Vector3d[] orbita = new Vector3d[5];
        for (int i = 0; i < orbita.length; i++) {
            double ang = Math.toRadians(-40 + 40 * i);
            orbita[i] = f.add(5.5D * Math.cos(ang), 2.1D, 5.5D * Math.sin(ang));
        }
        Vector3d desde = f.add(5.5D * Math.cos(Math.toRadians(135)), 2.0D, 5.5D * Math.sin(Math.toRadians(135)));
        Vector3d[] alLado = {orbita[orbita.length - 1], f.add(0.5D, 2.4D, 6.2D), desde};
        tomas.add(Toma.camino(R, 51.6D + CORRE, finOrbita, concat(orbita, alLado), rel(f, 0, 1.1, 0), 56, 52, 0.004));
        // el fantasma del Negro Ledesma: de frente, desde el lado abierto de la ronda (entre el Vizcacha y los
        // Fundadores), así no lo tapa ninguna silla; la cámara se acerca despacio
        Vector3d cara = f.add(0.45D, 1.55D, 0.0D);
        Vector3d cerca = f.add(3.6D * Math.cos(Math.toRadians(135)), 1.75D, 3.6D * Math.sin(Math.toRadians(135)));
        tomas.add(Toma.camino(R, finOrbita, 64.0D + CORRE, new Vector3d[] {desde, cerca},
                new Vector3d[] {f.add(0, 1.1, 0), cara}, 52, 40, 0.002));
        // títulos: la cámara sube al cielo
        tomas.add(Toma.camino(R, 64.0D + CORRE, duracion + 1.0D,
                rel(f, 5, 2.0, 5, 13, 11, 13, 22, 27, 22),
                rel(f, 0, 0.6, 0, 0, 0.2, 0), 58, 72, 0.0));
    }

    private void armarEventosFinal() {
        en(0.0D, () -> sonar(new ResourceLocation(TrucoYa.MOD_ID, "cinematica.final_musica"), SoundCategory.RECORDS, 1.0F, 1.0F));
        for (Guion.Linea l : guion.lineas) {
            en(l.t, () -> sonar(new ResourceLocation(TrucoYa.MOD_ID, l.sonido), SoundCategory.VOICE, 1.0F, 1.0F));
            if (l.t < 13.0D) {
                en(l.t, () -> viz.gestoCliente(true));
                en(l.fin - 0.3D, () -> viz.gestoCliente(false));
            }
        }
        en(12.95D, this::abrirFiesta);
        for (int i = 0; i < 4; i++) {
            final int k = i;
            double t0 = 28.2D + TURNO_FUNDADOR * i;
            en(t0, () -> conEscenario(e -> e.gestoFundador(k, true)));
            en(t0 + TURNO_FUNDADOR - 1.4D, () -> conEscenario(e -> e.gestoFundador(k, false)));
        }
        en(26.9D, () -> vanilla(SoundEvents.HORSE_AMBIENT, 0.7F, 1.0F));
        en(27.4D, () -> conEscenario(Escenario::bajarse));
        // después de los créditos: alguien baraja en la oscuridad
        for (double k : new double[] {1.2D, 2.0D, 2.9D}) {
            en(TITULOS + k, () -> sonar(new ResourceLocation(TrucoYa.MOD_ID, "card_shuffle"), SoundCategory.MASTER, 0.9F,
                    0.85F + rnd.nextFloat() * 0.1F));
        }
        en(54.4D + CORRE, () -> gesto("joven", true));
        en(59.6D + CORRE, () -> gesto("joven", false));
        eventos.sort(Comparator.comparingDouble(ev -> ev.t));
    }

    /** El campo al atardecer (sin filtro de recuerdo): el Vizcacha junto al fuego y los Fundadores que llegan. */
    private void abrirFiesta() {
        diaAntes = level.getDayTime();
        cicloAntes = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        ticksRecuerdo = 0;
        diaRecuerdo = 12200L;
        escenario = Escenario.crear(level, mc.player.blockPosition());
        escenario.construir();
        escenario.fiesta();
    }

    // ------------------------------------------------------------------ sonido

    private void sonar(ResourceLocation id, SoundCategory cat, float vol, float tono) {
        ISound s = new SimpleSound(id, cat, vol, tono, false, 0, ISound.AttenuationType.NONE, 0.0D, 0.0D, 0.0D, true);
        sonidos.add(s);
        mc.getSoundManager().play(s);
    }

    private void vanilla(@Nullable SoundEvent ev, float vol, float tono) {
        if (ev != null && vol > 0.0F) {
            sonar(ev.getLocation(), SoundCategory.MASTER, vol, tono);
        }
    }

    // ------------------------------------------------------------------ cada cuadro

    private void frame() {
        if (mc.level != level || mc.player == null) {
            abortar();
            return;
        }
        long ahora = Util.getNanos();
        double dt = (ahora - ultimo) / 1.0E9D;
        ultimo = ahora;
        if (mc.isPaused()) {
            dt = 0.0D;
        }
        dt = Math.min(dt, 1.0D);
        t += dt;
        destello = Math.max(0.0D, destello - dt * 4.0D);

        while (proximo < eventos.size() && eventos.get(proximo).t <= t) {
            eventos.get(proximo++).accion.run();
        }
        if (escenario != null) {
            escenario.actualizar(t, dt);
        }
        if (t >= duracion) {
            terminar();
            return;
        }
        aplicarAjustes();
        ubicarCamara(dt);
    }

    private Toma tomaEn(double t) {
        Toma elegida = tomas.get(0);
        for (Toma k : tomas) {
            if (t >= k.t0) {
                elegida = k;
            }
        }
        return elegida;
    }

    private void ubicarCamara(double dt) {
        Toma k = tomaEn(t);
        if (k.marco == Toma.Marco.RECUERDO && escenario == null) {
            k = tomas.get(esFinal ? 0 : 1);    // por si el escenario no pudo armarse
        }
        Toma.Pose pose = k.pose(t);
        Vector3d pos, mira;
        if (k.marco == Toma.Marco.RECUERDO) {
            pos = escenario.mundo(pose.pos);
            mira = escenario.mundo(pose.mira);
        } else {
            pos = presente(pose.pos);
            mira = presente(pose.mira);
            pos = sinParedes(mira, pos, dt);
        }
        Vector3d d = mira.subtract(pos);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(d.y, horiz) * (180.0D / Math.PI)));
        cam.apuntar(pos.x, pos.y, pos.z, yaw, pitch);
        fov = pose.fov;
        roll = pose.roll;

        // el jugador mira al Vizcacha
        ClientPlayerEntity p = mc.player;
        Vector3d h = viz.position().subtract(p.position());
        float py = (float) (MathHelper.atan2(h.z, h.x) * (180.0D / Math.PI)) - 90.0F;
        p.yRot = p.yRotO = p.yHeadRot = p.yHeadRotO = p.yBodyRot = p.yBodyRotO = py;
        p.xRot = p.xRotO = 0.0F;
    }

    private Vector3d presente(Vector3d l) {
        return v0.add(adelante.scale(l.x)).add(0, l.y, 0).add(derecha.scale(l.z));
    }

    /** Si entre el objetivo y la cámara hay una pared, la cámara se acerca (suave, sin saltos). */
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

    private void aplicarAjustes() {
        mc.options.hideGui = true;
        mc.options.setCameraType(PointOfView.FIRST_PERSON);
        mc.options.renderClouds = CloudOption.OFF;
        if (mc.getCameraEntity() != cam) {
            mc.setCameraEntity(cam);
        }
        if (escenario != null) {
            // en el final cae la tarde de a poco: los títulos salen con las primeras estrellas
            level.setDayTime(diaRecuerdo + (esFinal ? (long) Math.max(0.0D, (t - 13.0D) * 24.0D * 73.0D / Math.max(20.0D, duracion - 13.0D)) : 0L));
        }
    }

    // ------------------------------------------------------------------ fin

    private void terminar() {
        if (terminada) {
            return;
        }
        terminada = true;
        try {
            for (ISound s : sonidos) {
                mc.getSoundManager().stop(s);
            }
            cerrarRecuerdo();
            viz.gestoCliente(false);
            if (vizFalso) {
                level.removeEntity(viz.getId());
            }
        } catch (Exception e) {
            TrucoYa.LOGGER.warn("Truco Ya: problema al cerrar la cinemática", e);
        }
        restaurarAjustes();
        actual = null;
        salida = 0.0D;
        salidaNanos = Util.getNanos();
        avisarFin();
    }

    /** El mundo se descargó (desconexión, cambio de dimensión): solo se devuelven los ajustes. */
    private void abortar() {
        terminada = true;
        try {
            for (ISound s : sonidos) {
                mc.getSoundManager().stop(s);
            }
            if (filtro) {
                mc.gameRenderer.shutdownEffect();
            }
        } catch (Exception e) {
            com.trucoya.TrucoYa.LOGGER.debug("Truco Ya: no se pudo quitar el filtro de la cinemática", e);
        }
        restaurarAjustes();
        actual = null;
    }

    private void restaurarAjustes() {
        mc.options.hideGui = hudAntes;
        mc.options.setCameraType(vistaAntes);
        mc.options.renderClouds = nubesAntes;
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
            com.trucoya.TrucoYa.LOGGER.debug("Truco Ya: sin conexión para avisar el fin de la cinemática (el servidor lo resuelve por tiempo)", e);
        }
    }

    private static void fallo(Exception e) {
        TrucoYa.LOGGER.error("Truco Ya: la cinemática falló y se cortó (el juego sigue normal)", e);
        Cinematica c = actual;
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
        if (event.phase == TickEvent.Phase.START) {
            if (actual != null) {
                try {
                    actual.frame();
                } catch (Exception e) {
                    fallo(e);
                }
            }
            return;
        }
        try {
            if (actual != null) {
                Pantalla.dibujar(actual);
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
        Cinematica c = actual;
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
                    // se tragan mientras dura la función
                }
            }
            for (KeyBinding k : mc.options.keyHotbarSlots) {
                while (k.consumeClick()) {
                    // ídem
                }
            }
            if (mc.options.keyJump.isDown()) {
                c.salto += 0.05D;
                if (c.salto >= SALTO) {
                    c.terminar();
                    return;
                }
            } else {
                c.salto = Math.max(0.0D, c.salto - 0.1D);
            }
            if (c.escenario != null) {
                c.ticksRecuerdo++;
                c.escenario.particulas(c.t);
                c.sonidosDeAmbiente();
            }
        } catch (Exception e) {
            fallo(e);
        }
    }

    private void sonidosDeAmbiente() {
        int tick = ticksRecuerdo;
        if (esFinal) {
            if (tick % 26 == 0) {
                vanilla(SoundEvents.CAMPFIRE_CRACKLE, 0.6F, 0.9F + rnd.nextFloat() * 0.2F);
            }
            if (t > Escenario.LLEGAN_DESDE && t < Escenario.LLEGAN_HASTA && tick % 4 == 0) {
                double cerca = (t - Escenario.LLEGAN_DESDE) / (Escenario.LLEGAN_HASTA - Escenario.LLEGAN_DESDE);
                vanilla(SoundEvents.HORSE_GALLOP, (float) (0.15D + 0.5D * cerca), 0.85F + rnd.nextFloat() * 0.3F);
            }
            return;
        }
        if (t < 33.0D) {
            if (tick % 26 == 0) {
                vanilla(SoundEvents.CAMPFIRE_CRACKLE, 0.8F, 0.9F + rnd.nextFloat() * 0.2F);
            }
            if (tick % 90 == 45) {
                vanilla(SoundEvents.HORSE_BREATHE, 0.3F, 0.9F);
            }
        } else if (t < 57.9D) {
            if (t < Escenario.ARRANQUE) {
                if (tick % 35 == 0) {
                    vanilla(rnd.nextBoolean() ? SoundEvents.HORSE_BREATHE : SoundEvents.HORSE_AMBIENT, 0.4F, 0.9F + rnd.nextFloat() * 0.2F);
                }
            } else {
                boolean lenta = t >= Escenario.CAMARA_LENTA;
                if (tick % (lenta ? 9 : 3) == 0) {
                    float vol = (float) (0.35D + 0.5D * Escenario.galope(t));
                    vanilla(SoundEvents.HORSE_GALLOP, vol, lenta ? 0.5F + rnd.nextFloat() * 0.1F : 0.8F + rnd.nextFloat() * 0.4F);
                }
            }
        } else if (tick % 40 == 0) {
            vanilla(SoundEvents.FIRE_AMBIENT, 0.6F, 0.8F);
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
    public static void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (actual != null) {
            event.setRoll((float) actual.roll);
        }
    }

    @SubscribeEvent
    public static void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (actual != null) {
            event.setFOV(actual.fov);
        }
    }

    @SubscribeEvent
    public static void onFog(EntityViewRenderEvent.FogDensity event) {
        Cinematica c = actual;
        if (c != null && c.escenario != null) {
            float d = c.esFinal ? 0.016F : c.t < 33.0D ? 0.045F : c.t < 58.0D ? 0.03F : 0.05F;
            RenderSystem.fogMode(GlStateManager.FogMode.EXP2);
            event.setDensity(d);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(EntityViewRenderEvent.FogColors event) {
        Cinematica c = actual;
        if (c != null && c.escenario != null) {
            float r, g, b;
            if (c.esFinal) {
                r = 0.95F; g = 0.66F; b = 0.42F;          // atardecer
            } else if (c.t < 33.0D) {
                r = 0.09F; g = 0.08F; b = 0.07F;
            } else if (c.t < 58.0D) {
                r = 0.86F; g = 0.70F; b = 0.52F;
            } else {
                r = 0.55F; g = 0.50F; b = 0.45F;
            }
            event.setRed(r);
            event.setGreen(g);
            event.setBlue(b);
        }
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        ISound s = event.getSound();
        if (actual != null && s != null && s.getSource() == SoundCategory.MUSIC) {
            event.setResultSound(null);     // la música del juego espera
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
        Cinematica c = actual;
        if (c != null && event.getWorld() == c.level) {
            c.abortar();
        }
    }
}
