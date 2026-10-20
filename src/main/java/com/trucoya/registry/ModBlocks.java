package com.trucoya.registry;

import com.trucoya.TrucoYa;
import com.trucoya.block.AnotadorBlock;
import com.trucoya.block.MesaTrucoBlock;
import com.trucoya.block.SillaCriollaBlock;
import com.trucoya.fase5.PicadaBlock;
import com.trucoya.fase5.YerbalBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TrucoYa.MOD_ID);

    public static final RegistryObject<Block> SILLA_CRIOLLA = BLOCKS.register("silla_criolla",
            () -> new SillaCriollaBlock(AbstractBlock.Properties.of(Material.WOOD)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> MESA_TRUCO = BLOCKS.register("mesa_truco",
            () -> new MesaTrucoBlock(AbstractBlock.Properties.of(Material.WOOD)
                    .strength(2.5F, 3.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> ANOTADOR = BLOCKS.register("anotador",
            () -> new AnotadorBlock(AbstractBlock.Properties.of(Material.WOOD)
                    .strength(0.5F).sound(SoundType.WOOD).noOcclusion()));

    // ------------------------------------------------------------ Fase 5
    public static final RegistryObject<Block> YERBAL = BLOCKS.register("yerbal",
            () -> new YerbalBlock(AbstractBlock.Properties.of(Material.PLANT).noCollission().randomTicks()
                    .instabreak().sound(SoundType.CROP)));

    public static final RegistryObject<Block> TABLA_PICADA = BLOCKS.register("tabla_picada",
            () -> new PicadaBlock(AbstractBlock.Properties.of(Material.WOOD).strength(0.5F)
                    .sound(SoundType.WOOD).noOcclusion()));

    // ------------------------------------------------------------ El Camino del Truco
    public static final RegistryObject<Block> RAYA_CUADRERA = BLOCKS.register("raya_cuadrera",
            () -> new com.trucoya.block.RayaCuadreraBlock(AbstractBlock.Properties.of(Material.WOOD)
                    .strength(1.0F).sound(SoundType.WOOD).noOcclusion()));

    private ModBlocks() {
    }
}
