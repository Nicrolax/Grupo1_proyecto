package com.trucoya.entity;

import com.trucoya.network.BubblePacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModItems;
import com.trucoya.progreso.JugadorData;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtCustomerGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import com.trucoya.tileentity.MesaTrucoTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Gaucho jugador de truco (modelo animado con GeckoLib). Roles:
 * <ul>
 *   <li>PAISANO: parroquiano de la pulpería, acude a la mesa cuando alguien se sienta.</li>
 *   <li>CANTINERO: comerciante de insumos y también jugador local.</li>
 *   <li>FORASTERO: llega a caballo a desafiar a quien espera en una mesa en campo abierto.</li>
 *   <li>VIZCACHA: el Jefe Final, nivel 5, en la Estancia del Caudillo.</li>
 *   <li>PAISANA, PEON y PAYADOR: gente de campo (pulperías, estancias, aldeas).</li>
 *   <li>MATRERO: jugador fuerte de las guaridas clandestinas.</li>
 * </ul>
 * Los gauchos no pueden ser lastimados por jugadores (son parte del boliche).
 */
public class GauchoEntity extends AbstractVillagerEntity implements IAnimatable {

    public enum Rol {
        // El orden importa: coincide con GauchoAspectos y con el número guardado en el mundo.
        PAISANO, CANTINERO, FORASTERO, VIZCACHA, PAISANA, MATRERO, PEON, PAYADOR, FUNDADOR,
        /** La Salamanca: el diablo y el fantasma del Negro Ledesma (solo existen mientras dura esa noche). */
        MANDINGA, LEDESMA;

        public static Rol byName(String name) {
            for (Rol r : values()) {
                if (r.name().equalsIgnoreCase(name)) return r;
            }
            return PAISANO;
        }
    }

    private static final DataParameter<Integer> ROL = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> NIVEL = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> VARIANTE = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.INT);
    /** Guardia armado con facón (se ve el cuchillo en la mano). */
    private static final DataParameter<Boolean> ARMADO = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.BOOLEAN);
    /** Está persiguiendo a un intruso (pose de amenaza). */
    private static final DataParameter<Boolean> AGRESIVO = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> GESTO = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.INT);
    /** Cuánto se esfumó (0 = entero, 100 = ya no está): el fantasma de Ledesma al amanecer, el Mandinga al perder. */
    private static final DataParameter<Integer> DESVANECER = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.INT);
    /** Tiene un pedido para ofrecer (se ve "¡Pedido!" sobre la cabeza). */
    private static final DataParameter<Boolean> PEDIDO = EntityDataManager.defineId(GauchoEntity.class, DataSerializers.BOOLEAN);
    @Nullable
    private CompoundNBT pedido;
    private long pedidoDia = -1L;

    private static final String[] NOMBRES_PAISANO = {"Don Eusebio", "Ño Pancho", "El Tata Ramírez", "Don Anacleto",
        "El Negro Tito", "Don Sinforiano", "El Rubio Sosa", "Don Florencio", "El Gringo Bertoni", "Don Hilario"};
    private static final String[] NOMBRES_PAISANA = {"Doña Eulalia", "La China Rosaura", "Doña Pilar",
        "Ña Encarnación", "La Moza Celedonia", "Doña Tránsito", "Doña Benita", "La Negra Remedios"};
    private static final String[] NOMBRES_CANTINERO = {"El Pulpero Abelardo", "Don Cosme, pulpero", "Don Fermín, pulpero"};
    private static final String[] NOMBRES_PULPERA = {"Doña Petrona, pulpera", "Doña Juana, pulpera"};
    private static final String[] NOMBRES_MATRERO = {"El Matrero Gómez", "El Tuerto Acosta", "El Zorro Barrios",
        "El Chano Pereyra", "El Lobo Ferreyra", "El Facón Olivera"};
    private static final String[] NOMBRES_PEON = {"Peón Ramírez", "Peón Sosa", "El Tropero Silva",
        "Don Ciriaco, capataz", "El Domador Correa", "El Resero Luna"};
    private static final String[] NOMBRES_PAYADOR = {"El Payador Ledesma", "Don Gabino, payador",
        "El Cantor Medina", "El Payador Montiel"};
    private static final String[] NOMBRES_FORASTERO = {"Forastero Cirilo", "El Tape Olivera", "Don Venancio",
        "Matrero Benítez", "El Indio Cardozo", "Don Leandro, tropero"};


    private final AnimationFactory factory = new AnimationFactory(this);

    @Nullable
    private BlockPos targetChair;
    @Nullable
    private BlockPos tablePos;
    @Nullable
    private UUID horseId;
    @Nullable
    private Vector3d leaveTarget;
    private int leaveTimer = -1;
    private int stuckTicks;
    private boolean initialized;
    @Nullable
    private BlockPos puesto;
    private boolean guardiaPendiente;
    @Nullable
    private UUID avisado;
    private int avisoHasta;
    private static final double ZONA_GUARDIA = 14.0D;
    // Visita: llega a caballo hasta un jugador, le dice algo, le da algo y se va (los Fundadores, la historia).
    @Nullable
    private UUID visitaA;
    /** De visita con los otros tres Fundadores (la maneja progreso.Fundadores): quieto, sin vagar. */
    private boolean visitaGrupal;
    private final java.util.List<String> visitaLineas = new java.util.ArrayList<>();
    private ItemStack visitaRegalo = ItemStack.EMPTY;
    private int visitaPaso;
    private int visitaDesde;
    private int visitaProxima;
    /** Al terminar la visita no se va (se queda donde está). */
    private boolean visitaSeQueda;

    public GauchoEntity(EntityType<? extends GauchoEntity> type, World world) {
        super(type, world);
        this.setCanPickUpLoot(false);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ROL, Rol.PAISANO.ordinal());
        this.entityData.define(NIVEL, 1);
        this.entityData.define(VARIANTE, -1);
        this.entityData.define(ARMADO, false);
        this.entityData.define(AGRESIVO, false);
        this.entityData.define(GESTO, 0);
        this.entityData.define(DESVANECER, 0);
        this.entityData.define(PEDIDO, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 0.8D, true) {
            @Override
            public boolean canUse() {
                return isGuardia() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isGuardia() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(1, new IrALaSillaGoal(this));
        this.goalSelector.addGoal(2, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(3, new LookAtCustomerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 0.45D) {
            @Override
            public boolean canUse() {
                return getRol() != Rol.VIZCACHA && !deLaSalamanca() && targetChair == null && leaveTimer < 0
                        && !visitaGrupal && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
    }

    // ------------------------------------------------------------------ datos

    public Rol getRol() {
        int i = this.entityData.get(ROL);
        Rol[] all = Rol.values();
        return i >= 0 && i < all.length ? all[i] : Rol.PAISANO;
    }

    public void setRol(Rol rol) {
        boolean cambio = getRol() != rol;
        this.entityData.set(ROL, rol.ordinal());
        if (cambio) {
            this.entityData.set(VARIANTE, -1); // se elige una pilcha acorde al nuevo rol
        }
        this.offers = null; // se regeneran según el rol
    }

    /** Variante de pilcha (textura y partes del modelo); -1 si todavía no se eligió. */
    public int getVariante() {
        return this.entityData.get(VARIANTE);
    }

    public void setVariante(int variante) {
        this.entityData.set(VARIANTE, GauchoAspectos.normalizar(getRol(), variante));
    }

    /** La voz de sus cantos: el Vizcacha, viejo; las mujeres, femenina; los demás, según la pilcha. */
    public com.trucoya.core.Voz getVoz() {
        int v = Math.max(0, getVariante());
        if (getRol() == Rol.VIZCACHA) return com.trucoya.core.Voz.VIEJO;
        if (getRol() == Rol.MANDINGA) return com.trucoya.core.Voz.DIABLO;
        if (getRol() == Rol.LEDESMA) return com.trucoya.core.Voz.GAUCHO;
        if (getRol() == Rol.FUNDADOR) return getVariante() == 2 ? com.trucoya.core.Voz.GAUCHO : com.trucoya.core.Voz.HOMBRE;
        if (esMujer()) return v % 2 == 0 ? com.trucoya.core.Voz.MUJER : com.trucoya.core.Voz.PAISANA;
        if (getRol() == Rol.MATRERO || getRol() == Rol.PEON) return com.trucoya.core.Voz.GAUCHO;
        return v % 2 == 0 ? com.trucoya.core.Voz.HOMBRE : com.trucoya.core.Voz.GAUCHO;
    }

    /** El Vizcacha que viene a buscar al jugador para ir a la Salamanca (una copia: el de la estancia sigue allá). */
    public boolean guiaSalamanca;

    /** Personaje de la Salamanca (el Mandinga, Ledesma o el Vizcacha que guía): lo maneja progreso.Salamanca. */
    public boolean deLaSalamanca() {
        return guiaSalamanca || getRol() == Rol.MANDINGA || getRol() == Rol.LEDESMA;
    }

    public boolean esFantasma() {
        return getRol() == Rol.LEDESMA;
    }

    /** 0 = entero, 1 = se esfumó del todo. */
    public float desvanecido() {
        return Math.max(0, Math.min(100, this.entityData.get(DESVANECER))) / 100.0F;
    }

    public void setDesvanecido(float f) {
        this.entityData.set(DESVANECER, Math.round(Math.max(0.0F, Math.min(1.0F, f)) * 100.0F));
    }

    public boolean esMujer() {
        return GauchoAspectos.tiene(getRol(), Math.max(0, getVariante()), GauchoAspectos.MUJER);
    }

    public int getNivel() {
        return this.entityData.get(NIVEL);
    }

    public void setNivel(int nivel) {
        this.entityData.set(NIVEL, Math.max(1, Math.min(5, nivel)));
    }

    /** Actor de la cinemática (entidad que existe solo en un cliente; el servidor no la conoce). */
    public boolean actorCinematica;

    /** Prepara un actor de la cinemática: rol, aspecto y si lleva el facón. Solo cliente. */
    public void prepararActor(Rol rol, int variante, boolean armado) {
        this.actorCinematica = true;
        this.entityData.set(ROL, rol.ordinal());
        this.entityData.set(VARIANTE, variante);
        this.entityData.set(ARMADO, armado);
    }

    /** Hace (o deja de hacer) gestos de estar hablando, solo en este cliente (para la cinemática). */
    public void gestoCliente(boolean hablando) {
        if (this.level.isClientSide) {
            this.entityData.set(GESTO, hablando ? 1_000_000 : 0);
        }
    }

    public boolean isGesturing() {
        return this.entityData.get(GESTO) > 0;
    }

    public String nombre() {
        return this.getName().getString();
    }

    @Nullable
    public BlockPos getTablePos() {
        return tablePos;
    }

    /** Libre para acudir a una mesa. */
    public boolean isAvailable() {
        return this.isAlive() && !this.isPassenger() && targetChair == null && leaveTimer < 0 && !this.isTrading()
                && this.getTarget() == null && visitaA == null && !visitaGrupal && !deLaSalamanca();
    }

    // ------------------------------------------------------------------ visitas

    /** Va hasta el jugador (a caballo si tiene), le dice {@code lineas}, le entrega {@code regalo} y se va. */
    public void visitar(ServerPlayerEntity p, java.util.List<String> lineas, ItemStack regalo, boolean seQueda) {
        this.visitaA = p.getUUID();
        this.visitaLineas.clear();
        this.visitaLineas.addAll(lineas);
        this.visitaRegalo = regalo.copy();
        this.visitaPaso = 0;
        this.visitaDesde = this.tickCount;
        this.visitaProxima = 0;
        this.visitaSeQueda = seQueda;
    }

    public boolean estaDeVisita() {
        return visitaA != null || visitaGrupal;
    }

    public void setDeVisitaGrupal(boolean b) {
        this.visitaGrupal = b;
    }

    /** Se despide y se va (a caballo si tiene uno). */
    public void despedirse() {
        this.visitaA = null;
        startLeaving();
        this.leaveTimer = 300;
    }

    private void tickVisita() {
        if (!(this.level instanceof ServerWorld)) return;
        PlayerEntity pe = this.level.getPlayerByUUID(visitaA);
        ServerPlayerEntity p = pe instanceof ServerPlayerEntity ? (ServerPlayerEntity) pe : null;
        boolean vencida = this.tickCount - visitaDesde > 20 * 120;
        if (p == null || !p.isAlive() || p.distanceToSqr(this) > 96 * 96 || vencida) {
            if (p != null && !visitaRegalo.isEmpty()) {
                for (int i = visitaPaso; i < visitaLineas.size(); i++) {
                    p.sendMessage(new StringTextComponent(nombre() + ": " + visitaLineas.get(i)), p.getUUID());
                }
                if (!p.inventory.add(visitaRegalo)) p.drop(visitaRegalo, false);
            }
            visitaRegalo = ItemStack.EMPTY;
            if (visitaSeQueda) visitaA = null; else despedirse();
            return;
        }
        net.minecraft.entity.Entity montura = this.getVehicle();
        double d = this.distanceToSqr(p);
        if (d > 4.5 * 4.5 && this.tickCount - visitaDesde > 20 * 25 && visitaPaso == 0) {
            // no encuentra el camino (un río, un barranco, una pared): se baja y aparece al lado
            if (montura != null) {
                this.stopRiding();
                montura.moveTo(p.getX() + 2.0D, p.getY(), p.getZ() + 2.0D, montura.yRot, 0F);
            }
            Vector3d al = p.position().add(p.getLookAngle().multiply(2.5D, 0, 2.5D));
            this.moveTo(al.x, p.getY(), al.z, this.yRot, 0F);
            if (this.level instanceof ServerWorld) {
                ((ServerWorld) this.level).sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.01);
            }
            return;
        }
        if (d > 4.5 * 4.5) {
            if (this.tickCount % 20 == 0) {
                if (montura instanceof AbstractHorseEntity) {
                    ((AbstractHorseEntity) montura).getNavigation().moveTo(p, 1.5D);
                } else {
                    this.getNavigation().moveTo(p, 0.6D);
                }
            }
            return;
        }
        if (montura instanceof AbstractHorseEntity) {
            ((AbstractHorseEntity) montura).getNavigation().stop();
        }
        this.getNavigation().stop();
        this.getLookControl().setLookAt(p, 30.0F, 30.0F);
        if (this.tickCount < visitaProxima) return;
        if (visitaPaso < visitaLineas.size()) {
            String linea = visitaLineas.get(visitaPaso++);
            say(linea);
            p.sendMessage(new StringTextComponent(nombre() + ": ").withStyle(net.minecraft.util.text.TextFormatting.GOLD)
                    .append(new StringTextComponent(linea).withStyle(net.minecraft.util.text.TextFormatting.WHITE)), p.getUUID());
            visitaProxima = this.tickCount + 50 + Math.min(80, linea.length() * 2);
            return;
        }
        if (!visitaRegalo.isEmpty()) {
            ItemStack r = visitaRegalo;
            visitaRegalo = ItemStack.EMPTY;
            p.sendMessage(new StringTextComponent(nombre() + " te dio: ").withStyle(net.minecraft.util.text.TextFormatting.GRAY)
                    .append(r.getHoverName()), p.getUUID());
            if (!p.inventory.add(r)) p.drop(r, false);
            this.level.playSound(null, p.blockPosition(), net.minecraft.util.SoundEvents.ITEM_PICKUP,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.8F, 1.0F);
            visitaProxima = this.tickCount + 40;
            return;
        }
        if (visitaSeQueda) visitaA = null; else despedirse();
    }

    // ------------------------------------------------------------------ guardias de la guarida

    public boolean isGuardia() {
        return this.entityData.get(ARMADO);
    }

    public boolean isAgresivo() {
        return this.entityData.get(AGRESIVO);
    }

    /** Lo pone (o lo saca) de guardia: lleva facón y cuida el lugar donde está parado. */
    public void setGuardia(boolean guardia) {
        this.entityData.set(ARMADO, guardia);
        if (guardia) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(ModItems.FACON.get()));
            this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);   // el facón lo suelta loot propio
            if (puesto == null) {
                puesto = this.blockPosition();
            }
            this.restrictTo(puesto, 10);
        } else {
            this.setItemSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
            this.restrictTo(BlockPos.ZERO, -1);
            this.setTarget(null);
        }
    }

    /** Tiene permiso para estar en la guarida: lleva una Ficha de Clandestina o es Nivel 3 o más. */
    public static boolean tienePermiso(PlayerEntity p) {
        if (p.isCreative() || p.isSpectator()) {
            return true;
        }
        if (p.inventory.contains(new ItemStack(ModItems.FICHA_CLANDESTINA.get()))) {
            return true;
        }
        return JugadorData.nivel(p) >= 3;
    }

    private boolean enZonaVigilada(PlayerEntity p) {
        if (puesto == null) return false;
        double dx = p.getX() - (puesto.getX() + 0.5D);
        double dz = p.getZ() - (puesto.getZ() + 0.5D);
        double dy = p.getY() - puesto.getY();
        return dx * dx + dz * dz <= ZONA_GUARDIA * ZONA_GUARDIA && dy > -4.0D && dy < 5.0D;
    }

    /** Cada medio segundo: avisa a los intrusos sin ficha y, si no se van, los ataca. */
    private void vigilar() {
        LivingEntity objetivo = this.getTarget();
        if (objetivo != null) {
            boolean seFue = objetivo instanceof PlayerEntity && !enZonaVigilada((PlayerEntity) objetivo)
                    && objetivo.distanceToSqr(this) > 18 * 18;
            boolean arreglo = objetivo instanceof PlayerEntity && tienePermiso((PlayerEntity) objetivo)
                    && !(objetivo == this.getLastHurtByMob());
            if (!objetivo.isAlive() || seFue || arreglo
                    || (objetivo instanceof PlayerEntity && (((PlayerEntity) objetivo).isCreative()
                    || ((PlayerEntity) objetivo).isSpectator()))) {
                if (objetivo.isAlive() && objetivo instanceof ServerPlayerEntity) {
                    talk((ServerPlayerEntity) objetivo, arreglo ? "Ahora sí. Pasá, pero portate bien."
                            : "¡Y no vuelvas sin ficha!");
                }
                this.setTarget(null);
                this.setLastHurtByMob(null);
            }
            this.entityData.set(AGRESIVO, this.getTarget() != null);
            return;
        }
        this.entityData.set(AGRESIVO, false);
        if (puesto == null || this.getVehicle() instanceof SeatEntity) {
            return;
        }
        for (PlayerEntity p : this.level.getEntitiesOfClass(PlayerEntity.class,
                new net.minecraft.util.math.AxisAlignedBB(puesto).inflate(ZONA_GUARDIA, 6, ZONA_GUARDIA))) {
            if (!enZonaVigilada(p) || tienePermiso(p) || !this.canSee(p)) {
                if (p.getUUID().equals(avisado) && !enZonaVigilada(p)) {
                    avisado = null;
                }
                continue;
            }
            if (!p.getUUID().equals(avisado)) {
                avisado = p.getUUID();
                avisoHasta = this.tickCount + 80;
                if (p instanceof ServerPlayerEntity) {
                    talk((ServerPlayerEntity) p, "¡Alto ahí, forastero! Acá se entra con Ficha de Clandestina. "
                            + "Rajá o te corto.");
                }
                this.getLookControl().setLookAt(p, 30F, 30F);
            } else if (this.tickCount >= avisoHasta) {
                if (p instanceof ServerPlayerEntity) {
                    talk((ServerPlayerEntity) p, "¡Te lo avisé! ¡A él, muchachos!");
                }
                this.setTarget(p);
                // los otros guardias cercanos se suman
                for (GauchoEntity otro : this.level.getEntitiesOfClass(GauchoEntity.class,
                        this.getBoundingBox().inflate(16, 6, 16))) {
                    if (otro != this && otro.isGuardia() && otro.getTarget() == null
                            && !(otro.getVehicle() instanceof SeatEntity)) {
                        otro.setTarget(p);
                    }
                }
                this.entityData.set(AGRESIVO, true);
                return;
            }
            return;
        }
    }

    /** Camina hasta la silla indicada y se sienta. */
    public void goSit(BlockPos chair, BlockPos table) {
        this.targetChair = chair.immutable();
        this.tablePos = table.immutable();
        this.stuckTicks = 0;
    }

    /** Está caminando hacia una silla. */
    public boolean yendoASentarse() {
        return targetChair != null;
    }

    @Nullable
    BlockPos getTargetChair() {
        return targetChair;
    }

    void clearTargetChair() {
        this.targetChair = null;
    }

    /** Se levanta de la mesa. El forastero, además, se va. */
    public void standUp() {
        if (this.getVehicle() instanceof SeatEntity) {
            this.stopRiding();
        }
        this.targetChair = null;
        if (getRol() == Rol.FORASTERO) {
            startLeaving();
        } else {
            this.tablePos = null;
        }
    }

    public void setHorse(@Nullable UUID id) {
        this.horseId = id;
    }

    private void startLeaving() {
        this.leaveTimer = 400;
        BlockPos from = tablePos != null ? tablePos : this.blockPosition();
        double angle = this.random.nextDouble() * Math.PI * 2;
        this.leaveTarget = new Vector3d(from.getX() + Math.cos(angle) * 40, from.getY(), from.getZ() + Math.sin(angle) * 40);
    }

    /** Dice algo: globo de texto para los jugadores cercanos y un gesto. */
    public void say(String text) {
        if (this.level.isClientSide) return;
        this.entityData.set(GESTO, 30);
        // El globo va corto (la frase entera ya sale en el chat); nunca debe tirar abajo el servidor.
        String globo = text;
        if (globo.length() > 48) {
            int corte = globo.lastIndexOf(' ', 46);
            globo = globo.substring(0, corte > 20 ? corte : 46) + "…";
        }
        try {
            TrucoNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new BubblePacket(this.getId(), globo));
        } catch (RuntimeException e) {
            com.trucoya.TrucoYa.LOGGER.warn("Truco Ya: no se pudo mostrar el globo de {}", nombre(), e);
        }
    }

    // ------------------------------------------------------------------ ciclo

    /**
     * A caballo, el cuerpo del jinete mira hacia donde va el caballo. En Minecraft el caballo copia la rotación del
     * cuerpo de un jinete que es un mob, y el jinete copia la del caballo: los dos quedaban trabados mirando hacia
     * donde aparecieron y el caballo parecía andar de costado o marcha atrás. Corre en el servidor y en el cliente.
     */
    @Override
    public void rideTick() {
        super.rideTick();
        net.minecraft.entity.Entity v = this.getVehicle();
        if (v instanceof AbstractHorseEntity) {
            AbstractHorseEntity h = (AbstractHorseEntity) v;
            Vector3d mov = h.getDeltaMovement();
            float rumbo = h.yRot;
            if (mov.x * mov.x + mov.z * mov.z > 0.0004D) {
                rumbo = (float) (MathHelper.atan2(mov.z, mov.x) * (180D / Math.PI)) - 90.0F;
            }
            this.yBodyRot = rumbo;
            h.yBodyRot = rumbo;
            if (!this.level.isClientSide) {
                h.yRot = rumbo;
            }
            // la cabeza puede mirar a los costados, pero no para atrás
            this.yHeadRot = rumbo + MathHelper.clamp(MathHelper.wrapDegrees(this.yHeadRot - rumbo), -60.0F, 60.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            return;
        }
        if (!initialized) {
            initialized = true;
            initDefaults();
        }
        if (guardiaPendiente) {
            guardiaPendiente = false;
            setGuardia(true);
        }
        if (getVariante() < 0) {
            setVariante(this.random.nextInt(GauchoAspectos.variantes(getRol())));
        }
        // Los de la Salamanca existen solo mientras dura esa noche (si se cargan después, se van).
        if (deLaSalamanca() && this.tickCount % 20 == 3 && this.tickCount > 40
                && !com.trucoya.progreso.Salamanca.esActor(this)) {
            this.remove();
            return;
        }
        // El Viejo Vizcacha: la primera vez que alguien lo ve, la cinemática "El Último Truco".
        if (getRol() == Rol.VIZCACHA && !guiaSalamanca && this.tickCount % 20 == 7) {
            com.trucoya.progreso.CinematicaServidor.revisar(this);
        }
        int g = this.entityData.get(GESTO);
        if (g > 0) this.entityData.set(GESTO, g - 1);
        if (this.tickCount % 100 == 13) {
            revisarPedido();
        }

        if (visitaA != null) {
            tickVisita();
            return;
        }
        if (visitaGrupal) {
            return;
        }
        // un Fundador solo existe para visitar: si quedó suelto (se reinició el servidor a mitad de una visita), se va
        if (getRol() == Rol.FUNDADOR && leaveTimer < 0 && this.tickCount > 200 && !actorCinematica) {
            despedirse();
        }

        if (isGuardia() && this.tickCount % 10 == 0) {
            if (this.getTarget() != null && this.getVehicle() instanceof SeatEntity) {
                this.standUp();  // se levanta de la mesa para pelear
            }
            vigilar();
        }

        // Sentado: mirar hacia la mesa.
        if (this.getVehicle() instanceof SeatEntity && tablePos != null) {
            double dx = tablePos.getX() + 0.5D - this.getX();
            double dz = tablePos.getZ() + 0.5D - this.getZ();
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            this.yRot = yaw;
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }

        // A caballo: el caballo lleva al forastero hasta la mesa (o lejos, si se va).
        if (this.getVehicle() instanceof AbstractHorseEntity) {
            AbstractHorseEntity horse = (AbstractHorseEntity) this.getVehicle();
            Vector3d dest = leaveTimer >= 0 ? leaveTarget
                    : tablePos != null ? new Vector3d(tablePos.getX() + 0.5D, tablePos.getY(), tablePos.getZ() + 0.5D) : null;
            if (dest != null) {
                if (this.tickCount % 20 == 0) {
                    horse.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.6D);
                }
                if (leaveTimer < 0 && horse.distanceToSqr(dest) < 49.0D) {
                    this.stopRiding();
                    horse.getNavigation().stop();
                }
            }
        }

        if (leaveTimer >= 0) {
            tickLeaving();
        }
    }

    private void tickLeaving() {
        leaveTimer--;
        if (!this.isPassenger() && horseId != null && this.level instanceof ServerWorld) {
            net.minecraft.entity.Entity e = ((ServerWorld) this.level).getEntity(horseId);
            if (e instanceof AbstractHorseEntity && e.isAlive() && !e.isVehicle()) {
                if (this.distanceToSqr(e) < 9.0D) {
                    this.startRiding(e, true);
                } else if (this.tickCount % 20 == 0) {
                    this.getNavigation().moveTo(e, 0.6D);
                }
            } else if (leaveTarget != null && this.tickCount % 40 == 0) {
                this.getNavigation().moveTo(leaveTarget.x, leaveTarget.y, leaveTarget.z, 0.6D);
            }
        } else if (!this.isPassenger() && leaveTarget != null && this.tickCount % 40 == 0) {
            this.getNavigation().moveTo(leaveTarget.x, leaveTarget.y, leaveTarget.z, 0.6D);
        }
        if (leaveTimer <= 0) {
            vanish();
        }
    }

    private void vanish() {
        if (this.level instanceof ServerWorld) {
            ServerWorld sw = (ServerWorld) this.level;
            sw.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1, this.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
            if (horseId != null) {
                net.minecraft.entity.Entity e = sw.getEntity(horseId);
                if (e != null) {
                    e.ejectPassengers();
                    e.remove();
                }
            }
        }
        this.remove();
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isGuardia()) {
            // Los guardias se pueden pelear (pero no se ahogan ni se asfixian en la pared).
            return source == DamageSource.IN_WALL || source == DamageSource.DROWN || super.isInvulnerableTo(source);
        }
        return source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && isGuardia() && !this.level.isClientSide && source.getEntity() instanceof LivingEntity
                && !(source.getEntity() instanceof GauchoEntity)) {
            LivingEntity agresor = (LivingEntity) source.getEntity();
            if (!(agresor instanceof PlayerEntity && ((PlayerEntity) agresor).isCreative())) {
                this.setTarget(agresor);
                this.entityData.set(AGRESIVO, true);
                if (this.getVehicle() instanceof SeatEntity) {
                    this.standUp();
                }
            }
        }
        return r;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (isGuardia()) {
            if (this.random.nextFloat() < 0.35F + 0.1F * looting) {
                this.spawnAtLocation(new ItemStack(ModItems.FACON.get()));
            }
            if (this.random.nextFloat() < 0.2F) {
                this.spawnAtLocation(new ItemStack(ModItems.FICHA_CLANDESTINA.get()));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return getRol() == Rol.FORASTERO || getRol() == Rol.FUNDADOR;
    }

    // ------------------------------------------------------------------ pedidos

    public boolean ofrecePedido() {
        return this.entityData.get(PEDIDO);
    }

    @Nullable
    public CompoundNBT pedidoOfrecido() {
        return pedido;
    }

    public boolean puedePedir() {
        Rol r = getRol();
        return (r == Rol.PAISANO || r == Rol.PAISANA || r == Rol.PEON || r == Rol.PAYADOR) && !isGuardia() && !guiaSalamanca
                && !actorCinematica && visitaA == null;
    }

    /** Una vez por día de juego: a veces se le ocurre un pedido nuevo (o se olvida del viejo). */
    private void revisarPedido() {
        long dia = this.level.getDayTime() / 24000L;
        if (dia == pedidoDia) {
            this.entityData.set(PEDIDO, pedido != null && puedePedir());
            return;
        }
        pedidoDia = dia;
        if (!puedePedir() || !com.trucoya.TrucoConfig.PEDIDOS.get()) {
            pedido = null;
        } else if (pedido == null ? this.random.nextFloat() < 0.4F : this.random.nextFloat() < 0.3F) {
            pedido = this.random.nextFloat() < 0.75F ? com.trucoya.progreso.Pedidos.crear(this, this.random) : null;
        }
        this.entityData.set(PEDIDO, pedido != null && puedePedir());
    }

    @Override
    protected ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND || !this.isAlive()) {
            return ActionResultType.PASS;
        }
        if (deLaSalamanca()) {
            if (!this.level.isClientSide && this.tickCount % 2 == 0) {
                com.trucoya.progreso.Salamanca.hablarCon(this, player);
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        // Convidarle un mate cebado
        ItemStack enMano = player.getItemInHand(hand);
        // Pedidos: entregar lo que pidió, o ver / aceptar el que ofrece (agachado se lo invita a jugar, como siempre)
        if (!player.isShiftKeyDown() && getRol() != Rol.CANTINERO && !(enMano.getItem() instanceof com.trucoya.fase5.MateItem)) {
            if (!this.level.isClientSide && player instanceof ServerPlayerEntity
                    && com.trucoya.progreso.Pedidos.hablar(this, (ServerPlayerEntity) player, puedePedir() ? pedido : null)) {
                return ActionResultType.CONSUME;
            }
        }
        if (enMano.getItem() instanceof com.trucoya.fase5.MateItem
                && com.trucoya.fase5.MateItem.estado(enMano) == com.trucoya.fase5.MateItem.CEBADO) {
            if (!this.level.isClientSide) {
                com.trucoya.fase5.MateItem.convidar(enMano, player, this);
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        // El cantinero vende con clic derecho; agachado (Shift) se le pide una partida.
        if (getRol() == Rol.CANTINERO && !player.isShiftKeyDown() && !this.isPassenger() && !this.isTrading()) {
            if (!this.level.isClientSide && !this.getOffers().isEmpty()) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        if (!this.level.isClientSide && player instanceof ServerPlayerEntity) {
            invitar((ServerPlayerEntity) player);
        }
        return ActionResultType.sidedSuccess(this.level.isClientSide);
    }

    /** El jugador lo invita a jugar: busca una Mesa de Truco cercana y va a sentarse. */
    private void invitar(ServerPlayerEntity player) {
        if (this.getVehicle() instanceof SeatEntity) {
            talk(player, "Ya estoy en la mesa. ¡Sentate y repartí!");
            return;
        }
        if (!isAvailable() && targetChair != null) {
            talk(player, "Ya voy para la mesa, paisano.");
            return;
        }
        MesaTrucoTileEntity mesa = null;
        double best = 24.0D * 24.0D;
        for (TileEntity te : this.level.blockEntityList) {
            if (te instanceof MesaTrucoTileEntity && !te.isRemoved()) {
                BlockPos p = te.getBlockPos();
                double d = player.distanceToSqr(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D);
                if (d < best) {
                    best = d;
                    mesa = (MesaTrucoTileEntity) te;
                }
            }
        }
        if (mesa == null) {
            talk(player, "Poné una Mesa de Truco con dos Sillas Criollas y te juego un partido.");
            return;
        }
        String result = mesa.inviteNpc(this, player);
        talk(player, result);
    }

    private void talk(ServerPlayerEntity player, String line) {
        say(line);
        player.displayClientMessage(new StringTextComponent(nombre() + " (Nv. " + getNivel() + "): " + line), false);
    }

    // ------------------------------------------------------------------ comercio

    @Override
    protected void updateTrades() {
        if (getRol() != Rol.CANTINERO) {
            return;
        }
        MerchantOffers o = this.getOffers();
        o.add(new MerchantOffer(new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.SILLA_CRIOLLA.get()), 999, 1, 0.05F));
        o.add(new MerchantOffer(new ItemStack(Items.EMERALD, 4), new ItemStack(ModItems.MESA_TRUCO.get()), 999, 1, 0.05F));
        o.add(new MerchantOffer(new ItemStack(Items.EMERALD, 1), new ItemStack(ModItems.ANOTADOR.get()), 999, 1, 0.05F));
        o.add(new MerchantOffer(new ItemStack(Items.EMERALD, 24), new ItemStack(ModItems.FICHA_CLANDESTINA.get()), 999, 5, 0.05F));
        ModItems.addCantineroTrades(o);
        // cada pulpero vende la baraja de su región (y la criolla, para regalar)
        com.trucoya.progreso.Historia.Region reg = com.trucoya.progreso.Historia.Region.de(
                this.level.getBiome(this.blockPosition()).getBiomeCategory());
        o.add(new MerchantOffer(new ItemStack(Items.EMERALD, 16), new ItemStack(ModItems.baraja(com.trucoya.barajas.Baraja.deRegion(reg))),
                3, 10, 0.05F));
        o.add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(ModItems.BARAJA_CRIOLLA.get()), 999, 1, 0.05F));
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.updateTrades();
        }
        return this.offers;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity other) {
        return null;
    }

    // ------------------------------------------------------------------ aparición y guardado

    @Nullable
    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficulty, SpawnReason reason,
                                           @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        // Los datos del huevo (EntityTag) se aplican después: initDefaults() corre en el primer tick.
        // los gauchos nunca nacen "bebés" (el valor por defecto de los aldeanos da un 5 %)
        if (data == null) {
            data = new AgeableEntity.AgeableData(false);
        }
        return super.finalizeSpawn(world, difficulty, reason, data, nbt);
    }

    /** Nombre, nivel y persistencia según el rol (si no vinieron dados). */
    public void initDefaults() {
        Rol rol = getRol();
        if (getVariante() < 0) {
            setVariante(this.random.nextInt(GauchoAspectos.variantes(rol)));
        }
        if (!this.hasCustomName()) {
            String[] lista;
            switch (rol) {
                case CANTINERO: lista = esMujer() ? NOMBRES_PULPERA : NOMBRES_CANTINERO; break;
                case FORASTERO: lista = NOMBRES_FORASTERO; break;
                case VIZCACHA: lista = new String[]{"El Viejo Vizcacha"}; break;
                case FUNDADOR: lista = new String[]{com.trucoya.progreso.Fundadores.FUNDADORES[
                        GauchoAspectos.normalizar(rol, getVariante())].nombre}; break;
                case MANDINGA: lista = new String[]{"El Mandinga"}; break;
                case LEDESMA: lista = new String[]{"El Negro Ledesma"}; break;
                case PAISANA: lista = NOMBRES_PAISANA; break;
                case MATRERO: lista = NOMBRES_MATRERO; break;
                case PEON: lista = NOMBRES_PEON; break;
                case PAYADOR: lista = NOMBRES_PAYADOR; break;
                default: lista = esMujer() ? NOMBRES_PAISANA : NOMBRES_PAISANO; break;
            }
            this.setCustomName(new StringTextComponent(lista[this.random.nextInt(lista.length)]));
        }
        if (rol == Rol.VIZCACHA || rol == Rol.MANDINGA) {
            setNivel(5);
        } else if (rol == Rol.LEDESMA) {
            setNivel(4);
        } else if (rol == Rol.CANTINERO && getNivel() < 2) {
            setNivel(2);
        } else if (rol == Rol.MATRERO && getNivel() < 3) {
            setNivel(3);
        }
        if (rol != Rol.FORASTERO && rol != Rol.FUNDADOR && !deLaSalamanca()) {
            this.setPersistenceRequired();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Rol", getRol().name());
        nbt.putInt("Nivel", getNivel());
        nbt.putInt("Variante", getVariante());
        if (tablePos != null) nbt.putLong("Mesa", tablePos.asLong());
        if (horseId != null) nbt.putUUID("Caballo", horseId);
        nbt.putInt("Partida", leaveTimer);
        nbt.putBoolean("Inicializado", initialized);
        nbt.putBoolean("Guardia", isGuardia());
        if (puesto != null) nbt.putLong("Puesto", puesto.asLong());
        if (guiaSalamanca) nbt.putBoolean("GuiaSalamanca", true);
        if (pedido != null) nbt.put("Pedido", pedido);
        nbt.putLong("PedidoDia", pedidoDia);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("Rol")) {
            Rol rol = Rol.byName(nbt.getString("Rol"));
            this.entityData.set(ROL, rol.ordinal());
        }
        if (nbt.contains("Nivel")) setNivel(nbt.getInt("Nivel"));
        if (nbt.contains("Variante") && nbt.getInt("Variante") >= 0) setVariante(nbt.getInt("Variante"));
        if (nbt.contains("Mesa")) tablePos = BlockPos.of(nbt.getLong("Mesa"));
        if (nbt.hasUUID("Caballo")) horseId = nbt.getUUID("Caballo");
        if (nbt.contains("Partida") && nbt.getInt("Partida") >= 0) startLeaving();
        initialized = nbt.getBoolean("Inicializado");
        guiaSalamanca = nbt.getBoolean("GuiaSalamanca");
        pedido = nbt.contains("Pedido", 10) ? nbt.getCompound("Pedido") : null;
        pedidoDia = nbt.contains("PedidoDia") ? nbt.getLong("PedidoDia") : -1L;
        this.entityData.set(PEDIDO, pedido != null);
        if (nbt.contains("Puesto")) puesto = BlockPos.of(nbt.getLong("Puesto"));
        if (nbt.getBoolean("Guardia")) {
            // el puesto se fija al primer tick (en la plantilla todavía no hay posición final)
            this.entityData.set(ARMADO, true);
            guardiaPendiente = true;
        }
        this.offers = null;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ------------------------------------------------------------------ animaciones (GeckoLib)

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController(this, "main", 4, this::predicate));
    }

    @SuppressWarnings("rawtypes")
    private PlayState predicate(AnimationEvent event) {
        AnimationController controller = event.getController();
        String anim;
        if (this.swinging) {
            anim = "animation.gaucho.attack";
        } else if (isAgresivo() && !this.isPassenger()) {
            anim = event.isMoving() ? "animation.gaucho.amenaza_walk" : "animation.gaucho.amenaza";
        } else if (this.getVehicle() instanceof SeatEntity) {
            anim = isGesturing() ? "animation.gaucho.sit_talk" : "animation.gaucho.sit";
        } else if (this.isPassenger()) {
            anim = "animation.gaucho.ride";
        } else if (event.isMoving()) {
            anim = "animation.gaucho.walk";
        } else if (isGesturing()) {
            anim = "animation.gaucho.talk";
        } else {
            anim = "animation.gaucho.idle";
        }
        controller.setAnimation(new AnimationBuilder().addAnimation(anim, true));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    // ------------------------------------------------------------------ objetivo: ir a la silla

    /** Camina hasta la silla asignada y se sienta (si se traba, se sienta igual). */
    static class IrALaSillaGoal extends Goal {
        private final GauchoEntity g;

        IrALaSillaGoal(GauchoEntity g) {
            this.g = g;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return g.getTargetChair() != null && !g.isPassenger();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            BlockPos chair = g.getTargetChair();
            if (chair == null) return;
            double x = chair.getX() + 0.5D;
            double y = chair.getY();
            double z = chair.getZ() + 0.5D;
            g.stuckTicks++;
            if (g.stuckTicks % 10 == 1) {
                g.getNavigation().moveTo(x, y, z, 0.6D);
            }
            boolean close = g.distanceToSqr(x, y, z) < 4.0D;
            if (close || g.stuckTicks > 600) {
                if (!(g.level.getBlockState(chair).getBlock() instanceof com.trucoya.block.SillaCriollaBlock)
                        || !SeatEntity.sit(g, chair)) {
                    g.clearTargetChair();
                    return;
                }
                g.getNavigation().stop();
                g.clearTargetChair();
                List<PlayerEntity> near = g.level.getEntitiesOfClass(PlayerEntity.class, g.getBoundingBox().inflate(8.0D));
                if (!near.isEmpty() && g.getRol() != Rol.LEDESMA) {
                    g.say(g.guiaSalamanca ? "Vos y yo, gurí. Contra el diablo."
                            : g.getRol() == Rol.MANDINGA ? "Mi silla de siempre..."
                            : g.getRol() == Rol.VIZCACHA ? "Vamos a ver de qué estás hecho." : "¡Acá estoy! Repartí cuando quieras.");
                }
            }
        }
    }
}
