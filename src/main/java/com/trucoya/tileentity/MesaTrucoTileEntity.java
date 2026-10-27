package com.trucoya.tileentity;

import com.trucoya.TrucoYa;
import com.trucoya.block.AnotadorBlock;
import com.trucoya.block.SillaCriollaBlock;
import com.trucoya.core.ActionResult;
import com.trucoya.core.ActionType;
import com.trucoya.core.Card;
import com.trucoya.core.CardRules;
import com.trucoya.core.GameEvent;
import com.trucoya.core.MatchConfig;
import com.trucoya.core.PlayerView;
import com.trucoya.core.Prestigio;
import com.trucoya.core.Sena;
import com.trucoya.core.TableSnapshot;
import com.trucoya.core.TrucoBot;
import com.trucoya.core.TrucoMatch;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.entity.SeatEntity;
import com.trucoya.network.BubblePacket;
import com.trucoya.network.SenaNoticePacket;
import com.trucoya.network.TableViewPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.progreso.JugadorData;
import com.trucoya.progreso.Logros;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import com.trucoya.registry.ModSounds;
import com.trucoya.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Estado de la partida que se juega en una Mesa de Truco. Vive solo en el servidor: los
 * clientes reciben {@link TableSnapshot} filtrados por jugador y mandan intenciones que se
 * validan contra el motor ({@link TrucoMatch}).
 *
 * Asientos del motor: humanos (jugadores), gauchos (NPC con IA de su nivel) o paisanos
 * anónimos (IA nivel 2) para completar. Maneja también el pozo de apuestas (guardado en el
 * mundo, con devolución garantizada si la partida se suspende), el Prestigio Criollo y la
 * llegada de gauchos a la mesa.
 *
 * Las partidas en curso no se guardan en disco: si el servidor se reinicia o la zona se
 * descarga, la mesa queda libre (el pozo sí se conserva y se puede retirar).
 */
public class MesaTrucoTileEntity extends TileEntity implements ITickableTileEntity {

    /** Orden horario de las sillas: los compañeros (asientos k y k+2) quedan enfrentados. */
    private static final Direction[] CLOCKWISE = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final int ROUND_PAUSE_TICKS = 60;
    private static final int BOT_MIN_DELAY = 20;
    private static final int BOT_EXTRA_DELAY = 25;
    private static final int SENA_COOLDOWN = 40;
    private static final float SENA_CAUGHT_CHANCE = 0.2F;
    private static final double MAX_REACH_SQR = 8.0D * 8.0D;
    private static final int MAX_POT = 18;
    private static final int FORASTERO_WAIT = 200;
    private static final int ANON_BOT_LEVEL = 2;

    public static final ITag.INamedTag<Item> APUESTA_CLANDESTINA =
            ItemTags.createOptional(new ResourceLocation(TrucoYa.MOD_ID, "apuesta_clandestina"));

    private enum Kind { HUMAN, NPC, BOT }

    private final Random random = new Random();

    // Configuración (se guarda en el mundo).
    private int cfgPlayers = 2;
    private int cfgTarget = 30;
    private boolean cfgFlor = true;
    private boolean clandestina;

    // Partida actual.
    @Nullable
    private TrucoMatch match;
    private Kind[] seatKind = new Kind[0];
    private UUID[] seatId = new UUID[0];
    private String[] seatName = new String[0];
    private int[] seatLevel = new int[0];
    private TrucoBot[] bots = new TrucoBot[0];
    private final LinkedList<String> log = new LinkedList<>();
    private int lastCallSeat = -1;
    /** Tantos cantados en la mano actual (por asiento), para mostrarlos en el tapete. */
    private final int[] tantos = {-1, -1, -1, -1};
    private boolean tantosFlor;
    private int tantoGanador = -1;
    private String lastCallText = "";
    private boolean lastCallWasTruco;
    private int lastTrucoCaller = -1;
    /** Nivel mínimo de los gauchos rivales (el campeón de la región), 0 si no aplica. */
    private int campeon;
    private final int[] maxDeficit = new int[2];
    private final Map<UUID, Integer> lastSena = new HashMap<>();

    // Pozo de apuestas (se guarda en el mundo).
    private final List<ItemStack> potStacks = new ArrayList<>();
    private final List<UUID> potOwners = new ArrayList<>();
    private final List<String> potNames = new ArrayList<>();

    // Historia: región de la pulpería donde está la mesa (null: no es una pulpería), calculada una vez.
    @Nullable
    private com.trucoya.progreso.Historia.Region region;
    private boolean regionCalculada;
    // Torneo de la pulpería: tres rivales seguidos, partidos cortos a 20.
    public static final int TORNEO_RONDAS = 3;
    public static final int TORNEO_INSCRIPCION = 5;
    public static final int TORNEO_PREMIO = 20;
    @Nullable
    private UUID torneoJugador;
    private int torneoRonda;
    private int torneoProximoAt = -1;
    // Lo que se ve sobre la mesa en el mundo (las cartas jugadas, la muestra y el mazo): ver MesaTrucoRenderer.
    /** Silla (0-3: norte, este, sur, oeste) de cada asiento de la partida. */
    private int[] sillaDeAsiento = {0, 2, 1, 3};
    /** La baraja de la partida (la que eligió quien repartió). */
    private int baraja;
    private int vistaEnviada;
    // Del lado del cliente: la última vista que llegó del servidor.
    public final VistaMesa vista = new VistaMesa();

    // La Salamanca: la mesa del Mandinga (el jugador y el Vizcacha contra el Mandinga y Ledesma).
    private boolean salamanca;
    private boolean achicoHecho;
    private boolean rondaArmada;
    private int asientoVizcacha = -1, asientoLedesma = -1, asientoMandinga = -1;
    // Quienes le ganaron al Vizcacha en esta partida: al terminar ven la cinemática final.
    private final List<UUID> finalPara = new ArrayList<>();

    // Llegada de gauchos.
    @Nullable
    private UUID incomingNpc;
    private int incomingSince;
    private int forasteroAt = -1;
    private int idleNpcTicks;
    private int autoStartAt = -1;

    private int ticks;
    private int nextRoundAt = -1;
    private int botActAt = -1;
    private int endAt = -1;

    public MesaTrucoTileEntity() {
        super(ModTileEntities.MESA_TRUCO.get());
    }

    public void setClandestina(boolean value) {
        this.clandestina = value;
        setChanged();
    }

    public boolean isClandestina() {
        return clandestina;
    }

    // ------------------------------------------------------------------ sillas y ocupantes

    @Nullable
    private LivingEntity occupantAt(Direction side) {
        if (level == null) {
            return null;
        }
        BlockPos chair = worldPosition.relative(side);
        if (!(level.getBlockState(chair).getBlock() instanceof SillaCriollaBlock)) {
            return null;
        }
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, new AxisAlignedBB(chair))) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof ServerPlayerEntity || passenger instanceof GauchoEntity) {
                    return (LivingEntity) passenger;
                }
            }
        }
        return null;
    }

    private boolean chairExists(Direction side) {
        return level != null && level.getBlockState(worldPosition.relative(side)).getBlock() instanceof SillaCriollaBlock;
    }

    private int chairIndexOf(Entity e) {
        for (int i = 0; i < CLOCKWISE.length; i++) {
            if (occupantAt(CLOCKWISE[i]) == e) {
                return i;
            }
        }
        return -1;
    }

    public boolean isSeatedHere(ServerPlayerEntity player) {
        return chairIndexOf(player) >= 0;
    }

    private List<ServerPlayerEntity> seatedPlayers() {
        List<ServerPlayerEntity> out = new ArrayList<>(4);
        for (Direction d : CLOCKWISE) {
            LivingEntity e = occupantAt(d);
            if (e instanceof ServerPlayerEntity) out.add((ServerPlayerEntity) e);
        }
        return out;
    }

    private List<GauchoEntity> seatedGauchos() {
        List<GauchoEntity> out = new ArrayList<>(4);
        for (Direction d : CLOCKWISE) {
            LivingEntity e = occupantAt(d);
            if (e instanceof GauchoEntity) out.add((GauchoEntity) e);
        }
        return out;
    }

    /** Una silla libre, preferentemente la que está enfrente de {@code fromIndex}. */
    @Nullable
    private Direction freeChair(int fromIndex) {
        int[] order = fromIndex >= 0
                ? new int[] {(fromIndex + 2) % 4, (fromIndex + 1) % 4, (fromIndex + 3) % 4}
                : new int[] {0, 1, 2, 3};
        for (int i : order) {
            Direction d = CLOCKWISE[i];
            if (chairExists(d) && occupantAt(d) == null) {
                return d;
            }
        }
        return null;
    }

    private int seatOf(ServerPlayerEntity player) {
        for (int s = 0; s < seatId.length; s++) {
            if (seatKind[s] == Kind.HUMAN && player.getUUID().equals(seatId[s])) {
                return s;
            }
        }
        return -1;
    }

    private boolean isRunning() {
        return match != null && !match.isOver();
    }

    private boolean inReach(ServerPlayerEntity player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= MAX_REACH_SQR;
    }

    private static void tell(ServerPlayerEntity player, String text) {
        player.displayClientMessage(new StringTextComponent(text), true);
    }

    @Nullable
    private ServerPlayerEntity onlinePlayer(int seat) {
        if (seat < 0 || seat >= seatId.length || seatKind[seat] != Kind.HUMAN || seatId[seat] == null || level == null) {
            return null;
        }
        MinecraftServer server = level.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(seatId[seat]);
    }

    @Nullable
    private GauchoEntity npcEntity(int seat) {
        if (seat < 0 || seat >= seatId.length || seatKind[seat] != Kind.NPC || !(level instanceof ServerWorld)) {
            return null;
        }
        Entity e = ((ServerWorld) level).getEntity(seatId[seat]);
        return e instanceof GauchoEntity ? (GauchoEntity) e : null;
    }

    // ------------------------------------------------------------------ llegada de jugadores y gauchos

    public void onPlayerSat(ServerPlayerEntity player) {
        addLog(player.getGameProfile().getName() + " se sentó a la mesa.");
        if (!isRunning() && !seatedGauchos().isEmpty() && incomingNpc == null) {
            autoStartAt = ticks + 100;
            addLog("Hay un gaucho esperando: se reparte en 5 segundos (o tocá Repartir).");
        }
        broadcast(false);
        openFor(player);
        summonOpponent(player);
    }

    public void openFor(ServerPlayerEntity player) {
        send(player, true);
    }

    /** Al sentarse alguien: acude el gaucho más cercano; si no hay ninguno, puede venir un forastero. */
    private void summonOpponent(ServerPlayerEntity player) {
        if (isRunning() || level == null || incomingNpc != null || !seatedGauchos().isEmpty()) {
            return;
        }
        Direction chair = freeChair(chairIndexOf(player));
        if (chair == null) {
            return;
        }
        GauchoEntity best = null;
        double bestDist = Double.MAX_VALUE;
        boolean anyGaucho = false;
        for (GauchoEntity g : level.getEntitiesOfClass(GauchoEntity.class, new AxisAlignedBB(worldPosition).inflate(48.0D))) {
            anyGaucho = true;
            if (!g.isAvailable() || g.getRol() == GauchoEntity.Rol.FORASTERO) continue;
            if (g.getRol() == GauchoEntity.Rol.VIZCACHA && !com.trucoya.progreso.Historia.habilitadoVizcacha(player)) {
                g.say(com.trucoya.progreso.Historia.negativaVizcacha(player));
                continue;
            }
            double d = g.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            if (d < 40 * 40 && d < bestDist) {
                bestDist = d;
                best = g;
            }
        }
        if (best != null) {
            best.goSit(worldPosition.relative(chair), worldPosition);
            incomingNpc = best.getUUID();
            incomingSince = ticks;
            addLog(best.nombre() + " (Nv. " + best.getNivel() + ") se acerca a la mesa.");
            broadcast(false);
        } else if (!anyGaucho) {
            forasteroAt = ticks + FORASTERO_WAIT;
            tell(player, "Esperá un rato sentado... capaz que pasa algún forastero.");
        }
    }

    /**
     * Un jugador invitó a un gaucho a esta mesa. Devuelve lo que contesta el gaucho.
     */
    public String inviteNpc(GauchoEntity g, ServerPlayerEntity player) {
        if (isRunning()) {
            return "En esa mesa ya se está jugando. Esperá que terminen.";
        }
        if (incomingNpc != null) {
            return "Ya hay alguien yendo para la mesa.";
        }
        if (g.getRol() == GauchoEntity.Rol.VIZCACHA && !com.trucoya.progreso.Historia.habilitadoVizcacha(player)) {
            return com.trucoya.progreso.Historia.negativaVizcacha(player);
        }
        int playerChair = chairIndexOf(player);
        Direction chair = freeChair(playerChair);
        if (chair == null) {
            return "La mesa necesita otra Silla Criolla libre, pegada a la mesa.";
        }
        g.goSit(worldPosition.relative(chair), worldPosition);
        incomingNpc = g.getUUID();
        incomingSince = ticks;
        addLog(g.nombre() + " (Nv. " + g.getNivel() + ") acepta el desafío y va a la mesa.");
        broadcast(false);
        return playerChair >= 0 ? "¡Vamos! Ya me siento." : "¡Vamos! Sentate en la otra silla.";
    }

    private void spawnForastero(ServerPlayerEntity waiting) {
        spawnForastero(waiting, Prestigio.nivelForastero(JugadorData.nivel(waiting), random));
    }

    private void spawnForastero(ServerPlayerEntity waiting, int lvl) {
        if (!(level instanceof ServerWorld)) {
            return;
        }
        ServerWorld sw = (ServerWorld) level;
        Direction chair = freeChair(chairIndexOf(waiting));
        if (chair == null) {
            return;
        }
        BlockPos spot = null;
        for (int attempt = 0; attempt < 16 && spot == null; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = 20 + random.nextInt(8);
            int x = worldPosition.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = worldPosition.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int y = sw.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (sw.getFluidState(p.below()).isEmpty() && sw.getBlockState(p.below()).getMaterial().isSolid()
                    && Math.abs(y - worldPosition.getY()) < 8) {
                spot = p;
            }
        }
        if (spot == null) {
            return;
        }
        GauchoEntity g = ModEntities.GAUCHO.get().create(sw);
        HorseEntity horse = EntityType.HORSE.create(sw);
        if (g == null || horse == null) {
            return;
        }
        g.setRol(GauchoEntity.Rol.FORASTERO);
        g.setNivel(lvl);
        float yaw = random.nextFloat() * 360F;
        horse.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, yaw, 0F);
        horse.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
        horse.setTamed(true);
        horse.equipSaddle(null);
        horse.getAttribute(net.minecraft.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(64.0D);
        g.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, yaw, 0F);
        g.finalizeSpawn(sw, sw.getCurrentDifficultyAt(spot), SpawnReason.EVENT, null, null);
        g.initDefaults();
        sw.addFreshEntity(horse);
        sw.addFreshEntity(g);
        g.startRiding(horse, true);
        g.setHorse(horse.getUUID());
        g.goSit(worldPosition.relative(chair), worldPosition);
        incomingNpc = g.getUUID();
        incomingSince = ticks;
        addLog("Llega a caballo " + g.nombre() + " (Nv. " + lvl + "), un forastero.");
        for (ServerPlayerEntity p : seatedPlayers()) {
            tell(p, "Se acerca un gaucho forastero a caballo...");
        }
        broadcast(false);
    }

    // ------------------------------------------------------------------ inicio de partida

    public void requestStart(ServerPlayerEntity player, int players, int target, boolean flor) {
        if (!inReach(player) || !isSeatedHere(player)) {
            tell(player, "Tenés que estar sentado a esta mesa.");
            return;
        }
        if (isRunning()) {
            tell(player, "Ya hay una partida en juego en esta mesa.");
            return;
        }
        boolean enTorneo = torneoRonda > 0 && player.getUUID().equals(torneoJugador);
        if (enTorneo) {
            players = 2;
            target = 20;
        }
        boolean esSalamanca = com.trucoya.progreso.Salamanca.esMesa(level, worldPosition);
        if (esSalamanca) {
            if (!com.trucoya.progreso.Salamanca.esJugador(player) || seatedPlayers().size() != 1) {
                tell(player, "Esta es la mesa del Mandinga: juega solo quien vino con el Vizcacha.");
                return;
            }
            if (seatedPlayers().size() + seatedGauchos().size() < 4) {
                tell(player, "Esperá a que estén los cuatro sentados.");
                return;
            }
            players = 4;
            target = 30;
            flor = true;
        }
        if ((players != 2 && players != 4) || (target != 30 && target != 40 && !(enTorneo && target == 20))) {
            return;
        }
        List<ServerPlayerEntity> humans = seatedPlayers();
        for (GauchoEntity g : seatedGauchos()) {
            if (g.getRol() == GauchoEntity.Rol.VIZCACHA) {
                for (ServerPlayerEntity h : humans) {
                    if (!com.trucoya.progreso.Historia.habilitadoVizcacha(h)) {
                        tell(player, "El Viejo Vizcacha no se sienta con " + h.getGameProfile().getName() + ": "
                                + com.trucoya.progreso.Historia.negativaVizcacha(h));
                        g.say(com.trucoya.progreso.Historia.negativaVizcacha(h));
                        return;
                    }
                }
            }
        }
        if (players == 2 && humans.size() > 2) {
            tell(player, "Hay " + humans.size() + " sentados: elegí 2 contra 2.");
            return;
        }
        if (clandestina && JugadorData.nivel(player) < 3 && !consumeFicha(player)) {
            tell(player, "La guarida es para Nivel 3 de Prestigio o con una Ficha de Clandestina.");
            return;
        }
        if (!enTorneo) {
            cfgPlayers = players;
            cfgTarget = target;
            cfgFlor = flor;
            setChanged();
        }
        // En una pulpería, mientras alguien de la mesa no tenga el sello de la región, juega el campeón.
        com.trucoya.progreso.Historia.Region reg = region();
        campeon = 0;
        if (reg != null) {
            for (ServerPlayerEntity h : humans) {
                if (!com.trucoya.progreso.Historia.tieneSello(h, reg)) {
                    campeon = reg.nivelCampeon;
                }
            }
        }
        finalPara.clear();

        seatKind = new Kind[players];
        seatId = new UUID[players];
        seatName = new String[players];
        seatLevel = new int[players];
        bots = new TrucoBot[players];
        int start = chairIndexOf(player);
        if (players == 4) {
            for (int k = 0; k < 4; k++) {
                fillSeat(k, occupantAt(CLOCKWISE[(start + k) % 4]), k);
            }
        } else {
            fillSeat(0, player, 0);
            LivingEntity rival = null;
            for (ServerPlayerEntity p : humans) {
                if (p != player) rival = p;
            }
            if (rival == null) {
                for (GauchoEntity g : seatedGauchos()) {
                    rival = g;
                    break;
                }
            }
            fillSeat(1, rival, 1);
        }
        // en qué silla está cada asiento (para dibujar las cartas delante de cada uno)
        sillaDeAsiento = new int[players];
        boolean[] usada = new boolean[4];
        for (int s = 0; s < players; s++) {
            LivingEntity quien = seatKind[s] == Kind.HUMAN ? onlinePlayer(s) : npcEntity(s);
            int c = quien == null ? -1 : chairIndexOf(quien);
            sillaDeAsiento[s] = c;
            if (c >= 0) usada[c] = true;
        }
        for (int s = 0; s < players; s++) {
            if (sillaDeAsiento[s] < 0) {
                int pref = players == 2 ? (sillaDeAsiento[1 - s] + 2) % 4 : (start + s) % 4;
                for (int k = 0; k < 4 && (pref < 0 || usada[pref]); k++) pref = (Math.max(0, start) + s + k) % 4;
                sillaDeAsiento[s] = Math.max(0, pref);
                usada[sillaDeAsiento[s]] = true;
            }
        }
        baraja = com.trucoya.barajas.Coleccion.elegida(player).ordinal();
        if (!potStacks.isEmpty() && !preparePot()) {
            refundPot("No hay contra quién apostar: se devolvió el pozo.");
        }
        salamanca = esSalamanca;
        achicoHecho = false;
        rondaArmada = false;
        asientoVizcacha = asientoLedesma = asientoMandinga = -1;
        if (salamanca) {
            for (int s = 0; s < players; s++) {
                GauchoEntity g = npcEntity(s);
                if (g == null) continue;
                if (g.getRol() == GauchoEntity.Rol.VIZCACHA) asientoVizcacha = s;
                if (g.getRol() == GauchoEntity.Rol.LEDESMA) asientoLedesma = s;
                if (g.getRol() == GauchoEntity.Rol.MANDINGA) asientoMandinga = s;
            }
        }
        match = new TrucoMatch(new MatchConfig(players, target, flor), random);
        log.clear();
        lastCallSeat = -1;
        lastCallText = "";
        lastTrucoCaller = -1;
        maxDeficit[0] = 0;
        maxDeficit[1] = 0;
        nextRoundAt = -1;
        botActAt = -1;
        endAt = -1;
        forasteroAt = -1;
        incomingNpc = null;
        autoStartAt = -1;
        addLog((enTorneo ? "Torneo, ronda " + torneoRonda + " de " + TORNEO_RONDAS + " (partido a 20)" : "Partido a " + target)
                + (flor ? " con flor" : " sin flor") + ": " + teamName(0) + " contra " + teamName(1) + ".");
        if (campeon > 0) {
            addLog("Por el sello de " + reg.nombre + " juega el campeón de la región (Nv. " + campeon + ").");
        }
        if (!potStacks.isEmpty()) {
            addLog("Hay pozo en juego: " + potStacks.size() + " apuesta(s).");
        }
        match.start();
        if (salamanca) {
            addLog("La Salamanca: el Mandinga y el Negro Ledesma contra vos y el Viejo Vizcacha. Si ganan, el Negro es libre.");
            com.trucoya.progreso.Salamanca.partidaEmpezo(level, worldPosition);
            GauchoEntity m = npcEntity(asientoMandinga);
            if (m != null) m.say("Repartí, Vizcacha... que la noche es corta para el que pierde.");
        }
        sacarAnotadorDeArriba();
        updateAnotador();
        afterChange();
    }

    /** Un anotador arriba de la mesa (de mundos viejos) tapa las cartas: se levanta y queda en el piso. */
    private void sacarAnotadorDeArriba() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos up = worldPosition.above();
        if (level.getBlockState(up).getBlock() instanceof AnotadorBlock) {
            level.destroyBlock(up, true);
        }
    }

    private boolean consumeFicha(ServerPlayerEntity player) {
        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
            ItemStack s = player.inventory.getItem(i);
            if (s.getItem() == ModItems.FICHA_CLANDESTINA.get()) {
                s.shrink(1);
                tell(player, "Entregaste una Ficha de Clandestina.");
                return true;
            }
        }
        return false;
    }

    private static final String[] BOT_NAMES = {"Don Ramón", "El Tape", "Doña Rosa", "El Pardo"};

    private void fillSeat(int seat, @Nullable LivingEntity occupant, int botNumber) {
        if (occupant instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) occupant;
            seatKind[seat] = Kind.HUMAN;
            seatId[seat] = p.getUUID();
            seatName[seat] = p.getGameProfile().getName();
            seatLevel[seat] = JugadorData.nivel(p);
            bots[seat] = null;
        } else if (occupant instanceof GauchoEntity) {
            GauchoEntity g = (GauchoEntity) occupant;
            seatKind[seat] = Kind.NPC;
            seatId[seat] = g.getUUID();
            int nivel = Math.max(g.getNivel(), Math.min(5, campeon));
            seatName[seat] = g.nombre() + " (Nv. " + nivel + ")";
            seatLevel[seat] = nivel;
            // el Viejo Vizcacha juega un escalón por encima de todos (nivel 6 del bot)
            bots[seat] = new TrucoBot(new Random(random.nextLong()),
                    g.deLaSalamanca() ? com.trucoya.progreso.Salamanca.nivelBot(g, nivel)
                            : g.getRol() == GauchoEntity.Rol.VIZCACHA ? 6 : nivel);
        } else {
            seatKind[seat] = Kind.BOT;
            seatId[seat] = null;
            seatName[seat] = BOT_NAMES[botNumber % BOT_NAMES.length];
            seatLevel[seat] = ANON_BOT_LEVEL;
            bots[seat] = new TrucoBot(new Random(random.nextLong()), ANON_BOT_LEVEL);
        }
    }

    // ------------------------------------------------------------------ pozo de apuestas

    public void handleBet(ServerPlayerEntity player) {
        if (!inReach(player) || !isSeatedHere(player)) {
            tell(player, "Tenés que estar sentado a esta mesa.");
            return;
        }
        if (isRunning()) {
            tell(player, "Las apuestas se hacen antes de repartir.");
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            tell(player, "Tené en la mano lo que querés apostar.");
            return;
        }
        if (clandestina && !held.getItem().is(APUESTA_CLANDESTINA)) {
            tell(player, "En la guarida se apuesta fuerte: oro, esmeraldas, diamantes o netherite.");
            return;
        }
        if (potStacks.size() >= MAX_POT) {
            tell(player, "El pozo está lleno.");
            return;
        }
        ItemStack stake = held.copy();
        player.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        potStacks.add(stake);
        potOwners.add(player.getUUID());
        potNames.add(player.getGameProfile().getName());
        addLog(player.getGameProfile().getName() + " apuesta " + stake.getCount() + " " + stake.getHoverName().getString() + ".");
        setChanged();
        broadcast(false);
    }

    public void handleWithdraw(ServerPlayerEntity player) {
        if (!inReach(player)) {
            return;
        }
        if (isRunning()) {
            tell(player, "No se puede retirar la apuesta con la partida en juego.");
            return;
        }
        boolean any = false;
        for (int i = potStacks.size() - 1; i >= 0; i--) {
            if (player.getUUID().equals(potOwners.get(i))) {
                give(player, potStacks.get(i));
                removePot(i);
                any = true;
            }
        }
        if (any) {
            addLog(player.getGameProfile().getName() + " retiró su apuesta.");
            setChanged();
            broadcast(false);
        }
    }

    private void removePot(int i) {
        potStacks.remove(i);
        potOwners.remove(i);
        potNames.remove(i);
    }

    /**
     * Al empezar: si un equipo es solo de gauchos, el gaucho iguala lo apostado por los humanos
     * (hasta lo que le da su bolsa según el nivel). Devuelve false si no hay contra quién apostar.
     */
    private boolean preparePot() {
        boolean[] hasHuman = new boolean[2];
        boolean[] hasNpc = new boolean[2];
        int[] npcLevel = new int[2];
        for (int s = 0; s < seatKind.length; s++) {
            if (seatKind[s] == Kind.HUMAN) hasHuman[s % 2] = true;
            if (seatKind[s] == Kind.NPC) {
                hasNpc[s % 2] = true;
                npcLevel[s % 2] = Math.max(npcLevel[s % 2], seatLevel[s]);
            }
        }
        // Solo cuentan las apuestas de quienes juegan.
        for (int i = potStacks.size() - 1; i >= 0; i--) {
            int seat = -1;
            for (int s = 0; s < seatId.length; s++) {
                if (seatKind[s] == Kind.HUMAN && potOwners.get(i) != null && potOwners.get(i).equals(seatId[s])) seat = s;
            }
            if (seat < 0) {
                ServerPlayerEntity owner = level != null && level.getServer() != null && potOwners.get(i) != null
                        ? level.getServer().getPlayerList().getPlayer(potOwners.get(i)) : null;
                if (owner != null) give(owner, potStacks.get(i)); else dropAtTable(potStacks.get(i));
                removePot(i);
            }
        }
        if (potStacks.isEmpty()) {
            return true;
        }
        for (int team = 0; team < 2; team++) {
            int other = 1 - team;
            if (hasHuman[team] && !hasHuman[other]) {
                if (!hasNpc[other]) {
                    return false; // un paisano anónimo no apuesta
                }
                int cap = npcLevel[other] >= 5 ? 192 : 16 * npcLevel[other];
                int used = 0;
                int n = potStacks.size();
                for (int i = 0; i < n && used < cap; i++) {
                    ItemStack copy = potStacks.get(i).copy();
                    copy.setCount(Math.min(copy.getCount(), cap - used));
                    used += copy.getCount();
                    potStacks.add(copy);
                    potOwners.add(null);
                    potNames.add("Banca");
                }
                addLog(teamName(other) + " iguala la apuesta.");
            }
        }
        setChanged();
        return true;
    }

    private void payout(int winnerTeam) {
        if (potStacks.isEmpty()) {
            return;
        }
        List<ServerPlayerEntity> winners = new ArrayList<>();
        for (int s = 0; s < seatKind.length; s++) {
            if (s % 2 == winnerTeam) {
                ServerPlayerEntity p = onlinePlayer(s);
                if (p != null) winners.add(p);
            }
        }
        if (winners.isEmpty()) {
            addLog("El pozo se lo lleva " + teamName(winnerTeam) + ".");
        } else {
            for (int i = 0; i < potStacks.size(); i++) {
                give(winners.get(i % winners.size()), potStacks.get(i));
            }
            addLog("¡" + teamName(winnerTeam) + " se lleva el pozo!");
            boolean oro = false;
            for (ItemStack st : potStacks) {
                ResourceLocation id = st.getItem().getRegistryName();
                if (id != null && id.getPath().contains("gold")) oro = true;
            }
            for (ServerPlayerEntity p : winners) {
                Logros.pozoGanado(p, clandestina && oro);
            }
        }
        potStacks.clear();
        potOwners.clear();
        potNames.clear();
        setChanged();
    }

    /** Devuelve cada apuesta a su dueño (o la deja sobre la mesa si no está conectado). */
    private void refundPot(String reason) {
        if (potStacks.isEmpty()) {
            return;
        }
        MinecraftServer server = level != null ? level.getServer() : null;
        for (int i = 0; i < potStacks.size(); i++) {
            UUID owner = potOwners.get(i);
            if (owner == null) continue; // lo igualado por la banca no es de nadie
            ServerPlayerEntity p = server != null ? server.getPlayerList().getPlayer(owner) : null;
            if (p != null) give(p, potStacks.get(i)); else dropAtTable(potStacks.get(i));
        }
        potStacks.clear();
        potOwners.clear();
        potNames.clear();
        addLog(reason);
        setChanged();
    }

    private void give(ServerPlayerEntity player, ItemStack stack) {
        ItemStack s = stack.copy();
        player.inventory.add(s); // descuenta de 's' lo que entró
        if (!s.isEmpty()) {
            player.drop(s, false);
        }
    }

    private void dropAtTable(ItemStack stack) {
        if (level != null && !stack.isEmpty()) {
            InventoryHelper.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D,
                    worldPosition.getZ() + 0.5D, stack.copy());
        }
    }

    // ------------------------------------------------------------------ acciones de juego

    public void handleAction(ServerPlayerEntity player, ActionType action) {
        int seat = validSeat(player);
        if (seat < 0 || action == null || action == ActionType.PLAY_CARD) {
            return;
        }
        ActionResult r = match.act(seat, action);
        if (!r.isSuccess()) {
            tell(player, r.getReason());
            send(player, false);
            return;
        }
        afterChange();
    }

    public void handleCard(ServerPlayerEntity player, int cardId) {
        int seat = validSeat(player);
        if (seat < 0) {
            return;
        }
        ActionResult r = match.playCardById(seat, cardId);
        if (!r.isSuccess()) {
            tell(player, r.getReason());
            send(player, false);
            return;
        }
        afterChange();
    }

    public void handleSena(ServerPlayerEntity player, Sena sena) {
        int seat = validSeat(player);
        if (seat < 0 || sena == null) {
            return;
        }
        if (match.getConfig().getPlayers() != 4) {
            tell(player, "Las señas son para jugar en pareja (2 contra 2).");
            return;
        }
        Integer last = lastSena.get(player.getUUID());
        if (last != null && ticks - last < SENA_COOLDOWN) {
            return;
        }
        lastSena.put(player.getUUID(), ticks);
        String who = seatName[seat];
        int partner = (seat + 2) % 4;
        ServerPlayerEntity mate = onlinePlayer(partner);
        if (mate != null) {
            TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> mate),
                    new SenaNoticePacket(who + " te hace una seña: " + sena.getGesture().toLowerCase(java.util.Locale.ROOT)
                            + " (" + sena.getMeaning() + ")", true));
        } else if (bots[partner] != null) {
            bots[partner].learnSena(seat, sena, match.getMuestra()); // el compañero gaucho la entiende
        }
        tell(player, "Seña hecha: " + sena.getGesture().toLowerCase(java.util.Locale.ROOT) + ".");
        for (int s = 0; s < seatKind.length; s++) {
            if (s % 2 == seat % 2) continue;
            // con Confraternidad (mate compartido) las señas salen más disimuladas
            float disimulo = com.trucoya.fase5.Fase5.tieneConfraternidad(player) ? 0.5F : 1.0F;
            if (seatKind[s] == Kind.HUMAN) {
                if (random.nextFloat() < SENA_CAUGHT_CHANCE * disimulo) {
                    ServerPlayerEntity rival = onlinePlayer(s);
                    if (rival != null) {
                        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> rival),
                                new SenaNoticePacket("Te pareció ver que " + who + " hizo un gesto: "
                                        + sena.getGesture().toLowerCase(java.util.Locale.ROOT) + ".", false));
                    }
                }
            } else if (bots[s] != null) {
                int lvl = bots[s].getLevel();
                float chance = (lvl >= 6 ? 0.8F : lvl >= 5 ? 0.6F : lvl == 4 ? 0.3F : 0.05F) * disimulo;   // al Vizcacha no se le escapa casi nada
                if (random.nextFloat() < chance) {
                    bots[s].learnSena(seat, sena, match.getMuestra());
                    if (lvl >= 4 && seatKind[s] == Kind.NPC && random.nextFloat() < 0.5F) {
                        GauchoEntity g = npcEntity(s);
                        if (g != null) g.say("Ojo, que te vi la seña...");
                    }
                }
            }
        }
    }

    /** Asiento del jugador si puede actuar en esta mesa, o -1. */
    private int validSeat(ServerPlayerEntity player) {
        if (!isRunning() || !inReach(player)) {
            return -1;
        }
        int seat = seatOf(player);
        if (seat < 0) {
            tell(player, "No estás jugando en esta mesa.");
            return -1;
        }
        if (!isSeatedHere(player)) {
            tell(player, "Tenés que estar sentado para jugar.");
            return -1;
        }
        return seat;
    }

    /** Termina la partida en curso (la mesa se rompió o no quedan jugadores). Devuelve el pozo. */
    public void abort(String reason) {
        if (salamanca && match != null && !match.isOver()) {
            com.trucoya.progreso.Salamanca.partidaAbandonada(level, worldPosition);
        }
        salamanca = false;
        refundPot("Se devolvieron las apuestas.");
        cancelarTorneo(reason);
        if (match == null) {
            return;
        }
        standUpNpcs();
        match = null;
        seatKind = new Kind[0];
        seatId = new UUID[0];
        seatName = new String[0];
        seatLevel = new int[0];
        bots = new TrucoBot[0];
        nextRoundAt = -1;
        botActAt = -1;
        endAt = -1;
        addLog("Partida suspendida: " + reason);
        broadcast(false);
        sincronizarVista();
    }

    private void standUpNpcs() {
        // los de la Salamanca se levantan cuando lo dice la historia (progreso.Salamanca)
        for (GauchoEntity g : seatedGauchos()) {
            if (!g.deLaSalamanca()) g.standUp();
        }
        for (int s = 0; s < seatKind.length; s++) {
            GauchoEntity g = npcEntity(s);
            if (g != null && g.isPassenger() && !g.deLaSalamanca()) g.standUp();
        }
    }

    // ------------------------------------------------------------------ ciclo

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        ticks++;
        tickArrivals();
        tickTorneo();
        if (match == null) {
            return;
        }
        if (match.isOver()) {
            if (endAt >= 0 && ticks >= endAt) {
                endAt = -1;
                standUpNpcs();
                broadcast(false);
                for (UUID id : finalPara) {
                    ServerPlayerEntity fp = level.getServer() != null ? level.getServer().getPlayerList().getPlayer(id) : null;
                    if (fp != null) {
                        com.trucoya.progreso.CinematicaServidor.iniciarFinal(fp);
                    }
                }
                finalPara.clear();
            }
            return;
        }
        if (ticks % 20 == 0) {
            checkPlayersPresent();
            if (match == null) {
                return;
            }
        }
        if (match.isRoundFinished()) {
            if (nextRoundAt < 0) {
                nextRoundAt = ticks + ROUND_PAUSE_TICKS;
            } else if (ticks >= nextRoundAt) {
                nextRoundAt = -1;
                empezarRonda();
                afterChange();
            }
            return;
        }
        if (botActAt < 0) {
            scheduleBot();
        } else if (ticks >= botActAt) {
            botActAt = -1;
            for (int s = 0; s < bots.length; s++) {
                if (bots[s] != null && !match.legalActions(s).isEmpty()) {
                    if (!jugadaDeLaSalamanca(s)) {
                        bots[s].play(match, s);
                    }
                    afterChange();
                    break;
                }
            }
        }
    }

    private void tickArrivals() {
        if (incomingNpc != null) {
            boolean arrived = false;
            for (GauchoEntity g : seatedGauchos()) {
                if (g.getUUID().equals(incomingNpc)) arrived = true;
            }
            if (arrived) {
                incomingNpc = null;
                if (!isRunning() && !seatedPlayers().isEmpty()) {
                    autoStartAt = ticks + 100;
                    addLog("¡Están todos sentados! Se reparte en 5 segundos (o tocá Repartir).");
                }
                broadcast(false);
            } else if (ticks - incomingSince > 1200) {
                incomingNpc = null;
            }
        }
        if (autoStartAt >= 0 && ticks >= autoStartAt) {
            autoStartAt = -1;
            List<ServerPlayerEntity> humans = seatedPlayers();
            if (!isRunning() && !humans.isEmpty() && !seatedGauchos().isEmpty()) {
                int occupants = humans.size() + seatedGauchos().size();
                requestStart(humans.get(0), occupants >= 3 || cfgPlayers == 4 && occupants == 4 ? 4 : 2, cfgTarget, cfgFlor);
            }
        }
        if (forasteroAt >= 0 && ticks >= forasteroAt) {
            forasteroAt = -1;
            List<ServerPlayerEntity> humans = seatedPlayers();
            if (!isRunning() && !humans.isEmpty() && seatedGauchos().isEmpty() && incomingNpc == null) {
                spawnForastero(humans.get(0));
            }
        }
        if (ticks % 20 == 0 && !isRunning()) {
            boolean humansHere = !seatedPlayers().isEmpty();
            if (!humansHere) {
                forasteroAt = -1;
            }
            if (!humansHere && !seatedGauchos().isEmpty()) {
                idleNpcTicks += 20;
                if (idleNpcTicks >= 300) {
                    idleNpcTicks = 0;
                    standUpNpcs();
                }
            } else {
                idleNpcTicks = 0;
            }
        }
    }

    private void scheduleBot() {
        if (!isRunning() || match.isRoundFinished()) {
            return;
        }
        for (int s = 0; s < bots.length; s++) {
            if (bots[s] != null && !match.legalActions(s).isEmpty()) {
                botActAt = ticks + BOT_MIN_DELAY + random.nextInt(BOT_EXTRA_DELAY);
                return;
            }
        }
    }

    private void checkPlayersPresent() {
        boolean changed = false;
        int humans = 0;
        for (int s = 0; s < seatKind.length; s++) {
            if (seatKind[s] != Kind.HUMAN) {
                continue;
            }
            ServerPlayerEntity p = onlinePlayer(s);
            if (p == null || !isSeatedHere(p)) {
                if (salamanca) {
                    GauchoEntity m = npcEntity(asientoMandinga);
                    if (m != null) m.say("¿Te levantás de mi mesa? Entonces perdiste.");
                    com.trucoya.progreso.Salamanca.partidaAbandonada(level, worldPosition);
                    abort("se levantaron de la mesa del Mandinga.");
                    return;
                }
                addLog(seatName[s] + " se levantó; lo reemplaza un paisano.");
                seatKind[s] = Kind.BOT;
                seatName[s] = seatName[s] + " (paisano)";
                seatId[s] = null;
                bots[s] = new TrucoBot(new Random(random.nextLong()), ANON_BOT_LEVEL);
                changed = true;
            } else {
                humans++;
            }
        }
        if (humans == 0) {
            abort("todos se levantaron de la mesa.");
            return;
        }
        if (changed) {
            botActAt = -1;
            afterChange();
        }
    }

    private void afterChange() {
        processEvents();
        botActAt = -1;
        scheduleBot();
        broadcast(false);
    }

    private void processEvents() {
        if (match == null) {
            return;
        }
        for (GameEvent e : match.drainEvents()) {
            for (TrucoBot b : bots) {
                if (b != null) b.observe(e, match);
            }
            switch (e.getType()) {
                case ROUND_STARTED:
                    play(ModSounds.CARD_SHUFFLE.get(), 0.8F);
                    lastCallSeat = -1;
                    lastCallText = "";
                    lastTrucoCaller = -1;
                    java.util.Arrays.fill(tantos, -1);
                    tantosFlor = false;
                    tantoGanador = -1;
                    addLog("Reparte " + nameOf(e.getValue()) + ". Muestra: " + e.getCard() + ".");
                    npcPartnerSenas();
                    if (salamanca) senaDeLedesma();
                    break;
                case CARD_PLAYED:
                    play(ModSounds.CARD_PLAY.get(), 0.9F);
                    break;
                case CALL:
                    onCall(e.getSeat(), e.getAction());
                    break;
                case TRICK_RESOLVED:
                    addLog("Baza " + (e.getTrick() + 1) + ": " + (e.getTeam() < 0 ? "parda" : "la gana " + nameOf(e.getSeat())) + ".");
                    break;
                case TANTO_DECLARED:
                    if (e.getSeat() >= 0 && e.getSeat() < 4) {
                        tantos[e.getSeat()] = e.getValue();
                        tantosFlor = e.getAction() == ActionType.FLOR;
                        bubble(e.getSeat(), e.getValue() == com.trucoya.core.GameEvent.SON_BUENAS ? "Son buenas."
                                : (tantosFlor ? "Flor de " : "Tengo ") + e.getValue() + ".");
                    }
                    break;
                case ENVIDO_RESOLVED:
                    tantoGanador = e.getSeat();
                    diarioEquipo(e.getSeat() % 2, com.trucoya.progreso.Diario.ENVIDO);
                    addLog("Envido: " + tantosTexto() + " — gana " + nameOf(e.getSeat()) + ".");
                    bubble(e.getSeat(), "¡" + e.getValue() + "! Me lo llevo.");
                    break;
                case FLOR_RESOLVED:
                    if (e.getValue() >= 0) {
                        tantoGanador = e.getSeat();
                    }
                    addLog(e.getValue() >= 0
                            ? "Flor: " + tantosTexto() + " — gana " + nameOf(e.getSeat()) + "."
                            : "Flor de " + nameOf(e.getSeat()) + ".");
                    break;
                case POINTS_AWARDED:
                    play(ModSounds.POROTO.get(), 0.7F);
                    // el nivel del truco de esta mano (los puntos pueden venir recortados al final del partido)
                    if ("Bazas".equals(e.getText()) && match.getTrucoLevel() >= 2) {
                        diarioEquipo(e.getTeam(), match.getTrucoLevel() >= 3 ? com.trucoya.progreso.Diario.VALE_CUATRO
                                : com.trucoya.progreso.Diario.RETRUCO);
                    }
                    addLog("+" + e.getPoints() + " para " + teamName(e.getTeam()) + " (" + e.getText() + ").");
                    if (e.getText() != null && e.getText().startsWith("Contraflor al resto (")) {
                        for (int s = 0; s < seatKind.length; s++) {
                            ServerPlayerEntity hp = onlinePlayer(s);
                            if (hp != null && s % 2 == e.getTeam()) Logros.contraflorAlResto(hp);
                        }
                    }
                    updateAnotador();
                    for (int t = 0; t < 2; t++) {
                        maxDeficit[t] = Math.max(maxDeficit[t], match.getScore(1 - t) - match.getScore(t));
                    }
                    break;
                case STAGE_CHANGED:
                    addLog(teamName(e.getTeam()) + (seatName.length == 4 ? " entran" : " entra") + " en las buenas.");
                    break;
                case MATCH_ENDED:
                    addLog("¡Ganó el partido " + teamName(e.getTeam()) + "!");
                    nextRoundAt = -1;
                    endAt = ticks + 100;
                    settleMatch(e.getTeam());
                    break;
                default:
                    break;
            }
        }
    }

    /** "Vos 28 · El Viejo Vizcacha 31" con los tantos cantados en la mano. */
    private String tantosTexto() {
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < tantos.length; s++) {
            if (tantos[s] >= 0 || tantos[s] == com.trucoya.core.GameEvent.SON_BUENAS) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(nameOf(s)).append(' ').append(tantos[s] >= 0 ? String.valueOf(tantos[s]) : "son buenas");
            }
        }
        return sb.toString();
    }

    private void onCall(int seat, ActionType action) {
        // la voz de quien canta: el jugador elige la suya con /truco voz; los gauchos tienen la propia
        com.trucoya.core.Voz voz = com.trucoya.core.Voz.HOMBRE;
        net.minecraft.entity.Entity quien = null;
        ServerPlayerEntity hp = seatKind[seat] == Kind.HUMAN ? onlinePlayer(seat) : null;
        GauchoEntity g = seatKind[seat] == Kind.NPC ? npcEntity(seat) : null;
        if (hp != null) {
            voz = com.trucoya.core.Voz.porNumero(JugadorData.get(hp, JugadorData.VOZ));
            quien = hp;
        } else if (g != null) {
            voz = g.getVoz();
            quien = g;
        }
        SoundEvent voice = ModSounds.voice(action, voz);
        if (voice != null && level != null) {
            if (quien != null) {
                level.playSound(null, quien.getX(), quien.getY() + 1.4D, quien.getZ(), voice, SoundCategory.PLAYERS,
                        1.0F, 0.97F + random.nextFloat() * 0.06F);
            } else {
                play(voice, 1.0F);
            }
        }
        lastCallSeat = seat;
        lastCallText = action.getSpeech();
        addLog(nameOf(seat) + ": " + action.getSpeech());
        bubble(seat, action.getSpeech());

        if (action.isTrucoCall()) {
            lastTrucoCaller = seat;
            lastCallWasTruco = true;
        } else if (action.isEnvidoCall() || action == ActionType.FLOR) {
            lastCallWasTruco = false;
        }
        ServerPlayerEntity p = onlinePlayer(seat);
        if (p != null && action == ActionType.FLOR) {
            JugadorData.increment(p, JugadorData.FLORES);
            JugadorData.addPrestigio(p, Prestigio.FLOR, "Flor");
            Logros.flor(p);
            com.trucoya.progreso.Pedidos.evento(p, com.trucoya.progreso.Pedidos.RETO_FLOR, null);
            if (!salamanca) com.trucoya.progreso.Diario.evento(p, com.trucoya.progreso.Diario.FLOR);
        }
        // Farol: un humano cantó truco con mano floja y el rival no quiso.
        if ((action == ActionType.NO_QUIERO || action == ActionType.ME_VOY_AL_MAZO) && lastCallWasTruco
                && lastTrucoCaller >= 0 && lastTrucoCaller % 2 != seat % 2) {
            ServerPlayerEntity caller = onlinePlayer(lastTrucoCaller);
            if (caller != null && TrucoBot.strengthOf(match.getDealtHand(lastTrucoCaller), match.getRules()) < 0.45) {
                JugadorData.increment(caller, JugadorData.FAROLES);
                JugadorData.addPrestigio(caller, Prestigio.FAROL, "Puro cuento");
                Logros.farol(caller);
                if (!salamanca) com.trucoya.progreso.Diario.evento(caller, com.trucoya.progreso.Diario.FAROL);
            }
        }
    }

    /** Un gaucho compañero de un humano le pasa una seña sobre su mano al empezar la ronda. */
    private void npcPartnerSenas() {
        if (match == null || match.getConfig().getPlayers() != 4) {
            return;
        }
        for (int s = 0; s < seatKind.length; s++) {
            if (seatKind[s] == Kind.HUMAN) continue;
            ServerPlayerEntity mate = onlinePlayer((s + 2) % 4);
            if (mate == null || random.nextFloat() > 0.7F) continue;
            Sena sena = senaFor(match.getDealtHand(s), match.getRules());
            String text = seatName[s] + " te hace una seña: " + sena.getGesture().toLowerCase(java.util.Locale.ROOT) + " (" + sena.getMeaning() + ")";
            TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> mate), new SenaNoticePacket(text, true));
        }
    }

    private static Sena senaFor(List<Card> hand, CardRules rules) {
        if (rules.hasFlor(hand)) return Sena.FLOR;
        int bestRank = -1;
        Card best = null;
        for (Card c : hand) {
            int r = rules.trickRank(c);
            if (r > bestRank) {
                bestRank = r;
                best = c;
            }
        }
        if (best == null) return Sena.NADA;
        switch (rules.piezaRole(best)) {
            case 2: return Sena.DOS;
            case 4: return Sena.CUATRO;
            case 5: return Sena.CINCO;
            case 11: return Sena.CABALLO;
            case 10: return Sena.SOTA;
            default: break;
        }
        switch (bestRank) {
            case 14: return Sena.ANCHO_ESPADA;
            case 13: return Sena.ANCHO_BASTO;
            case 12: return Sena.SIETE_ESPADA;
            case 11: return Sena.SIETE_ORO;
            case 10: return Sena.TRES;
            default: return Sena.NADA;
        }
    }

    /** Fin del partido: prestigio, estadísticas, trofeo del Vizcacha, comentario de los gauchos y pozo. */
    // ------------------------------------------------------------------ la Salamanca

    /**
     * La próxima ronda. En la Salamanca, cuando al jugador y al Vizcacha les faltan 3 puntos o menos, el Negro
     * Ledesma y el Vizcacha reciben flor: el Negro contesta "con flor me achico" y les regala esos 3 puntos.
     */
    private void empezarRonda() {
        if (salamanca && !achicoHecho && asientoVizcacha >= 0 && asientoLedesma >= 0
                && match.getScore(0) >= match.getConfig().getTargetScore() - 3) {
            int repartidor = (match.getDealer() + 1) % match.getConfig().getPlayers();
            java.util.Set<Integer> conFlor = new java.util.HashSet<>(java.util.Arrays.asList(asientoVizcacha, asientoLedesma));
            if (match.startRound(repartidor, com.trucoya.core.Deck.withFlor(repartidor, 4, conFlor, random)).isSuccess()) {
                rondaArmada = true;
                return;
            }
        }
        rondaArmada = false;
        match.startNextRound();
    }

    /** Si en esta jugada manda la historia (la última mano de la Salamanca), la hace y devuelve true. */
    private boolean jugadaDeLaSalamanca(int s) {
        if (!salamanca || !rondaArmada || achicoHecho) {
            return false;
        }
        List<ActionType> legal = match.legalActions(s);
        if (s == asientoLedesma && legal.contains(ActionType.CON_FLOR_ME_ACHICO)) {
            GauchoEntity l = npcEntity(asientoLedesma);
            GauchoEntity m = npcEntity(asientoMandinga);
            match.act(s, ActionType.CON_FLOR_ME_ACHICO);
            achicoHecho = true;
            if (l != null) l.say("Con flor... me achico.");
            if (m != null) m.say("¡¿QUÉ HACÉS, NEGRO?! ¡Tenías la mejor flor de la mesa!");
            for (int k = 0; k < seatKind.length; k++) {
                ServerPlayerEntity hp = onlinePlayer(k);
                if (hp != null) {
                    hp.sendMessage(new StringTextComponent("El Negro Ledesma tenía flor... y se achicó.")
                            .withStyle(net.minecraft.util.text.TextFormatting.AQUA), hp.getUUID());
                }
            }
            return true;
        }
        if (s == asientoVizcacha && legal.contains(ActionType.CONTRAFLOR)) {
            match.act(s, ActionType.CONTRAFLOR);
            return true;
        }
        return false;
    }

    /** El Negro Ledesma juega para el Mandinga, pero a escondidas le hace señas al jugador. */
    private void senaDeLedesma() {
        if (asientoLedesma < 0 || random.nextFloat() > 0.4F || rondaArmada) {
            return;
        }
        ServerPlayerEntity jugador = onlinePlayer(0);
        if (jugador == null) {
            return;
        }
        Sena sena = senaFor(match.getDealtHand(asientoLedesma), match.getRules());
        String text = "El Negro Ledesma te hace una seña a escondidas: " + sena.getGesture().toLowerCase(java.util.Locale.ROOT)
                + " (" + sena.getMeaning() + ")";
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> jugador), new SenaNoticePacket(text, true));
        if (random.nextFloat() < 0.3F) {
            GauchoEntity m = npcEntity(asientoMandinga);
            if (m != null) {
                String[] retos = {"¡Negro! Las manos quietas.", "Te vi, Negro. Una más y te quemo.",
                        "¿A quién le guiñás, Negro? Mirá tus cartas."};
                m.say(retos[random.nextInt(retos.length)]);
            }
        }
    }

    private void settleMatch(int winner) {
        int[] bestOpp = new int[2];
        boolean[] onlyBots = {true, true};
        boolean[] vsForastero = new boolean[2];
        boolean[] vsVizcacha = new boolean[2];
        for (int s = 0; s < seatKind.length; s++) {
            int oppTeam = 1 - s % 2;
            bestOpp[oppTeam] = Math.max(bestOpp[oppTeam], seatLevel[s]);
            if (seatKind[s] != Kind.BOT) onlyBots[oppTeam] = false;
            GauchoEntity g = npcEntity(s);
            if (g != null) {
                if (g.getRol() == GauchoEntity.Rol.FORASTERO) vsForastero[oppTeam] = true;
                if (g.getRol() == GauchoEntity.Rol.VIZCACHA) vsVizcacha[oppTeam] = true;
            }
        }
        for (int s = 0; s < seatKind.length; s++) {
            ServerPlayerEntity p = onlinePlayer(s);
            if (p == null) continue;
            int team = s % 2;
            if (team == winner) {
                int pts = Prestigio.victoria(bestOpp[team]);
                if (onlyBots[team]) pts /= 2;
                if (com.trucoya.fase5.Fase5.tieneConfraternidad(p)) {
                    pts += Math.max(1, pts / 5);   // +20 % con Confraternidad
                }
                JugadorData.increment(p, JugadorData.GANADOS);
                int racha = JugadorData.increment(p, JugadorData.RACHA);
                if (racha > JugadorData.get(p, JugadorData.MEJOR_RACHA)) JugadorData.set(p, JugadorData.MEJOR_RACHA, racha);
                JugadorData.addPrestigio(p, pts, "Partido ganado");
                if (vsForastero[team]) JugadorData.increment(p, JugadorData.FORASTEROS);
                if (vsVizcacha[team]) {
                    JugadorData.increment(p, JugadorData.VIZCACHA);
                    give(p, new ItemStack(ModItems.BARAJA_DE_ORO.get()));
                    com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.ORO);
                    p.sendMessage(new StringTextComponent("¡Le ganaste al Viejo Vizcacha! Recibís la Baraja de Oro."), p.getUUID());
                    com.trucoya.progreso.Historia.vencioAlVizcacha(p);
                    finalPara.add(p.getUUID());
                }
                Logros.partidoGanado(p, vsForastero[team], vsVizcacha[team], maxDeficit[team] >= 10, racha,
                        JugadorData.nivel(p), clandestina);
                // la Ruta de las Pulperías: ganarle a gauchos en una pulpería da el sello de la región
                com.trucoya.progreso.Historia.Region reg = region();
                if (reg != null && !onlyBots[team]) {
                    com.trucoya.progreso.Historia.ganoEnPulperia(p, reg);
                    com.trucoya.progreso.Pedidos.evento(p, com.trucoya.progreso.Pedidos.RETO_PULPERIA, reg);
                    com.trucoya.progreso.Diario.evento(p, com.trucoya.progreso.Diario.PULPERIA);
                }
                if (!onlyBots[team] && !salamanca) {
                    com.trucoya.progreso.Pedidos.evento(p, com.trucoya.progreso.Pedidos.RETO_PARTIDOS, null);
                    com.trucoya.progreso.Diario.evento(p, com.trucoya.progreso.Diario.PARTIDOS);
                }
                if (vsForastero[team]) {
                    com.trucoya.progreso.Pedidos.evento(p, com.trucoya.progreso.Pedidos.RETO_FORASTERO, null);
                    com.trucoya.progreso.Diario.evento(p, com.trucoya.progreso.Diario.FORASTERO);
                }
                com.trucoya.progreso.Historia.revisarMapa(p);
            } else {
                JugadorData.increment(p, JugadorData.PERDIDOS);
                JugadorData.set(p, JugadorData.RACHA, 0);
                JugadorData.addPrestigio(p, Prestigio.DERROTA, "Partido perdido");
            }
        }
        if (salamanca) {
            com.trucoya.progreso.Salamanca.partidaTermino(level, worldPosition, winner == 0);
            salamanca = false;
        } else {
            for (int s = 0; s < seatKind.length; s++) {
                GauchoEntity g = npcEntity(s);
                if (g != null) {
                    g.say(s % 2 == winner ? "¡Así se juega en el campo, paisano!" : "Me ganaste... volvé cuando quieras.");
                }
            }
        }
        payout(winner);
        cerrarRondaDeTorneo(winner);
    }

    // ------------------------------------------------------------------ torneo

    /** "Pulpería de la Sierra · te falta el sello" (o vacío si la mesa no está en una pulpería). */
    private String lugar(ServerPlayerEntity p) {
        com.trucoya.progreso.Historia.Region reg = region();
        if (reg == null) {
            return "";
        }
        return "Pulpería de " + reg.nombre + (com.trucoya.progreso.Historia.tieneSello(p, reg)
                ? " · sello ganado" : " · ganale al campeón (Nv. " + reg.nivelCampeon + ") por el sello");
    }

    @Nullable
    private com.trucoya.progreso.Historia.Region region() {
        if (!regionCalculada && level instanceof ServerWorld) {
            regionCalculada = true;
            region = com.trucoya.progreso.Historia.pulperiaDe((ServerWorld) level, worldPosition);
        }
        return region;
    }

    private int nivelRonda(int ronda) {
        com.trucoya.progreso.Historia.Region reg = region();
        int base = reg != null ? reg.nivelCampeon : 2;
        return Math.min(5, base + ronda - 1);
    }

    /** Inscripción al torneo de la pulpería: 5 esmeraldas, tres rivales seguidos, partidos a 20. */
    public void iniciarTorneo(ServerPlayerEntity player) {
        if (!inReach(player) || !isSeatedHere(player)) {
            tell(player, "Tenés que estar sentado a esta mesa.");
            return;
        }
        if (isRunning() || torneoRonda > 0) {
            tell(player, "Ya hay algo en juego en esta mesa.");
            return;
        }
        com.trucoya.progreso.Historia.Region reg = region();
        if (reg == null) {
            tell(player, "Los torneos se juegan en las mesas de las pulperías.");
            return;
        }
        if (seatedPlayers().size() > 1) {
            tell(player, "El torneo es mano a mano: tiene que haber un solo jugador sentado.");
            return;
        }
        int esmeraldas = 0;
        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
            ItemStack st = player.inventory.getItem(i);
            if (st.getItem() == net.minecraft.item.Items.EMERALD) esmeraldas += st.getCount();
        }
        if (esmeraldas < TORNEO_INSCRIPCION && !player.isCreative()) {
            tell(player, "La inscripción al torneo cuesta " + TORNEO_INSCRIPCION + " esmeraldas.");
            return;
        }
        if (!player.isCreative()) {
            int falta = TORNEO_INSCRIPCION;
            for (int i = 0; i < player.inventory.getContainerSize() && falta > 0; i++) {
                ItemStack st = player.inventory.getItem(i);
                if (st.getItem() == net.minecraft.item.Items.EMERALD) {
                    int k = Math.min(falta, st.getCount());
                    st.shrink(k);
                    falta -= k;
                }
            }
        }
        standUpNpcs();
        incomingNpc = null;
        torneoJugador = player.getUUID();
        torneoRonda = 1;
        torneoProximoAt = ticks + 60;
        addLog("¡Torneo de la pulpería de " + reg.nombre + "! Tres rivales, partidos a 20. Premio: "
                + TORNEO_PREMIO + " esmeraldas y la Copa.");
        broadcast(false);
    }

    private void cerrarRondaDeTorneo(int winner) {
        if (torneoRonda <= 0 || torneoJugador == null) {
            return;
        }
        ServerPlayerEntity p = null;
        int seat = -1;
        for (int s = 0; s < seatId.length; s++) {
            if (torneoJugador.equals(seatId[s])) {
                seat = s;
                p = onlinePlayer(s);
            }
        }
        if (p == null || seat < 0) {
            cancelarTorneo("el jugador no está.");
            return;
        }
        if (seat % 2 != winner) {
            addLog("Torneo: quedaste afuera en la ronda " + torneoRonda + ".");
            tell(p, "Quedaste afuera del torneo en la ronda " + torneoRonda + ". ¡La próxima será!");
            torneoRonda = 0;
            torneoJugador = null;
            return;
        }
        if (torneoRonda >= TORNEO_RONDAS) {
            com.trucoya.progreso.Historia.Region reg = region();
            give(p, new ItemStack(net.minecraft.item.Items.EMERALD, TORNEO_PREMIO));
            ItemStack copa = new ItemStack(ModItems.COPA_TORNEO.get());
            copa.setHoverName(new StringTextComponent("Copa de " + (reg != null ? reg.nombre : "la pulpería"))
                    .withStyle(TextFormatting.GOLD));
            give(p, copa);
            JugadorData.increment(p, com.trucoya.progreso.Historia.TORNEOS);
            JugadorData.addPrestigio(p, 60, "Campeón del torneo");
            Logros.award(p, Logros.CAMPEON_TORNEO);
            com.trucoya.progreso.Historia.revisarMapa(p);
            p.sendMessage(new StringTextComponent("¡CAMPEÓN DEL TORNEO! " + TORNEO_PREMIO + " esmeraldas y la Copa son tuyas.")
                    .withStyle(TextFormatting.GOLD, TextFormatting.BOLD), p.getUUID());
            level.playSound(null, worldPosition, net.minecraft.util.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            addLog("¡" + p.getGameProfile().getName() + " es campeón del torneo!");
            torneoRonda = 0;
            torneoJugador = null;
            return;
        }
        torneoRonda++;
        torneoProximoAt = ticks + 200;
        addLog("Torneo: ganaste. Ronda " + torneoRonda + " de " + TORNEO_RONDAS + ": ya viene el próximo rival...");
    }

    private void cancelarTorneo(String motivo) {
        if (torneoRonda > 0) {
            addLog("Torneo suspendido: " + motivo);
        }
        torneoRonda = 0;
        torneoJugador = null;
        torneoProximoAt = -1;
    }

    private void tickTorneo() {
        if (torneoRonda <= 0 || torneoJugador == null) {
            return;
        }
        ServerPlayerEntity p = null;
        for (ServerPlayerEntity h : seatedPlayers()) {
            if (h.getUUID().equals(torneoJugador)) p = h;
        }
        if (p == null && !isRunning()) {
            cancelarTorneo("el jugador se levantó de la mesa.");
            broadcast(false);
            return;
        }
        if (torneoProximoAt >= 0 && ticks >= torneoProximoAt && !isRunning() && p != null) {
            torneoProximoAt = -1;
            if (seatedGauchos().isEmpty() && incomingNpc == null) {
                spawnForastero(p, nivelRonda(torneoRonda));
                if (incomingNpc == null) {
                    // sin lugar para que llegue a caballo: se sienta un paisano del lugar
                    torneoProximoAt = ticks + 100;
                }
            }
        }
    }

    private String nameOf(int seat) {
        return seat >= 0 && seat < seatName.length ? seatName[seat] : "?";
    }

    private String teamName(int team) {
        if (seatName.length == 4) {
            return seatName[team] + " y " + seatName[team + 2];
        }
        return nameOf(team);
    }

    private void addLog(String line) {
        log.add(line);
        while (log.size() > TableSnapshot.MAX_LOG) {
            log.removeFirst();
        }
    }

    private void play(SoundEvent sound, float volume) {
        if (level != null && sound != null) {
            level.playSound(null, worldPosition, sound, SoundCategory.PLAYERS, volume,
                    0.95F + random.nextFloat() * 0.1F);
        }
    }

    private void bubble(int seat, String text) {
        ServerPlayerEntity p = onlinePlayer(seat);
        if (p != null) {
            TrucoNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                    new BubblePacket(p.getId(), text));
            return;
        }
        GauchoEntity g = npcEntity(seat);
        if (g != null) {
            g.say(text);
        }
    }

    private void updateAnotador() {
        if (level == null || match == null) {
            return;
        }
        // el anotador va al lado de la mesa: en el piso o arriba de un mueble, a dos bloques como mucho
        for (BlockPos q : BlockPos.betweenClosed(worldPosition.offset(-2, -1, -2), worldPosition.offset(2, 1, 2))) {
            if (q.equals(worldPosition.above())) {
                continue;
            }
            BlockState state = level.getBlockState(q);
            if (state.getBlock() instanceof AnotadorBlock) {
                BlockState updated = ((AnotadorBlock) state.getBlock()).withScore(state, match.getScore(0), match.getScore(1));
                if (updated != state) {
                    level.setBlock(q.immutable(), updated, 3);
                }
            }
        }
    }

    // ------------------------------------------------------------------ envío al cliente

    private void broadcast(boolean open) {
        sincronizarVista();
        List<ServerPlayerEntity> targets = seatedPlayers();
        for (int s = 0; s < seatKind.length; s++) {
            ServerPlayerEntity p = onlinePlayer(s);
            if (p != null && !targets.contains(p)) {
                targets.add(p);
            }
        }
        for (ServerPlayerEntity p : targets) {
            send(p, open);
        }
    }

    private void send(ServerPlayerEntity player, boolean open) {
        int seat = seatOf(player);
        PlayerView view = match != null && seat >= 0 ? match.viewFor(seat) : null;
        List<String> names = new ArrayList<>();
        boolean[] botFlags = new boolean[seatName.length];
        if (match != null) {
            for (int s = 0; s < seatName.length; s++) {
                names.add(seatName[s]);
                botFlags[s] = seatKind[s] != Kind.HUMAN;
            }
        }
        List<TableSnapshot.PotEntry> pot = new ArrayList<>();
        for (int i = 0; i < potStacks.size(); i++) {
            ItemStack st = potStacks.get(i);
            ResourceLocation id = st.getItem().getRegistryName();
            pot.add(new TableSnapshot.PotEntry(id == null ? "" : id.toString(), st.getCount(), potNames.get(i)));
        }
        TableSnapshot snap = new TableSnapshot(view, names, match != null ? botFlags : new boolean[0], log,
                lastCallSeat, lastCallText, cfgPlayers, cfgTarget, cfgFlor, seatedPlayers().size(), isRunning(), open,
                Prestigio.describir(JugadorData.prestigio(player)), pot, clandestina, tantos, tantosFlor, tantoGanador,
                lugar(player), torneoRonda);
        snap.baraja = match != null ? baraja : com.trucoya.barajas.Coleccion.elegida(player).ordinal();
        TrucoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TableViewPacket(worldPosition, snap.toBytes()));
    }

    // ------------------------------------------------------------------ las cartas sobre la mesa (para todos)

    /** Lo que hay sobre la mesa, en pocos números: lo reciben todos los que la tienen cerca. */
    /** Un desafío del día para los humanos de ese equipo (no en la Salamanca). */
    private void diarioEquipo(int team, int tipo) {
        if (salamanca || team < 0) {
            return;
        }
        for (int s = 0; s < seatKind.length; s++) {
            ServerPlayerEntity hp = onlinePlayer(s);
            if (hp != null && s % 2 == team) {
                com.trucoya.progreso.Diario.evento(hp, tipo);
            }
        }
    }

    private CompoundNBT vistaTag() {
        CompoundNBT t = new CompoundNBT();
        TrucoMatch m = match;
        if (m == null || (m.isOver() && endAt < 0)) {   // terminado el partido, la mesa queda limpia
            t.putBoolean("R", false);
            return t;
        }
        int n = m.getConfig().getPlayers();
        int[] jugadas = new int[12];
        java.util.Arrays.fill(jugadas, -1);
        for (int tr = 0; tr < 3; tr++) {
            for (int s = 0; s < n; s++) {
                Card c = m.getTrickCard(tr, s);
                jugadas[tr * 4 + s] = c == null ? -1 : c.getId();
            }
        }
        Card muestra = m.getMuestra();
        t.putBoolean("R", true);
        t.putInt("N", n);
        t.putIntArray("J", jugadas);
        t.putInt("M", muestra == null ? -1 : muestra.getId());
        t.putInt("D", m.getDealer());
        t.putIntArray("S", sillaDeAsiento.length == n ? sillaDeAsiento : new int[] {0, 2, 1, 3});
        t.putInt("B", baraja);
        int[] enMano = new int[4];
        for (int s = 0; s < n; s++) enMano[s] = m.isRoundFinished() ? 0 : m.getHand(s).size();
        t.putIntArray("H", enMano);
        return t;
    }

    /** Si cambió lo que se ve sobre la mesa, se manda a los que la tienen cerca. */
    private void sincronizarVista() {
        if (level == null || level.isClientSide) {
            return;
        }
        CompoundNBT t = vistaTag();
        int h = t.hashCode();
        if (h != vistaEnviada) {
            vistaEnviada = h;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT t = super.save(new CompoundNBT());   // id y posición
        t.put("Vista", vistaTag());
        return t;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        vista.leer(tag.getCompound("Vista"));
    }

    @Nullable
    @Override
    public net.minecraft.network.play.server.SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT t = new CompoundNBT();
        t.put("Vista", vistaTag());
        return new net.minecraft.network.play.server.SUpdateTileEntityPacket(worldPosition, 0, t);
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SUpdateTileEntityPacket pkt) {
        vista.leer(pkt.getTag().getCompound("Vista"));
    }

    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        return new net.minecraft.util.math.AxisAlignedBB(worldPosition).inflate(0.5D, 0.6D, 0.5D);
    }

    /** Lo que el cliente sabe de la mesa (para dibujarla). */
    public static final class VistaMesa {
        public boolean enJuego;
        public int jugadores = 2;
        public final int[] jugadas = new int[12];
        public int muestra = -1;
        public int repartidor = -1;
        public int[] silla = {0, 2, 1, 3};
        public int baraja;
        public final int[] enMano = new int[4];

        VistaMesa() {
            java.util.Arrays.fill(jugadas, -1);
        }

        void leer(CompoundNBT t) {
            enJuego = t.getBoolean("R");
            if (!enJuego) {
                java.util.Arrays.fill(jugadas, -1);
                muestra = -1;
                return;
            }
            jugadores = t.getInt("N") == 4 ? 4 : 2;
            int[] j = t.getIntArray("J");
            for (int i = 0; i < 12; i++) jugadas[i] = i < j.length ? j[i] : -1;
            muestra = t.getInt("M");
            repartidor = t.getInt("D");
            int[] s = t.getIntArray("S");
            if (s.length >= jugadores) silla = s;
            baraja = t.getInt("B");
            int[] h = t.getIntArray("H");
            for (int i = 0; i < 4; i++) enMano[i] = i < h.length ? h[i] : 0;
        }
    }

    // ------------------------------------------------------------------ persistencia

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("CfgPlayers", cfgPlayers);
        nbt.putInt("CfgTarget", cfgTarget);
        nbt.putBoolean("CfgFlor", cfgFlor);
        nbt.putBoolean("Clandestina", clandestina);
        ListNBT list = new ListNBT();
        for (int i = 0; i < potStacks.size(); i++) {
            CompoundNBT entry = new CompoundNBT();
            entry.put("Item", potStacks.get(i).save(new CompoundNBT()));
            if (potOwners.get(i) != null) entry.putUUID("Owner", potOwners.get(i));
            entry.putString("Name", potNames.get(i));
            list.add(entry);
        }
        nbt.put("Pozo", list);
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        cfgPlayers = nbt.getInt("CfgPlayers") == 4 ? 4 : 2;
        cfgTarget = nbt.getInt("CfgTarget") == 40 ? 40 : 30;
        cfgFlor = !nbt.contains("CfgFlor") || nbt.getBoolean("CfgFlor");
        clandestina = nbt.getBoolean("Clandestina");
        potStacks.clear();
        potOwners.clear();
        potNames.clear();
        ListNBT list = nbt.getList("Pozo", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT entry = list.getCompound(i);
            ItemStack stack = ItemStack.of(entry.getCompound("Item"));
            if (stack.isEmpty()) continue;
            UUID owner = entry.hasUUID("Owner") ? entry.getUUID("Owner") : null;
            if (owner == null) continue; // lo de la banca no sobrevive a una partida interrumpida
            potStacks.add(stack);
            potOwners.add(owner);
            potNames.add(entry.getString("Name"));
        }
    }

    @Override
    public String toString() {
        return "MesaTruco@" + worldPosition + (match != null ? " partida " + match.getScore(0) + "-" + match.getScore(1) : "");
    }
}
