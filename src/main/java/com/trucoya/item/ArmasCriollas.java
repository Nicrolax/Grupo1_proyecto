package com.trucoya.item;

import net.minecraft.item.IItemTier;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;

/** Materiales de las armas gauchas. */
public enum ArmasCriollas implements IItemTier {
    /** Facón de hoja de hierro: corto y rápido. */
    FACON(2, 420, 6.0F, 2.0F, 14, Ingredient.of(Items.IRON_INGOT)),
    /** Facón platero: hoja templada y cabo con virola de oro; más filo y más duración. */
    PLATERO(2, 900, 7.0F, 3.0F, 22, Ingredient.of(Items.GOLD_INGOT, Items.IRON_INGOT)),
    /** Rebenque de cuero trenzado. */
    CUERO(0, 160, 2.0F, 0.0F, 15, Ingredient.of(Items.LEATHER));

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchant;
    private final Ingredient repair;

    ArmasCriollas(int level, int uses, float speed, float damage, int enchant, Ingredient repair) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchant = enchant;
        this.repair = repair;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return enchant; }
    @Override public Ingredient getRepairIngredient() { return repair; }
}
