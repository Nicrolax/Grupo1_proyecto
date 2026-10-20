package com.trucoya.fase5;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.MagmaBlock;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Pava: clic derecho mirando agua para llenarla; clic derecho sobre una fogata, horno o fuego
 * encendidos para calentarla. Caliente alcanza para 12 cebadas.
 */
public class PavaItem extends Item {

    public static final int VACIA = 0, FRIA = 1, CALIENTE = 2;
    public static final int USOS = 12;

    public PavaItem(Properties props) {
        super(props);
    }

    public static int estado(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("Agua") : VACIA;
    }

    public static int usos(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("Usos") : 0;
    }

    private static void set(ItemStack s, int estado, int usos) {
        CompoundNBT t = s.getOrCreateTag();
        t.putInt("Agua", estado);
        t.putInt("Usos", usos);
    }

    /** Gasta una cebada de agua caliente. Devuelve false si no hay. */
    public static boolean servir(ItemStack s) {
        if (estado(s) != CALIENTE || usos(s) <= 0) {
            return false;
        }
        int u = usos(s) - 1;
        set(s, u > 0 ? CALIENTE : VACIA, u);
        return true;
    }

    /** Qué le falta a lo que hay en la otra mano para poder cebar (mensaje claro, según el caso). */
    static String queFalta(ItemStack otra) {
        if (!(otra.getItem() instanceof PavaItem)) {
            return "Para cebar, tené la Pava con agua caliente en la otra mano.";
        }
        switch (estado(otra)) {
            case FRIA:
                return "La pava tiene agua fría: calentala con clic derecho sobre una fogata prendida (o un horno prendido).";
            case VACIA:
                return "La pava está vacía: llenala con clic derecho mirando agua, y después calentala en el fuego.";
            default:
                return "Para cebar, tené la Pava con agua caliente en la otra mano.";
        }
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // con la pava en la mano también se ceba: el mate cargado en la otra
        ItemStack otra = player.getItemInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
        if (estado(stack) == CALIENTE && otra.getItem() instanceof MateItem) {
            int em = MateItem.estado(otra);
            if (em == MateItem.CARGADO) {
                MateItem.cebar(world, player, otra, stack);
                return ActionResult.sidedSuccess(stack, world.isClientSide);
            }
            if (!world.isClientSide) {
                player.displayClientMessage(new StringTextComponent(em == MateItem.CEBADO
                        ? "El mate ya está cebado: tomalo (mantené clic derecho con el mate) o convidalo."
                        : "Primero cargá el mate: Yerba Mate en la otra mano y clic derecho con el mate."), true);
            }
            return ActionResult.fail(stack);
        }
        if (estado(stack) == VACIA) {
            BlockRayTraceResult hit = getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);
            if (hit.getType() == RayTraceResult.Type.BLOCK) {
                FluidState f = world.getFluidState(hit.getBlockPos());
                if (f.is(FluidTags.WATER)) {
                    world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL,
                            SoundCategory.PLAYERS, 1.0F, 0.8F);
                    if (!world.isClientSide) {
                        set(stack, FRIA, 0);
                        player.displayClientMessage(new StringTextComponent(
                                "Pava llena. Ahora calentala en una fogata o un horno prendidos."), true);
                    }
                    return ActionResult.sidedSuccess(stack, world.isClientSide);
                }
            }
        }
        return ActionResult.pass(stack);
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        World world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState st = world.getBlockState(pos);
        if (estado(stack) == FRIA && esFuego(st)) {
            world.playSound(ctx.getPlayer(), pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (!world.isClientSide) {
                set(stack, CALIENTE, USOS);
                if (ctx.getPlayer() != null) {
                    ctx.getPlayer().displayClientMessage(new StringTextComponent(
                            "¡El agua está pronta! Justo antes de hervir, como debe ser."), true);
                }
            }
            return ActionResultType.sidedSuccess(world.isClientSide);
        }
        return ActionResultType.PASS;
    }

    /** Calentar la pava (también en un horno: sin abrirlo). Devuelve true si la calentó. */
    public static boolean calentar(World world, BlockPos pos, ItemStack stack, @Nullable PlayerEntity player) {
        if (estado(stack) != FRIA || !esFuego(world.getBlockState(pos))) {
            return false;
        }
        world.playSound(player, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!world.isClientSide) {
            set(stack, CALIENTE, USOS);
            if (player != null) {
                player.displayClientMessage(new StringTextComponent(
                        "¡El agua está pronta! Justo antes de hervir, como debe ser."), true);
            }
        }
        return true;
    }

    static boolean esFuego(BlockState st) {
        if (st.getBlock() instanceof CampfireBlock) {
            return st.getValue(CampfireBlock.LIT);
        }
        if (st.getBlock() instanceof AbstractFurnaceBlock) {
            return st.getValue(AbstractFurnaceBlock.LIT);
        }
        return st.getBlock() instanceof FireBlock || st.getBlock() instanceof MagmaBlock;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return estado(stack) == CALIENTE;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - usos(stack) / (double) USOS;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tip, ITooltipFlag flag) {
        String t;
        switch (estado(stack)) {
            case FRIA: t = "Con agua fría: calentala en el fuego"; break;
            case CALIENTE: t = "Agua caliente: " + usos(stack) + " cebadas"; break;
            default: t = "Vacía: clic derecho mirando agua";
        }
        tip.add(new StringTextComponent(t).withStyle(TextFormatting.GRAY));
    }
}
