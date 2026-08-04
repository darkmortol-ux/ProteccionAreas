package com.miplugin.protecciones.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CustomItems {

    public static final String HEAVY_GUNPOWDER_KEY = "polvo_ignicion_pesado";
    public static final String HEAVY_TNT_KEY = "tnt_uso_pesado";

    private final NamespacedKey heavyGunpowderKey;
    private final NamespacedKey heavyTntKey;

    public CustomItems(org.bukkit.plugin.Plugin plugin) {
        this.heavyGunpowderKey = new NamespacedKey(plugin, HEAVY_GUNPOWDER_KEY);
        this.heavyTntKey = new NamespacedKey(plugin, HEAVY_TNT_KEY);
    }

    public ItemStack createHeavyGunpowder(int amount) {
        ItemStack item = new ItemStack(Material.GUNPOWDER, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "POLVO DE IGNICIÓN PESADO");
        meta.setLore(List.of(
                ChatColor.GRAY + "Un polvo inestable usado para",
                ChatColor.GRAY + "fabricar la TNT de uso pesado."
        ));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(heavyGunpowderKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createHeavyTnt(int amount) {
        ItemStack item = new ItemStack(Material.TNT, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "TNT DE USO PESADO");
        meta.setLore(List.of(
                ChatColor.GRAY + "Puede destruir construcciones",
                ChatColor.GRAY + "dentro de zonas protegidas.",
                ChatColor.DARK_GRAY + "No destruye cofres, hornos,",
                ChatColor.DARK_GRAY + "mesas ni otros objetos de utilidad."
        ));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(heavyTntKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHeavyGunpowder(ItemStack item) {
        if (item == null || item.getType() != Material.GUNPOWDER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(heavyGunpowderKey, PersistentDataType.BYTE);
    }

    public boolean isHeavyTnt(ItemStack item) {
        if (item == null || item.getType() != Material.TNT || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(heavyTntKey, PersistentDataType.BYTE);
    }

    public NamespacedKey getHeavyGunpowderKey() {
        return heavyGunpowderKey;
    }

    public NamespacedKey getHeavyTntKey() {
        return heavyTntKey;
    }
}
