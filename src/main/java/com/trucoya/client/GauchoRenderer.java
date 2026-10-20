package com.trucoya.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.trucoya.entity.GauchoEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class GauchoRenderer extends GeoEntityRenderer<GauchoEntity> {

    /** Luz plena: el fantasma brilla con luz propia, aunque sea de noche. */
    private static final int LUZ_PLENA = 0xF000F0;

    public GauchoRenderer(EntityRendererManager manager) {
        super(manager, new GauchoModel());
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(GauchoEntity entity, float yaw, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer,
                       int packedLight) {
        if (entity.desvanecido() >= 0.995F) {
            return;     // ya se esfumó del todo
        }
        super.render(entity, yaw, partialTicks, stack, buffer, entity.esFantasma() ? LUZ_PLENA : packedLight);
        if (entity.ofrecePedido() && !entity.isInvisible() && net.minecraft.client.Minecraft.renderNames()
                && this.entityRenderDispatcher.distanceToSqr(entity) < 32.0D * 32.0D) {
            cartelPedido(entity, partialTicks, stack, buffer, packedLight);
        }
    }

    /** "¡Pedido!" flotando sobre la cabeza, por encima del nombre. */
    private void cartelPedido(GauchoEntity entity, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int luz) {
        stack.pushPose();
        float sube = MathHelper.sin((entity.tickCount + partialTicks) * 0.12F) * 0.06F;
        stack.translate(0.0D, entity.getBbHeight() + 0.85D + sube, 0.0D);
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        stack.scale(-0.03F, -0.03F, 0.03F);
        net.minecraft.util.math.vector.Matrix4f m = stack.last().pose();
        net.minecraft.client.gui.FontRenderer f = this.getFont();
        String txt = "¡Pedido!";
        float x = -f.width(txt) / 2.0F;
        int fondo = (int) (net.minecraft.client.Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        f.drawInBatch(txt, x, 0, 0x20E8B82A, false, m, buffer, true, fondo, luz);
        f.drawInBatch(txt, x, 0, 0xFFF2C94C, false, m, buffer, false, 0, LUZ_PLENA);
        stack.popPose();
    }

    /** El fantasma de Ledesma (y el Mandinga cuando se esfuma) se dibujan traslúcidos. */
    @Override
    public RenderType getRenderType(GauchoEntity entity, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer,
                                    IVertexBuilder vertexBuilder, int packedLight, ResourceLocation texture) {
        if (entity.esFantasma() || entity.desvanecido() > 0.0F) {
            return RenderType.entityTranslucent(texture);
        }
        return super.getRenderType(entity, partialTicks, stack, buffer, vertexBuilder, packedLight, texture);
    }

    @Override
    public Color getRenderColor(GauchoEntity entity, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer,
                                IVertexBuilder vertexBuilder, int packedLight) {
        float queda = 1.0F - entity.desvanecido();
        if (entity.esFantasma()) {
            float t = entity.tickCount + partialTicks;
            float titila = 0.06F * MathHelper.sin(t * 0.21F) + 0.03F * MathHelper.sin(t * 0.77F + 1.3F);
            return Color.ofRGBA(0.70F, 0.84F, 1.0F, MathHelper.clamp((0.52F + titila) * queda, 0.0F, 1.0F));
        }
        if (queda < 1.0F) {
            return Color.ofRGBA(1.0F, 0.75F, 0.7F, MathHelper.clamp(queda, 0.0F, 1.0F));
        }
        return super.getRenderColor(entity, partialTicks, stack, buffer, vertexBuilder, packedLight);
    }

    /** Los caídos de la cinemática se ven tendidos, sin el tinte rojo de "herido". */
    @Override
    public int getOverlay(GauchoEntity entity, float u) {
        return entity.actorCinematica ? OverlayTexture.NO_OVERLAY : super.getOverlay(entity, u);
    }

    @Override
    public int getPackedOverlay(LivingEntity entity, float u) {
        if (entity instanceof GauchoEntity && ((GauchoEntity) entity).actorCinematica) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(entity, u);
    }
}
