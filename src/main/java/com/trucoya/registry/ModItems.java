package com.trucoya.registry;

import com.trucoya.TrucoYa;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.item.BarajaDeOroItem;
import com.trucoya.item.GauchoEggItem;
import com.trucoya.item.ArmasCriollas;
import com.trucoya.item.BoleadorasItem;
import com.trucoya.item.RebenqueItem;
import com.trucoya.fase5.BebidaItem;
import com.trucoya.fase5.MateItem;
import com.trucoya.fase5.PavaItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockNamedItem;
import net.minecraft.item.Food;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.item.SwordItem;
import net.minecraft.item.Item;
import net.minecraft.item.MerchantOffers;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TrucoYa.MOD_ID);

    // Fase 2: mobiliario
    public static final RegistryObject<Item> SILLA_CRIOLLA = ITEMS.register("silla_criolla",
            () -> new BlockItem(ModBlocks.SILLA_CRIOLLA.get(), props()));
    public static final RegistryObject<Item> MESA_TRUCO = ITEMS.register("mesa_truco",
            () -> new BlockItem(ModBlocks.MESA_TRUCO.get(), props()));
    public static final RegistryObject<Item> ANOTADOR = ITEMS.register("anotador",
            () -> new BlockItem(ModBlocks.ANOTADOR.get(), props()));

    // Fase 3: progresión
    public static final RegistryObject<Item> BARAJA_DE_ORO = ITEMS.register("baraja_de_oro",
            () -> new BarajaDeOroItem(props().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    // Las barajas coleccionables (la de Oro es la de arriba)
    public static final RegistryObject<Item> BARAJA_CRIOLLA = baraja(com.trucoya.barajas.Baraja.CRIOLLA, Rarity.COMMON);
    public static final RegistryObject<Item> BARAJA_MANDINGA = baraja(com.trucoya.barajas.Baraja.MANDINGA, Rarity.EPIC);
    public static final RegistryObject<Item> BARAJA_LLANURA = baraja(com.trucoya.barajas.Baraja.LLANURA, Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARAJA_SABANA = baraja(com.trucoya.barajas.Baraja.SABANA, Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARAJA_DESIERTO = baraja(com.trucoya.barajas.Baraja.DESIERTO, Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARAJA_MONTE = baraja(com.trucoya.barajas.Baraja.MONTE, Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARAJA_NIEVE = baraja(com.trucoya.barajas.Baraja.NIEVE, Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARAJA_SIERRA = baraja(com.trucoya.barajas.Baraja.SIERRA, Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARAJA_FUNDADORES = baraja(com.trucoya.barajas.Baraja.FUNDADORES, Rarity.RARE);
    public static final RegistryObject<Item> ENCOMIENDA = ITEMS.register("encomienda",
            () -> new com.trucoya.item.EncomiendaItem(props().stacksTo(1)));
    public static final RegistryObject<Item> FICHA_CLANDESTINA = ITEMS.register("ficha_clandestina",
            () -> new Item(props().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // El Camino del Truco
    public static final RegistryObject<Item> LIBRETA = ITEMS.register("libreta_truquero",
            () -> new com.trucoya.item.LibretaItem(props().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> COPA_TORNEO = ITEMS.register("copa_torneo",
            () -> new com.trucoya.item.CopaItem(props().stacksTo(1).fireResistant().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RAYA_CUADRERA = ITEMS.register("raya_cuadrera",
            () -> new BlockItem(ModBlocks.RAYA_CUADRERA.get(), props()));

    public static final RegistryObject<Item> HUEVO_PAISANO = ITEMS.register("huevo_paisano",
            () -> new GauchoEggItem(GauchoEntity.Rol.PAISANO, 0x8B2E2E, 0xE8DCC0, props()));
    public static final RegistryObject<Item> HUEVO_CANTINERO = ITEMS.register("huevo_cantinero",
            () -> new GauchoEggItem(GauchoEntity.Rol.CANTINERO, 0xEDE6D6, 0x6B4423, props()));
    public static final RegistryObject<Item> HUEVO_FORASTERO = ITEMS.register("huevo_forastero",
            () -> new GauchoEggItem(GauchoEntity.Rol.FORASTERO, 0xB0802E, 0x3A2219, props()));
    public static final RegistryObject<Item> HUEVO_VIZCACHA = ITEMS.register("huevo_vizcacha",
            () -> new GauchoEggItem(GauchoEntity.Rol.VIZCACHA, 0x7A7A7A, 0xE8B82A, props()));
    public static final RegistryObject<Item> HUEVO_PAISANA = ITEMS.register("huevo_paisana",
            () -> new GauchoEggItem(GauchoEntity.Rol.PAISANA, 0x2F4A6B, 0xB3262C, props()));
    public static final RegistryObject<Item> HUEVO_MATRERO = ITEMS.register("huevo_matrero",
            () -> new GauchoEggItem(GauchoEntity.Rol.MATRERO, 0x2A2420, 0x7A1E22, props()));
    public static final RegistryObject<Item> HUEVO_PEON = ITEMS.register("huevo_peon",
            () -> new GauchoEggItem(GauchoEntity.Rol.PEON, 0xC9B48A, 0x5A6B3A, props()));
    public static final RegistryObject<Item> HUEVO_PAYADOR = ITEMS.register("huevo_payador",
            () -> new GauchoEggItem(GauchoEntity.Rol.PAYADOR, 0x1A1410, 0xB07A3A, props()));

    // ------------------------------------------------------------ armas gauchas
    public static final RegistryObject<Item> FACON = ITEMS.register("facon",
            () -> new SwordItem(ArmasCriollas.FACON, 2, -1.4F, props()));
    public static final RegistryObject<Item> FACON_PLATERO = ITEMS.register("facon_platero",
            () -> new SwordItem(ArmasCriollas.PLATERO, 2, -1.4F, props().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FACON_LEDESMA = ITEMS.register("facon_ledesma",
            () -> new com.trucoya.item.FaconLedesmaItem(ArmasCriollas.PLATERO, 4, -1.3F,
                    props().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> FAROL_ANIMAS = ITEMS.register("farol_animas",
            () -> new com.trucoya.item.FarolAnimasItem(props().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> REBENQUE = ITEMS.register("rebenque",
            () -> new RebenqueItem(props()));
    public static final RegistryObject<Item> BOLEADORAS = ITEMS.register("boleadoras",
            () -> new BoleadorasItem(props().stacksTo(4)));

    // ------------------------------------------------------------ Fase 5: mate
    public static final RegistryObject<Item> SEMILLAS_YERBA = ITEMS.register("semillas_yerba",
            () -> new BlockNamedItem(ModBlocks.YERBAL.get(), props()));
    public static final RegistryObject<Item> HOJAS_YERBA = ITEMS.register("hojas_yerba", () -> new Item(props()));
    public static final RegistryObject<Item> YERBA_MATE = ITEMS.register("yerba_mate", () -> new Item(props()));
    public static final RegistryObject<Item> BOMBILLA = ITEMS.register("bombilla", () -> new Item(props()));
    public static final RegistryObject<Item> MATE = ITEMS.register("mate", () -> new MateItem(props().stacksTo(1)));
    public static final RegistryObject<Item> PAVA = ITEMS.register("pava", () -> new PavaItem(props().stacksTo(1)));

    // ------------------------------------------------------------ Fase 5: picada y bebidas
    public static final RegistryObject<Item> SALAME = ITEMS.register("salame",
            () -> new Item(props().food(new Food.Builder().nutrition(4).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> QUESO = ITEMS.register("queso",
            () -> new Item(props().food(new Food.Builder().nutrition(3).saturationMod(0.5F).build())));
    public static final RegistryObject<Item> GALLETA = ITEMS.register("galleta",
            () -> new Item(props().food(new Food.Builder().nutrition(2).saturationMod(0.4F).fast().build())));
    public static final RegistryObject<Item> TABLA_PICADA = ITEMS.register("tabla_picada",
            () -> new BlockItem(ModBlocks.TABLA_PICADA.get(), props().stacksTo(4)));
    public static final RegistryObject<Item> VINO_TINTO = ITEMS.register("vino_tinto",
            () -> new BebidaItem(props().stacksTo(16), "Vino de la casa: Regeneración un ratito",
                    () -> new EffectInstance[]{new EffectInstance(Effects.REGENERATION, 200, 0)}));
    public static final RegistryObject<Item> CANA = ITEMS.register("cana",
            () -> new BebidaItem(props().stacksTo(16), "Caña: Velocidad 30 s (y un mareíto)",
                    () -> new EffectInstance[]{new EffectInstance(Effects.MOVEMENT_SPEED, 600, 0),
                            new EffectInstance(Effects.CONFUSION, 100, 0)}));
    public static final RegistryObject<Item> GRAPAMIEL = ITEMS.register("grapamiel",
            () -> new BebidaItem(props().stacksTo(16), "Grapamiel: Resistencia 30 s, ideal pal' frío",
                    () -> new EffectInstance[]{new EffectInstance(Effects.DAMAGE_RESISTANCE, 600, 0),
                            new EffectInstance(Effects.CONFUSION, 80, 0)}));

    // ------------------------------------------------------------ Fase 5: discos
    public static final RegistryObject<Item> DISCO_MILONGA = ITEMS.register("disco_milonga",
            () -> new MusicDiscItem(11, ModSounds.DISCO_MILONGA, props().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DISCO_CANDOMBE = ITEMS.register("disco_candombe",
            () -> new MusicDiscItem(12, ModSounds.DISCO_CANDOMBE, props().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DISCO_VALS = ITEMS.register("disco_vals",
            () -> new MusicDiscItem(13, ModSounds.DISCO_VALS, props().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DISCO_PAYADA = ITEMS.register("disco_payada",
            () -> new MusicDiscItem(14, ModSounds.DISCO_PAYADA, props().stacksTo(1).rarity(Rarity.RARE)));

    private static Item.Properties props() {
        return new Item.Properties().tab(TrucoYa.GROUP);
    }

    /** Ofertas extra del cantinero (las completa la Fase 5 con yerba, mate y picada). */
    public static void addCantineroTrades(MerchantOffers offers) {
        ModItemsFase5.addCantineroTrades(offers);
    }

    private ModItems() {
    }

    private static RegistryObject<Item> baraja(com.trucoya.barajas.Baraja b, Rarity rareza) {
        return ITEMS.register("baraja_" + b.clave, () -> new com.trucoya.item.BarajaItem(b, props().stacksTo(1).rarity(rareza)));
    }

    /** El ítem de cada baraja. */
    public static Item baraja(com.trucoya.barajas.Baraja b) {
        switch (b) {
            case ORO: return BARAJA_DE_ORO.get();
            case MANDINGA: return BARAJA_MANDINGA.get();
            case LLANURA: return BARAJA_LLANURA.get();
            case SABANA: return BARAJA_SABANA.get();
            case DESIERTO: return BARAJA_DESIERTO.get();
            case MONTE: return BARAJA_MONTE.get();
            case NIEVE: return BARAJA_NIEVE.get();
            case SIERRA: return BARAJA_SIERRA.get();
            case FUNDADORES: return BARAJA_FUNDADORES.get();
            default: return BARAJA_CRIOLLA.get();
        }
    }
}
