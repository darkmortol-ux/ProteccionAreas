package com.miplugin.protecciones.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ClaimWand {

    private final NamespacedKey sizeKey;
    private final Material material;

    public ClaimWand(Plugin plugin, Material material) {
        this.sizeKey = new NamespacedKey(plugin, "proteccion_wand_size");
        this.material = material;
    }

    public ItemStack create(int sizeBlocks) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Palo de Protección " + ChatColor.WHITE + "(" + sizeBlocks + "x" + sizeBlocks + ")");
        meta.setLore(List.of(
                ChatColor.GRAY + "Haz click derecho sobre un bloque",
                ChatColor.GRAY + "para crear tu protección de " + sizeBlocks + "x" + sizeBlocks + " ahí.",
                ChatColor.DARK_GRAY + "Este palo se consume al usarse."
        ));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(sizeKey, PersistentDataType.INTEGER, sizeBlocks);
        item.setItemMeta(meta);
        return item;
    }

    /** Devuelve el tamaño (16 o 32) si el item es un palo de protección válido, o -1 si no lo es. */
    public int getSize(ItemStack item) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) return -1;
        Integer size = item.getItemMeta().getPersistentDataContainer().get(sizeKey, PersistentDataType.INTEGER);
        return size != null ? size : -1;
    }
}
