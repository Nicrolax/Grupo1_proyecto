package com.trucoya.registry;

import com.trucoya.TrucoYa;
import com.trucoya.tileentity.MesaTrucoTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModTileEntities {

    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, TrucoYa.MOD_ID);

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<MesaTrucoTileEntity>> MESA_TRUCO = TILE_ENTITIES.register("mesa_truco",
            () -> TileEntityType.Builder.of(MesaTrucoTileEntity::new, ModBlocks.MESA_TRUCO.get()).build(null));

    private ModTileEntities() {
    }
}
