package com.trucoya.client;

import com.trucoya.TrucoYa;
import com.trucoya.fase5.MateItem;
import com.trucoya.fase5.PavaItem;
import com.trucoya.registry.ModBlocks;
import com.trucoya.registry.ModEntities;
import com.trucoya.registry.ModItems;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registro de renderizadores y teclas (bus del mod, solo cliente). */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SEAT.get(), SeatRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GAUCHO.get(), GauchoRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BOLEADORAS.get(),
                m -> new SpriteRenderer<>(m, Minecraft.getInstance().getItemRenderer()));
        ClientRegistry.bindTileEntityRenderer(com.trucoya.registry.ModTileEntities.MESA_TRUCO.get(), MesaTrucoRenderer::new);
        event.enqueueWork(() -> {
            ClientRegistry.registerKeyBinding(ModKeys.SENAS);
            ClientRegistry.registerKeyBinding(ModKeys.TAPETE);
            // Fase 5: plantas con transparencia y dibujos según el estado del mate y la pava
            RenderTypeLookup.setRenderLayer(ModBlocks.YERBAL.get(), RenderType.cutout());
            ItemModelsProperties.register(ModItems.MATE.get(), new ResourceLocation(TrucoYa.MOD_ID, "estado"),
                    (stack, world, entity) -> MateItem.estado(stack));
            ItemModelsProperties.register(ModItems.PAVA.get(), new ResourceLocation(TrucoYa.MOD_ID, "estado"),
                    (stack, world, entity) -> PavaItem.estado(stack));
        });
    }

    private ClientSetup() {
    }
}
