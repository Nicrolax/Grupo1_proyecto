package com.trucoya.client;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoAspectos;
import com.trucoya.entity.GauchoEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

import java.util.List;

/** Modelo GeckoLib del gaucho: una geometría y una textura por rol. */
@OnlyIn(Dist.CLIENT)
public class GauchoModel extends AnimatedGeoModel<GauchoEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(TrucoYa.MOD_ID, "geo/gaucho.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(TrucoYa.MOD_ID, "animations/gaucho.animation.json");
    private static final ResourceLocation[][] TEXTURES = new ResourceLocation[GauchoEntity.Rol.values().length][];

    static {
        for (GauchoEntity.Rol rol : GauchoEntity.Rol.values()) {
            int n = GauchoAspectos.variantes(rol);
            TEXTURES[rol.ordinal()] = new ResourceLocation[n];
            for (int i = 0; i < n; i++) {
                TEXTURES[rol.ordinal()][i] = new ResourceLocation(TrucoYa.MOD_ID, "textures/entity/gaucho_"
                        + rol.name().toLowerCase(java.util.Locale.ROOT) + "_" + i + ".png");
            }
        }
    }

    private static int variante(GauchoEntity e) {
        return GauchoAspectos.normalizar(e.getRol(), e.getVariante());
    }

    @Override
    public ResourceLocation getModelLocation(GauchoEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(GauchoEntity entity) {
        return TEXTURES[entity.getRol().ordinal()][variante(entity)];
    }

    @Override
    public ResourceLocation getAnimationFileLocation(GauchoEntity entity) {
        return ANIMATIONS;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setLivingAnimations(GauchoEntity entity, Integer uniqueID, AnimationEvent event) {
        super.setLivingAnimations(entity, uniqueID, event);
        IBone head = this.getAnimationProcessor().getBone("head");
        List<EntityModelData> data = event.getExtraDataOfType(EntityModelData.class);
        if (head != null && !data.isEmpty()) {
            EntityModelData d = data.get(0);
            head.setRotationX(d.headPitch * ((float) Math.PI / 180F));
            head.setRotationY(d.netHeadYaw * ((float) Math.PI / 180F));
        }
        // Partes del modelo según la pilcha: sombrero/boina, ala, pelo largo, pollera, guitarra.
        int f = GauchoAspectos.flags(entity.getRol(), variante(entity));
        ocultar("hat", (f & GauchoAspectos.SOMBRERO) == 0);
        ocultar("brim", (f & GauchoAspectos.ALA) == 0);
        ocultar("hair_long", (f & GauchoAspectos.PELO_LARGO) == 0);
        ocultar("skirt", (f & GauchoAspectos.POLLERA) == 0);
        ocultar("guitar", (f & GauchoAspectos.GUITARRA) == 0);
        ocultar("knife", !entity.isGuardia());
    }

    private void ocultar(String hueso, boolean oculto) {
        IBone b = this.getAnimationProcessor().getBone(hueso);
        if (b != null) {
            b.setHidden(oculto);
        }
    }
}
