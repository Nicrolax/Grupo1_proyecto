package com.trucoya.client.cinematica;

import com.trucoya.entity.GauchoAspectos;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.entity.SeatEntity;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * El escenario del recuerdo: un campo criollo armado en el cielo (por encima del mundo real, que la niebla
 * esconde), con el fogón, la carga de caballería y lo que quedó después. Todo existe SOLO en el cliente
 * de quien mira: los bloques se anotan y se devuelven exactamente como estaban, y los actores son
 * entidades del cliente con ids negativos (el servidor nunca se entera).
 *
 * Coordenadas locales: x hacia donde cargan los jinetes (este), y = 0 el suelo, z al sur.
 */
@OnlyIn(Dist.CLIENT)
final class Escenario {

    // ------------------------------------------------------------------ medidas
    static final int X0 = -34, X1 = 33, Z0 = -26, Z1 = 25, ALTO = 16;
    /** Centro del fogón. */
    static final Vector3d FOGON = new Vector3d(-20, 0, -12);
    /** Pies del Negro Ledesma caído. */
    static final Vector3d CAIDO = new Vector3d(6, 0, 0.5);

    // ------------------------------------------------------------------ cinemática de la carga
    static final double ARRANQUE = 41.4D, VEL = 5.5D, LENTO = 0.35D, CAMARA_LENTA = 52.0D;
    static final double LIDER_X0 = -14.5D;

    /** Posición x de un jinete propio que empezó en {@code x0}. Entre planos se "reacomodan" (corte de cine). */
    static double xJinete(double x0, double t) {
        if (t < ARRANQUE) {
            return x0;
        }
        if (t < 47.0D) {
            double s = t - ARRANQUE;
            return x0 + (s < 1.2D ? VEL * s * s / 2.4D : VEL * (s - 0.6D));
        }
        if (t < CAMARA_LENTA) {
            return x0 - 4.0D + VEL * (t - 47.0D);
        }
        return x0 + 10.0D + VEL * LENTO * (t - CAMARA_LENTA);
    }

    static double xEnemigo(double x0, double t) {
        return x0 - VEL * LENTO * (t - CAMARA_LENTA);
    }

    /** Qué tan "a galope" va la caballada (0 parado, 1 a fondo). */
    static double galope(double t) {
        if (t < ARRANQUE) {
            return 0.0D;
        }
        return Math.min(1.0D, (t - ARRANQUE) / 1.2D);
    }

    static double escalaTiempo(double t) {
        return t >= CAMARA_LENTA ? LENTO : 1.0D;
    }

    // ------------------------------------------------------------------ estado

    enum Escena { NINGUNA, CAMPAMENTO, CARGA, SECUELA, FIESTA }

    private static final int ID_BASE = -1_600_000;

    final ClientWorld level;
    /** Bloque de suelo local (0, 0, 0). */
    final BlockPos base;
    /** Pies del punto local (0, 0, 0) en el mundo. */
    final Vector3d origen;
    private final Map<BlockPos, BlockState> originales = new LinkedHashMap<>();
    private final List<Entity> actores = new ArrayList<>();
    private final List<Jinete> jinetes = new ArrayList<>();
    private final Random rnd = new Random(1840L);
    private int proximoId;
    Escena escena = Escena.NINGUNA;

    // actores con nombre
    private GauchoEntity ledesma, joven, paisanoA, peonB;
    // la fiesta final: los cuatro fundadores llegan a caballo al fogón del Vizcacha
    private final GauchoEntity[] fundadores = new GauchoEntity[4];
    private final Caballo[] caballosFundadores = new Caballo[4];
    private boolean bajaron;
    /** El fantasma del Negro Ledesma (aparece un momento entre el humo, en la ronda). */
    private GauchoEntity fantasma;
    static final double LLEGAN_DESDE = 13.5D, LLEGAN_HASTA = 26.8D;
    /** Dónde queda parado cada fundador, alrededor del fuego (del lado del este). */
    static final Vector3d[] RONDA = {
        FOGON.add(2.3, 0, -2.3), FOGON.add(3.0, 0, -0.8), FOGON.add(3.0, 0, 0.8), FOGON.add(2.3, 0, 2.3)};

    /** Posición x de un fundador a caballo mientras llega (frena al final). */
    static double xLlegada(int i, double t) {
        double u = Math.max(0.0D, Math.min(1.0D, (t - LLEGAN_DESDE - i * 0.35D) / (LLEGAN_HASTA - LLEGAN_DESDE)));
        double frena = 1.0D - (1.0D - u) * (1.0D - u);      // llega rápido y frena despacio
        return RONDA[i].x + 26.0D - 22.5D * frena;
    }

    static double velLlegada(int i, double t) {
        double u = (t - LLEGAN_DESDE - i * 0.35D) / (LLEGAN_HASTA - LLEGAN_DESDE);
        return u <= 0 || u >= 1 ? 0.0D : Math.min(1.0D, 2.0D * (1.0D - u) + 0.15D);
    }

    /** La cara de un fundador (para las cámaras). */
    static Vector3d caraFundador(int i) {
        return RONDA[i].add(0, 1.62, 0);
    }
    private ItemEntity baraja;
    private Caballo suelto;
    private double carta0 = -1.0D;

    private Escenario(ClientWorld level, BlockPos base) {
        this.level = level;
        this.base = base;
        this.origen = new Vector3d(base.getX() + 0.5D, base.getY() + 1.0D, base.getZ() + 0.5D);
    }

    /** Busca, por encima del jugador, un hueco de aire donde armar el escenario. */
    static Escenario crear(ClientWorld level, BlockPos jugador) {
        int techo = level.getMaxBuildHeight() - ALTO - 2;
        int y0 = Math.min(techo, jugador.getY() + 50);
        for (int y = y0; y <= techo; y += 6) {
            if (libre(level, jugador.getX(), y, jugador.getZ())) {
                return new Escenario(level, new BlockPos(jugador.getX(), y, jugador.getZ()));
            }
        }
        // Sin hueco perfecto (montañas altísimas): se arma igual, pero solo donde hay aire.
        return new Escenario(level, new BlockPos(jugador.getX(), y0, jugador.getZ()));
    }

    private static boolean libre(ClientWorld level, int cx, int y, int cz) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int dy = -1; dy <= ALTO; dy++) {
                    m.set(cx + x, y + dy, cz + z);
                    if (!level.getBlockState(m).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    Vector3d mundo(Vector3d local) {
        return origen.add(local);
    }

    Vector3d mundo(double x, double y, double z) {
        return origen.add(x, y, z);
    }

    // ------------------------------------------------------------------ bloques

    private void poner(int x, int y, int z, BlockState s) {
        BlockPos p = base.offset(x, y, z);
        if (!originales.containsKey(p)) {
            BlockState antes = level.getBlockState(p);
            if (!antes.isAir()) {
                return;   // nunca se pisa un bloque real
            }
            originales.put(p, antes);
        }
        // 2: avisar al render · 16: sin actualizar vecinos (no hay físicas en el cliente)
        level.setBlock(p, s, 2 | 16);
    }

    /** Arma el campo: pasto, arroyo, el fogón con sus bancos, carpas, palenque y bandera. */
    void construir() {
        BlockState pasto = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState tierra = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState podzol = Blocks.PODZOL.defaultBlockState();
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                if (x >= 26 && x <= 28) {               // el Paso de las Ánimas: un arroyo que cruza el campo
                    poner(x, 0, z, Blocks.WATER.defaultBlockState());
                    poner(x, -1, z, Blocks.SAND.defaultBlockState());
                    continue;
                }
                double r = rnd.nextDouble();
                boolean orilla = x == 25 || x == 29;
                BlockState suelo = orilla ? (r < 0.5D ? Blocks.SAND.defaultBlockState() : tierra)
                        : r < 0.10D ? tierra : r < 0.14D ? podzol : pasto;
                poner(x, 0, z, suelo);
                boolean campamento = Math.abs(x - FOGON.x) < 7 && Math.abs(z - FOGON.z) < 7;
                double p = rnd.nextDouble();
                if (suelo == pasto) {
                    if (p < 0.30D) {
                        poner(x, 1, z, Blocks.GRASS.defaultBlockState());
                    } else if (p < 0.34D && !campamento) {
                        poner(x, 1, z, Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
                        poner(x, 2, z, Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                    } else if (p < 0.36D) {
                        poner(x, 1, z, Blocks.FERN.defaultBlockState());
                    }
                } else if (suelo == tierra && p < 0.2D) {
                    poner(x, 1, z, Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
        int fx = (int) FOGON.x, fz = (int) FOGON.z;
        // el fogón y sus piedras
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                poner(fx + dx, 1, fz + dz, Blocks.AIR.defaultBlockState());
            }
        }
        poner(fx, 1, fz, Blocks.CAMPFIRE.defaultBlockState());
        BlockState piedra = Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        poner(fx - 1, 1, fz - 1, piedra);
        poner(fx + 1, 1, fz - 1, piedra);
        poner(fx - 1, 1, fz + 1, piedra);
        poner(fx + 1, 1, fz + 1, Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
        // bancos de tronco alrededor
        BlockState troncoZ = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState troncoX = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int d = -1; d <= 1; d++) {
            poner(fx + 2, 1, fz + d, troncoZ);
            poner(fx - 2, 1, fz + d, troncoZ);
            poner(fx + d, 1, fz - 2, troncoX);
            poner(fx + d, 1, fz + 2, troncoX);
            poner(fx + 2, 2, fz + d, Blocks.AIR.defaultBlockState());
            poner(fx - 2, 2, fz + d, Blocks.AIR.defaultBlockState());
            poner(fx + d, 2, fz - 2, Blocks.AIR.defaultBlockState());
            poner(fx + d, 2, fz + 2, Blocks.AIR.defaultBlockState());
        }
        // carpas de lona
        carpa(-26, -21);
        carpa(-19, -22);
        // palenque con los caballos atados
        for (int z = -16; z <= -9; z++) {
            poner(-29, 1, z, Blocks.OAK_FENCE.defaultBlockState()
                    .setValue(FenceBlock.NORTH, z > -16).setValue(FenceBlock.SOUTH, z < -9));
        }
        poner(-23, 1, -8, Blocks.LIGHT_BLUE_BANNER.defaultBlockState());
        poner(-24, 1, -17, Blocks.BARREL.defaultBlockState());
        poner(-25, 1, -17, Blocks.HAY_BLOCK.defaultBlockState());
        poner(-17, 1, -17, Blocks.HAY_BLOCK.defaultBlockState());
    }

    /** Carpa de lona a dos aguas, con la cumbrera a lo largo de x. */
    private void carpa(int cx, int cz) {
        BlockState lona = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState sur = Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairsBlock.FACING, Direction.SOUTH);
        BlockState norte = Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairsBlock.FACING, Direction.NORTH);
        BlockState cumbre = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
        for (int x = cx; x <= cx + 3; x++) {
            poner(x, 1, cz - 2, sur);
            poner(x, 1, cz - 1, lona);
            poner(x, 1, cz, x == cx ? Blocks.AIR.defaultBlockState() : lona);   // la entrada
            poner(x, 1, cz + 1, lona);
            poner(x, 1, cz + 2, norte);
            poner(x, 2, cz - 1, sur);
            poner(x, 2, cz, lona);
            poner(x, 2, cz + 1, norte);
            poner(x, 3, cz, cumbre);
        }
    }

    /** Fuegos que quedaron ardiendo después de la carga. */
    void incendios() {
        int[][] fuegos = {{9, -2}, {3, 4}, {12, 6}, {-1, -3}, {15, -9}, {4, -9}, {17, 3}, {-4, 6}, {20, -4}, {8, 9}};
        for (int[] f : fuegos) {
            poner(f[0], 1, f[1], Blocks.FIRE.defaultBlockState());
        }
    }

    // ------------------------------------------------------------------ actores

    /** Caballo de la cinemática: ensillado y del pelaje que se le pida. */
    static final class Caballo extends HorseEntity {
        double fase;

        Caballo(World world, int pelaje) {
            super(EntityType.HORSE, world);
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("Variant", pelaje);
            this.readAdditionalSaveData(nbt);
            this.setFlag(4, true);     // ensillado
            this.setSilent(true);
        }
    }

    /** Un jinete con su caballo. */
    private static final class Jinete {
        final Caballo caballo;
        final GauchoEntity gaucho;
        final double x0, z0, fase0;
        final boolean enemigo;

        Jinete(Caballo caballo, GauchoEntity gaucho, double x0, double z0, double fase0, boolean enemigo) {
            this.caballo = caballo;
            this.gaucho = gaucho;
            this.x0 = x0;
            this.z0 = z0;
            this.fase0 = fase0;
            this.enemigo = enemigo;
        }
    }

    private <T extends Entity> T agregar(T e) {
        int id = ID_BASE - proximoId++;
        e.setId(id);
        level.putNonPlayerEntity(id, e);
        actores.add(e);
        return e;
    }

    private GauchoEntity gaucho(GauchoEntity.Rol rol, int variante, boolean armado, Vector3d local, float yaw) {
        GauchoEntity g = new GauchoEntity(ModEntities.GAUCHO.get(), level);
        g.prepararActor(rol, Math.floorMod(variante, GauchoAspectos.variantes(rol)), armado);
        Vector3d p = mundo(local);
        g.moveTo(p.x, p.y, p.z, yaw, 0.0F);
        g.setNoGravity(true);
        g.noPhysics = true;
        g.setSilent(true);
        colocar(g, p, yaw);
        return agregar(g);
    }

    private Caballo caballo(int pelaje, Vector3d local, float yaw) {
        Caballo c = new Caballo(level, pelaje);
        Vector3d p = mundo(local);
        c.moveTo(p.x, p.y, p.z, yaw, 0.0F);
        c.setNoGravity(true);
        c.noPhysics = true;
        colocar(c, p, yaw);
        return agregar(c);
    }

    private void sentar(GauchoEntity g, Vector3d local) {
        SeatEntity s = new SeatEntity(ModEntities.SEAT.get(), level);
        Vector3d p = mundo(local);
        s.setPos(p.x, p.y, p.z);
        agregar(s);
        g.startRiding(s, true);
    }

    /** Ubica una entidad en este cuadro exacto (sin interpolar con el tick anterior). */
    static void colocar(Entity e, Vector3d p, float yaw) {
        e.setPos(p.x, p.y, p.z);
        e.xo = e.xOld = p.x;
        e.yo = e.yOld = p.y;
        e.zo = e.zOld = p.z;
        e.yRot = e.yRotO = yaw;
        e.xRot = e.xRotO = 0.0F;
        if (e instanceof LivingEntity) {
            LivingEntity l = (LivingEntity) e;
            l.yBodyRot = l.yBodyRotO = yaw;
            l.yHeadRot = l.yHeadRotO = yaw;
        }
    }

    private static void animar(LivingEntity e, float velocidad, float fase) {
        e.animationSpeedOld = velocidad;
        e.animationSpeed = velocidad;
        e.animationPosition = fase;
    }

    private void limpiarActores() {
        for (int i = actores.size() - 1; i >= 0; i--) {
            Entity e = actores.get(i);
            e.stopRiding();
            e.ejectPassengers();
        }
        for (int i = actores.size() - 1; i >= 0; i--) {
            level.removeEntity(actores.get(i).getId());
        }
        actores.clear();
        jinetes.clear();
        ledesma = joven = paisanoA = peonB = null;
        java.util.Arrays.fill(fundadores, null);
        fantasma = null;
        java.util.Arrays.fill(caballosFundadores, null);
        bajaron = false;
        baraja = null;
        suelto = null;
    }

    private static float yawHacia(Vector3d desde, Vector3d hacia) {
        return (float) (Math.atan2(hacia.z - desde.z, hacia.x - desde.x) * (180.0D / Math.PI)) - 90.0F;
    }

    // ------------------------------------------------------------------ escenas

    /** La noche antes de la batalla: cuatro gauchos jóvenes juegan al truco junto al fuego. */
    void campamento() {
        limpiarActores();
        escena = Escena.CAMPAMENTO;
        double asiento = 0.82D;
        Vector3d f = FOGON;
        Vector3d pL = f.add(2, asiento, 0), pJ = f.add(-2, asiento, 0), pA = f.add(0, asiento, -2), pB = f.add(0, asiento, 2);
        ledesma = gaucho(GauchoEntity.Rol.MATRERO, 1, false, pL, yawHacia(pL, f));
        joven = gaucho(GauchoEntity.Rol.PEON, 0, false, pJ, yawHacia(pJ, f));
        paisanoA = gaucho(GauchoEntity.Rol.PAISANO, 2, false, pA, yawHacia(pA, f));
        peonB = gaucho(GauchoEntity.Rol.PEON, 3, false, pB, yawHacia(pB, f));
        sentar(ledesma, pL);
        sentar(joven, pJ);
        sentar(paisanoA, pA);
        sentar(peonB, pB);
        caballo(3, new Vector3d(-27.5, 0, -10.5), 90.0F);
        caballo(4 + 256, new Vector3d(-27.8, 0, -12.6), 90.0F);
        caballo(0, new Vector3d(-27.3, 0, -14.4), 90.0F);
    }

    /** Al alba: la caballada formada y, después, la carga. */
    void carga() {
        limpiarActores();
        escena = Escena.CARGA;
        double[] filas = {-16.0D, -18.5D, -21.0D};
        double[] columnas = {-6.0D, -3.0D, 0.0D, 3.0D, 6.0D};
        GauchoEntity.Rol[] roles = {GauchoEntity.Rol.PAISANO, GauchoEntity.Rol.PEON, GauchoEntity.Rol.MATRERO};
        for (int f = 0; f < filas.length; f++) {
            for (double z : columnas) {
                double x0 = filas[f] + (rnd.nextDouble() - 0.5D) * 0.8D;
                GauchoEntity.Rol rol = roles[rnd.nextInt(roles.length)];
                int variante = rnd.nextInt(8);
                int pelaje = 1 + rnd.nextInt(6) + 256 * rnd.nextInt(5);
                if (f == 0 && z == 0.0D) {               // el Vizcacha de joven, al frente, en su tordillo
                    x0 = LIDER_X0;
                    rol = GauchoEntity.Rol.PEON;
                    variante = 0;
                    pelaje = 0;
                } else if (f == 0 && z == 3.0D) {        // el Negro Ledesma, a su lado
                    rol = GauchoEntity.Rol.MATRERO;
                    variante = 1;
                    pelaje = 4;
                }
                jinete(rol, variante, pelaje, x0, z, false);
            }
        }
    }

    /** La otra caballada, que sale de la niebla del otro lado del arroyo. */
    void enemigos() {
        double[] zs = {-7.0D, -4.0D, -1.0D, 3.5D, 6.5D};
        for (int f = 0; f < 2; f++) {
            for (double z : zs) {
                double x0 = 19.5D + f * 2.6D + (rnd.nextDouble() - 0.5D) * 0.8D;
                jinete(GauchoEntity.Rol.MATRERO, rnd.nextInt(8), 4 + 256 * rnd.nextInt(2), x0, z + f * 0.8D, true);
            }
        }
    }

    private void jinete(GauchoEntity.Rol rol, int variante, int pelaje, double x0, double z0, boolean enemigo) {
        float yaw = enemigo ? 90.0F : -90.0F;
        Vector3d p = new Vector3d(x0, 0, z0);
        Caballo c = caballo(pelaje, p, yaw);
        GauchoEntity g = gaucho(rol, variante, true, p.add(0, 1.2, 0), yaw);
        g.startRiding(c, true);
        Jinete j = new Jinete(c, g, x0, z0, rnd.nextDouble() * 20.0D, enemigo);
        c.fase = j.fase0;
        jinetes.add(j);
        if (!enemigo && x0 == LIDER_X0) {
            joven = g;
        }
    }

    /** Lo que quedó: humo, fuegos, caídos, un caballo sin jinete. Y el Vizcacha junto a su amigo. */
    void secuela() {
        limpiarActores();
        escena = Escena.SECUELA;
        incendios();
        ledesma = gaucho(GauchoEntity.Rol.MATRERO, 1, false, CAIDO, 0.0F);
        ledesma.deathTime = 20;            // tendido de costado
        Vector3d pJ = CAIDO.add(0.9, -0.15, -1.3);
        joven = gaucho(GauchoEntity.Rol.PEON, 0, true, pJ, 0.0F);
        sentar(joven, pJ);
        GauchoEntity c1 = gaucho(GauchoEntity.Rol.PAISANO, 4, true, new Vector3d(11, 0, 3), 30.0F);
        c1.deathTime = 20;
        GauchoEntity c2 = gaucho(GauchoEntity.Rol.MATRERO, 3, true, new Vector3d(1.5, 0, -5), 200.0F);
        c2.deathTime = 20;
        suelto = caballo(4, new Vector3d(14, 0, -6), 135.0F);
        Caballo pastando = caballo(2 + 256, new Vector3d(2.5, 0, 6.5), 40.0F);
        pastando.setEating(true);
    }

    /** El final: el Vizcacha junto al fuego y los cuatro fundadores que llegan a caballo, al atardecer. */
    void fiesta() {
        limpiarActores();
        escena = Escena.FIESTA;
        Vector3d pV = FOGON.add(-2, 0.82, 0);
        GauchoEntity viejo = gaucho(GauchoEntity.Rol.VIZCACHA, 0, false, pV, yawHacia(pV, FOGON));
        sentar(viejo, pV);
        joven = viejo;                          // para los gestos: "joven" es el que habla de los dos
        int[] pelajes = {3, 0, 4, 2 + 256};
        for (int i = 0; i < 4; i++) {
            Vector3d p = new Vector3d(xLlegada(i, 0), 0, RONDA[i].z * 1.0D + (RONDA[i].z - FOGON.z) * 0.6D);
            caballosFundadores[i] = caballo(pelajes[i], p, 90.0F);
            fundadores[i] = gaucho(GauchoEntity.Rol.FUNDADOR, i, false, p.add(0, 1.2, 0), 90.0F);
            fundadores[i].startRiding(caballosFundadores[i], true);
        }
        caballo(1, new Vector3d(-27.5, 0, -10.5), 90.0F);
        Vector3d pF = FOGON.add(0.45, 0.15, 0.0);
        fantasma = gaucho(GauchoEntity.Rol.LEDESMA, 0, false, pF, yawHacia(pF, pV));
        fantasma.setDesvanecido(1.0F);
    }

    void gestoFundador(int i, boolean si) {
        if (i >= 0 && i < 4 && fundadores[i] != null) {
            fundadores[i].gestoCliente(si);
        }
    }

    /** La baraja aparece flotando delante de Ledesma cuando canta la flor (presagio). */
    void barajaEnElFogon() {
        quitarBaraja();
        baraja = agregar(new ItemEntity(level, 0, 0, 0, new ItemStack(ModItems.BARAJA_DE_ORO.get())));
        baraja.setNoGravity(true);
        baraja.noPhysics = true;
        baraja.setDeltaMovement(Vector3d.ZERO);
        colocar(baraja, mundo(FOGON.add(1.2, 1.25, 0)), 0.0F);
    }

    void quitarBaraja() {
        if (baraja != null) {
            level.removeEntity(baraja.getId());
            actores.remove(baraja);
            baraja = null;
        }
    }

    /** La entrega: la baraja pasa de la mano de Ledesma a la del Vizcacha. */
    void entregarBaraja(double t) {
        quitarBaraja();
        carta0 = t;
        baraja = agregar(new ItemEntity(level, 0, 0, 0, new ItemStack(ModItems.BARAJA_DE_ORO.get())));
        baraja.setNoGravity(true);
        baraja.noPhysics = true;
        baraja.setDeltaMovement(Vector3d.ZERO);
    }

    void rayo(Vector3d local) {
        LightningBoltEntity b = EntityType.LIGHTNING_BOLT.create(level);
        if (b == null) {
            return;
        }
        Vector3d p = mundo(local);
        b.moveTo(p.x, p.y, p.z);
        b.setVisualOnly(true);
        agregar(b);
    }

    void gesto(String quien, boolean si) {
        GauchoEntity g = "ledesma".equals(quien) ? ledesma : "joven".equals(quien) ? joven
                : "a".equals(quien) ? paisanoA : "b".equals(quien) ? peonB : null;
        if (g != null) {
            g.gestoCliente(si);
        }
    }

    void encabritar(boolean si) {
        for (Jinete j : jinetes) {
            if (j.gaucho == joven) {
                j.caballo.setStanding(si);
            }
        }
    }

    // ------------------------------------------------------------------ cuadro a cuadro

    /** Mueve a todos los actores al instante {@code t} (llamado en cada cuadro). */
    void actualizar(double t, double dt) {
        if (escena == Escena.FIESTA && fantasma != null) {
            double a = Pantalla.tramo(t, Cinematica.FANTASMA_DESDE, Cinematica.FANTASMA_DESDE + 1.8D,
                    Cinematica.FANTASMA_HASTA - 2.0D, Cinematica.FANTASMA_HASTA);
            fantasma.setDesvanecido((float) (1.0D - a));
            Vector3d pF = FOGON.add(0.45, 0.15 + 0.06 * Math.sin(t * 1.3D), 0.0);
            colocar(fantasma, mundo(pF), fantasma.yRot);
        }
        if (escena == Escena.FIESTA) {
            for (int i = 0; i < 4; i++) {
                Caballo c = caballosFundadores[i];
                GauchoEntity g = fundadores[i];
                if (c == null || g == null) continue;
                double z = RONDA[i].z + (RONDA[i].z - FOGON.z) * 0.6D;
                if (!bajaron) {
                    double v = velLlegada(i, t);
                    c.fase += dt * 20.0D * v;
                    colocar(c, mundo(xLlegada(i, t), 0, z), 90.0F);
                    animar(c, (float) v, (float) c.fase);
                    g.yBodyRot = g.yBodyRotO = 90.0F;
                    c.positionRider(g);
                    colocar(g, g.position(), 90.0F);
                } else {
                    // ya de a pie, en ronda, mirando al fuego (el caballo queda atrás)
                    colocar(c, mundo(RONDA[i].x + 2.6D, 0, z), 90.0F);
                    animar(c, 0.0F, (float) c.fase);
                    Vector3d p = RONDA[i];
                    colocar(g, mundo(p), yawHacia(p, FOGON));
                }
            }
            return;
        }
        if (escena == Escena.CARGA) {
            double g = galope(t);
            double esc = escalaTiempo(t);
            for (Jinete j : jinetes) {
                double x = j.enemigo ? xEnemigo(j.x0, t) : xJinete(j.x0, t);
                double z = j.z0 + (g > 0 ? 0.12D * Math.sin(t * 1.4D * esc + j.fase0) : 0.0D);
                float yaw = j.enemigo ? 90.0F : -90.0F;
                double gal = j.enemigo ? 1.0D : g;
                j.caballo.fase += dt * 20.0D * gal * esc;
                colocar(j.caballo, mundo(x, 0, z), yaw);
                animar(j.caballo, (float) gal, (float) j.caballo.fase);
                j.gaucho.yBodyRot = j.gaucho.yBodyRotO = yaw;
                j.caballo.positionRider(j.gaucho);
                colocar(j.gaucho, j.gaucho.position(), yaw);
            }
        } else if (escena == Escena.SECUELA) {
            if (suelto != null) {
                double u = Math.min(1.0D, Math.max(0.0D, (t - 58.0D) / 10.0D));
                Vector3d p = Toma.lerp(new Vector3d(14, 0, -6), new Vector3d(10.5, 0, -3.5), u);
                suelto.fase += dt * 20.0D * 0.35D;
                colocar(suelto, mundo(p), 125.0F);
                animar(suelto, 0.35F, (float) suelto.fase);
            }
            if (ledesma != null) {
                ledesma.deathTime = 20;
                ledesma.hurtTime = 0;
            }
            if (baraja != null && carta0 >= 0) {
                double u = Math.min(1.0D, (t - carta0) / 1.8D);
                double s = Toma.suave(u);
                Vector3d a = CAIDO.add(1.1, 0.35, 0.3), b = CAIDO.add(0.9, 0.62, -0.35);
                Vector3d p = Toma.lerp(a, b, s).add(0, Math.sin(Math.PI * s) * 0.45D + Math.max(0.0D, t - carta0 - 1.8D) * 0.06D, 0);
                colocar(baraja, mundo(p), 0.0F);
            }
        }
    }

    /** Se bajan del caballo y quedan en ronda alrededor del fuego. */
    void bajarse() {
        bajaron = true;
        for (GauchoEntity g : fundadores) {
            if (g != null) g.stopRiding();
        }
    }

    /** Partículas de cada tick (polvo, humo, chispas). */
    void particulas(double t) {
        if (escena == Escena.CAMPAMENTO || escena == Escena.FIESTA) {
            Vector3d f = mundo(FOGON.add(0, 0.4, 0));
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, f.x + (rnd.nextDouble() - 0.5D) * 0.4D, f.y + 0.3D,
                        f.z + (rnd.nextDouble() - 0.5D) * 0.4D, 0.0D, 0.05D, 0.0D);
            }
            if (rnd.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME, true, f.x + (rnd.nextDouble() - 0.5D) * 0.5D, f.y,
                        f.z + (rnd.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.02D, 0.0D);
            }
            if (rnd.nextInt(9) == 0) {
                level.addParticle(ParticleTypes.LAVA, true, f.x, f.y, f.z, 0.0D, 0.0D, 0.0D);
            }
            if (baraja != null) {
                Vector3d b = baraja.position();
                level.addParticle(ParticleTypes.END_ROD, true, b.x + (rnd.nextDouble() - 0.5D) * 0.6D, b.y + 0.2D + rnd.nextDouble() * 0.4D,
                        b.z + (rnd.nextDouble() - 0.5D) * 0.6D, 0.0D, 0.01D, 0.0D);
            }
        } else if (escena == Escena.CARGA) {
            double g = galope(t);
            BlockParticleData tierra = new BlockParticleData(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState());
            for (Jinete j : jinetes) {
                if ((j.enemigo ? 1.0D : g) <= 0.0D) {
                    continue;
                }
                Vector3d c = j.caballo.position();
                double atras = j.enemigo ? 0.8D : -0.8D;
                level.addParticle(tierra, true, c.x + atras + (rnd.nextDouble() - 0.5D) * 0.6D, c.y + 0.1D,
                        c.z + (rnd.nextDouble() - 0.5D) * 0.8D, 0.0D, 0.25D, 0.0D);
                if (rnd.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.CLOUD, true, c.x + atras * 1.5D, c.y + 0.2D,
                            c.z + (rnd.nextDouble() - 0.5D), atras * 0.05D, 0.02D, 0.0D);
                }
            }
        } else if (escena == Escena.SECUELA) {
            for (int i = 0; i < 3; i++) {
                double x = X0 + 6 + rnd.nextDouble() * (X1 - X0 - 12), z = Z0 + 6 + rnd.nextDouble() * (Z1 - Z0 - 12);
                Vector3d p = mundo(x, 0.2, z);
                level.addParticle(i == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.LARGE_SMOKE, true,
                        p.x, p.y, p.z, 0.0D, 0.03D, 0.0D);
            }
            Vector3d c = mundo(CAIDO);
            for (int i = 0; i < 4; i++) {
                level.addParticle(rnd.nextBoolean() ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, true,
                        c.x + (rnd.nextDouble() - 0.5D) * 16.0D, c.y + rnd.nextDouble() * 6.0D,
                        c.z + (rnd.nextDouble() - 0.5D) * 16.0D, 0.0D, 0.0D, 0.0D);
            }
            if (baraja != null) {
                Vector3d b = baraja.position();
                level.addParticle(ParticleTypes.END_ROD, true, b.x + (rnd.nextDouble() - 0.5D) * 0.5D, b.y + 0.2D + rnd.nextDouble() * 0.3D,
                        b.z + (rnd.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.015D, 0.0D);
            }
        }
    }

    /** El estallido dorado cuando el Vizcacha recibe la baraja. */
    void destelloBaraja() {
        if (baraja == null) {
            return;
        }
        Vector3d b = baraja.position();
        for (int i = 0; i < 60; i++) {
            level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, true, b.x, b.y + 0.25D, b.z,
                    (rnd.nextDouble() - 0.5D) * 0.6D, rnd.nextDouble() * 0.5D, (rnd.nextDouble() - 0.5D) * 0.6D);
        }
    }

    /** El choque de las dos caballadas. */
    void choque(double t) {
        double x = xJinete(LIDER_X0, t) + 1.5D;
        for (int i = 0; i < 14; i++) {
            Vector3d p = mundo(x + (rnd.nextDouble() - 0.5D) * 3.0D, 1.0D + rnd.nextDouble() * 1.5D, (rnd.nextDouble() - 0.5D) * 14.0D);
            level.addParticle(i % 2 == 0 ? ParticleTypes.EXPLOSION : ParticleTypes.POOF, true, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
        }
    }

    // ------------------------------------------------------------------ fin

    /** Saca a todos los actores y devuelve cada bloque a como estaba. */
    void restaurar() {
        limpiarActores();
        List<BlockPos> lista = new ArrayList<>(originales.keySet());
        for (int i = lista.size() - 1; i >= 0; i--) {
            BlockPos p = lista.get(i);
            level.setBlock(p, originales.get(p), 2 | 16);
        }
        originales.clear();
        escena = Escena.NINGUNA;
    }
}
