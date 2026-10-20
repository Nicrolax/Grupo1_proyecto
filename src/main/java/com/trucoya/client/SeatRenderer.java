package com.trucoya.client;

import com.trucoya.entity.SeatEntity;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** El asiento es invisible: nunca se dibuja. */
@OnlyIn(Dist.CLIENT)
public class SeatRenderer extends EntityRenderer<SeatEntity> {

    public SeatRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public boolean shouldRender(SeatEntity entity, ClippingHelper camera, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        return PlayerContainer.BLOCK_ATLAS;
    }
}
