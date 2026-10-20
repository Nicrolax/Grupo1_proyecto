package com.trucoya.registry;

import com.trucoya.TrucoYa;
import com.trucoya.entity.BoleadorasEntity;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.entity.SeatEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, TrucoYa.MOD_ID);

    /** Entidad invisible sobre la que "monta" el jugador sentado en la Silla Criolla. */
    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, EntityClassification.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .noSummon()
                    .build(TrucoYa.MOD_ID + ":seat"));

    /** Gaucho jugador de truco (paisano, cantinero, forastero o el Viejo Vizcacha). */
    public static final RegistryObject<EntityType<GauchoEntity>> GAUCHO = ENTITIES.register("gaucho",
            () -> EntityType.Builder.<GauchoEntity>of(GauchoEntity::new, EntityClassification.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(TrucoYa.MOD_ID + ":gaucho"));

    /** Boleadoras lanzadas. */
    public static final RegistryObject<EntityType<BoleadorasEntity>> BOLEADORAS = ENTITIES.register("boleadoras",
            () -> EntityType.Builder.<BoleadorasEntity>of(BoleadorasEntity::new, EntityClassification.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(TrucoYa.MOD_ID + ":boleadoras"));

    private ModEntities() {
    }
}
