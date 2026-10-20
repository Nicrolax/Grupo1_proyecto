package com.trucoya;

import com.trucoya.entity.GauchoEntity;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.registry.ModBlocks;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import com.trucoya.registry.ModSounds;
import com.trucoya.registry.ModTileEntities;
import com.trucoya.world.ModStructures;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Punto de entrada del mod "Truco Ya" (Minecraft 1.16.5 / Forge 36.2.39).
 *
 * La lógica del juego vive en {@code com.trucoya.core} y no depende de Minecraft:
 * el servidor es la única autoridad sobre el estado de la partida y los clientes
 * solo reciben vistas filtradas ({@link com.trucoya.core.PlayerView}).
 */
@Mod(TrucoYa.MOD_ID)
public class TrucoYa {

    public static final String MOD_ID = "trucoya";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final ItemGroup GROUP = new ItemGroup(MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModBlocks.MESA_TRUCO.get());
        }
    };

    public TrucoYa() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModTileEntities.TILE_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModStructures.STRUCTURES.register(modBus);
        com.trucoya.registry.ModEffects.EFFECTS.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                TrucoConfig.SPEC);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::entityAttributes);
    }

    private void entityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.GAUCHO.get(), GauchoEntity.createAttributes().build());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(TrucoNetwork::register);
        event.enqueueWork(ModStructures::setup);
        LOGGER.info("Truco Ya: motor de Truco Uruguayo cargado.");
    }
}
