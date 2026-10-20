package com.trucoya.registry;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;

/** Fase 5: lo que vende (y compra) el pulpero, además de los muebles y la ficha. */
public final class ModItemsFase5 {

    static void addCantineroTrades(MerchantOffers o) {
        // mate
        o.add(oferta(new ItemStack(Items.EMERALD, 1), new ItemStack(ModItems.YERBA_MATE.get(), 3)));
        o.add(oferta(new ItemStack(Items.EMERALD, 1), new ItemStack(ModItems.SEMILLAS_YERBA.get(), 4)));
        o.add(oferta(new ItemStack(Items.EMERALD, 3), new ItemStack(ModItems.MATE.get())));
        o.add(oferta(new ItemStack(Items.EMERALD, 4), new ItemStack(ModItems.PAVA.get())));
        // picada y bebidas
        o.add(oferta(new ItemStack(Items.EMERALD, 1), new ItemStack(ModItems.SALAME.get(), 3)));
        o.add(oferta(new ItemStack(Items.EMERALD, 1), new ItemStack(ModItems.QUESO.get(), 3)));
        o.add(oferta(new ItemStack(Items.EMERALD, 3), new ItemStack(ModItems.TABLA_PICADA.get())));
        o.add(oferta(new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.VINO_TINTO.get())));
        o.add(oferta(new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.CANA.get())));
        o.add(oferta(new ItemStack(Items.EMERALD, 3), new ItemStack(ModItems.GRAPAMIEL.get())));
        // discos
        o.add(oferta(new ItemStack(Items.EMERALD, 12), new ItemStack(ModItems.DISCO_MILONGA.get())));
        o.add(oferta(new ItemStack(Items.EMERALD, 12), new ItemStack(ModItems.DISCO_VALS.get())));
        // el pulpero también compra lo que produce el campo
        o.add(new MerchantOffer(new ItemStack(ModItems.HOJAS_YERBA.get(), 12), new ItemStack(Items.EMERALD, 1), 999, 2, 0.05F));
    }

    private static MerchantOffer oferta(ItemStack precio, ItemStack producto) {
        return new MerchantOffer(precio, producto, 999, 1, 0.05F);
    }

    private ModItemsFase5() {
    }
}
