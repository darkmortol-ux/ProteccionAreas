package com.miplugin.protecciones.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class CustomRecipes {

    public static void registerAll(Plugin plugin, CustomItems items) {
        registerHeavyGunpowder(plugin, items);
        registerHeavyTnt(plugin, items);
    }

    /** 9 pólvora normal en la mesa de crafteo -> 1 Polvo de Ignición Pesado */
    private static void registerHeavyGunpowder(Plugin plugin, CustomItems items) {
        NamespacedKey key = new NamespacedKey(plugin, "polvo_ignicion_pesado_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, items.createHeavyGunpowder(1));
        recipe.shape("PPP", "PPP", "PPP");
        recipe.setIngredient('P', new RecipeChoice.MaterialChoice(Material.GUNPOWDER));
        plugin.getServer().addRecipe(recipe);
    }

    /**
     * 4 Polvos de Ignición Pesado en las esquinas, TNT normal en el centro,
     * 1 Hilo arriba, 1 Polvo de Blaze a cada lado de la TNT, y 1 Crema de
     * Magma abajo -> 1 TNT DE USO PESADO
     */
    private static void registerHeavyTnt(Plugin plugin, CustomItems items) {
        NamespacedKey key = new NamespacedKey(plugin, "tnt_uso_pesado_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, items.createHeavyTnt(1));
        recipe.shape("HSH", "BTB", "HMH");
        recipe.setIngredient('H', new RecipeChoice.ExactChoice(items.createHeavyGunpowder(1)));
        recipe.setIngredient('S', new RecipeChoice.MaterialChoice(Material.STRING));
        recipe.setIngredient('B', new RecipeChoice.MaterialChoice(Material.BLAZE_POWDER));
        recipe.setIngredient('T', new RecipeChoice.MaterialChoice(Material.TNT));
        recipe.setIngredient('M', new RecipeChoice.MaterialChoice(Material.MAGMA_CREAM));
        plugin.getServer().addRecipe(recipe);
    }
}
