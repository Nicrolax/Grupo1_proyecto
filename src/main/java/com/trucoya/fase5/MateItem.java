package com.trucoya.fase5;

import com.trucoya.entity.GauchoEntity;
import com.trucoya.progreso.Logros;
import com.trucoya.registry.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Mate con bombilla. Tres estados:
 * <ol>
 *   <li>Vacío: con Yerba Mate en la otra mano, clic derecho lo carga (alcanza para 10 cebadas).</li>
 *   <li>Cargado: con la Pava caliente en la otra mano, clic derecho lo ceba.</li>
 *   <li>Cebado: mantener clic derecho para tomarlo (da Confraternidad) o clic derecho sobre otro
 *       jugador o un gaucho para convidarlo (los dos reciben Confraternidad y vos ganás "El que Ceba").</li>
 * </ol>
 * Cuando se terminan las cebadas, el mate se "lava" y hay que cargarle yerba nueva.
 */
public class MateItem extends Item {

    public static final int VACIO = 0, CARGADO = 1, CEBADO = 2;
    public static final int CEBADAS = 10;
    private static final int TOMAR = 20 * 60;          // 1 minuto de Confraternidad por mate tomado
    private static final int CONVIDAR = 20 * 90;       // 1 minuto y medio al que recibe el mate
    private static final int TOPE = 20 * 60 * 6;       // hasta 6 minutos acumulados

    private static final String[] GRACIAS = {"¡Gracias, che! Está bárbaro.", "¡Qué rico amargo!",
        "Este mate está como pa' ganar un truco.", "¡A la pucha, qué bien cebado!", "Gracias, paisano."};

    public MateItem(Properties props) {
        super(props);
    }

    public static int estado(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("Estado") : VACIO;
    }

    public static int cebadas(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("Cebadas") : 0;
    }

    private static void set(ItemStack s, int estado, int cebadas) {
        CompoundNBT t = s.getOrCreateTag();
        t.putInt("Estado", estado);
        t.putInt("Cebadas", cebadas);
    }

    /** Después de tomar o convidar: una cebada menos; si se terminó, el mate se lava. */
    private static boolean usarCebada(ItemStack s, PlayerEntity p) {
        int c = cebadas(s) - 1;
        if (c <= 0) {
            set(s, VACIO, 0);
            p.displayClientMessage(new StringTextComponent("Se lavó el mate. Cargale yerba nueva."), true);
            return false;
        }
        set(s, CARGADO, c);
        return true;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack mate = player.getItemInHand(hand);
        ItemStack otra = player.getItemInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
        switch (estado(mate)) {
            case CEBADO:
                player.startUsingItem(hand);
                return ActionResult.consume(mate);
            case CARGADO:
                if (otra.getItem() instanceof PavaItem && PavaItem.estado(otra) == PavaItem.CALIENTE) {
                    cebar(world, player, mate, otra);
                    return ActionResult.sidedSuccess(mate, world.isClientSide);
                }
                if (!world.isClientSide) {
                    player.displayClientMessage(new StringTextComponent(PavaItem.queFalta(otra)), true);
                }
                return ActionResult.fail(mate);
            default:
                if (otra.getItem() == ModItems.YERBA_MATE.get()) {
                    world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.SAND_PLACE,
                            SoundCategory.PLAYERS, 0.8F, 1.4F);
                    if (!world.isClientSide) {
                        if (!player.abilities.instabuild) {
                            otra.shrink(1);
                        }
                        set(mate, CARGADO, CEBADAS);
                        player.displayClientMessage(new StringTextComponent(
                                "Mate cargado. Ahora cebalo con la pava caliente."), true);
                    }
                    return ActionResult.sidedSuccess(mate, world.isClientSide);
                }
                if (!world.isClientSide) {
                    player.displayClientMessage(new StringTextComponent(otra.getItem() instanceof PavaItem
                            ? "Primero cargá el mate: Yerba Mate en la otra mano y clic derecho con el mate."
                            : "Para cargar el mate, tené Yerba Mate en la otra mano."), true);
                }
                return ActionResult.fail(mate);
        }
    }

    /** Cebar: el agua de la pava caliente al mate cargado (se puede hacer con cualquiera de los dos en la mano). */
    static void cebar(World world, PlayerEntity player, ItemStack mate, ItemStack pava) {
        world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY,
                SoundCategory.PLAYERS, 0.8F, 1.3F);
        if (!world.isClientSide && PavaItem.servir(pava)) {
            set(mate, CEBADO, cebadas(mate));
            player.displayClientMessage(new StringTextComponent(
                    "Mate cebado. Mantené clic derecho para tomarlo, o convidáselo a alguien."), true);
        }
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return estado(stack) == CEBADO ? UseAction.DRINK : UseAction.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return estado(stack) == CEBADO ? 40 : 0;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, LivingEntity entity) {
        if (estado(stack) == CEBADO && entity instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity) entity;
            if (!world.isClientSide) {
                Fase5.confraternidad(p, TOMAR, TOPE);
                p.getFoodData().eat(1, 0.2F);
                usarCebada(stack, p);
            }
        }
        return stack;
    }

    /** Convidar el mate a otro jugador o a un gaucho. */
    @Override
    public ActionResultType interactLivingEntity(ItemStack ignorado, PlayerEntity player, LivingEntity target, Hand hand) {
        ItemStack mate = player.getItemInHand(hand);
        if (estado(mate) != CEBADO || !(target instanceof PlayerEntity || target instanceof GauchoEntity)) {
            return ActionResultType.PASS;
        }
        if (!player.level.isClientSide) {
            convidar(mate, player, target);
        }
        return ActionResultType.sidedSuccess(player.level.isClientSide);
    }

    /** El cebador le pasa el mate a otro: los dos quedan en Confraternidad. */
    public static void convidar(ItemStack mate, PlayerEntity cebador, LivingEntity otro) {
        Fase5.confraternidad(otro, CONVIDAR, TOPE);
        Fase5.confraternidad(cebador, TOMAR / 2, TOPE);
        cebador.level.playSound(null, otro.getX(), otro.getY(), otro.getZ(), SoundEvents.GENERIC_DRINK,
                SoundCategory.PLAYERS, 0.8F, 1.1F);
        if (otro instanceof GauchoEntity) {
            ((GauchoEntity) otro).say(GRACIAS[cebador.getRandom().nextInt(GRACIAS.length)]);
        } else if (otro instanceof PlayerEntity) {
            ((PlayerEntity) otro).displayClientMessage(new StringTextComponent(
                    cebador.getName().getString() + " te convidó un mate."), true);
        }
        if (cebador instanceof ServerPlayerEntity) {
            Logros.award((ServerPlayerEntity) cebador, Logros.EL_CEBA);
            com.trucoya.progreso.Diario.evento((ServerPlayerEntity) cebador, com.trucoya.progreso.Diario.MATE);
        }
        usarCebada(mate, cebador);
    }

    @Override
    public ITextComponent getName(ItemStack stack) {
        switch (estado(stack)) {
            case CARGADO: return new TranslationTextComponent("item.trucoya.mate.cargado");
            case CEBADO: return new TranslationTextComponent("item.trucoya.mate.cebado");
            default: return new TranslationTextComponent("item.trucoya.mate");
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tip, ITooltipFlag flag) {
        switch (estado(stack)) {
            case CARGADO:
                tip.add(new StringTextComponent("Quedan " + cebadas(stack) + " cebadas. Cebalo con la pava caliente.")
                        .withStyle(TextFormatting.GRAY));
                break;
            case CEBADO:
                tip.add(new StringTextComponent("Mantené clic derecho para tomar, o clic derecho sobre alguien para convidar.")
                        .withStyle(TextFormatting.GRAY));
                break;
            default:
                tip.add(new StringTextComponent("Cargalo con Yerba Mate en la otra mano.").withStyle(TextFormatting.GRAY));
        }
    }
}
