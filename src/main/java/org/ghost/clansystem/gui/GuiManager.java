package org.ghost.clansystem.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.ghost.clansystem.ClanSystem;
import java.util.*;

public class GuiManager {

    private final ClanSystem plugin;
    private final Map<UUID, UUID> glowTargetSelection = new HashMap<>();

    public GuiManager(ClanSystem plugin) {
        this.plugin = plugin;
    }

    public void openGlowGui(Player player, String clan) {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.getConfigManager().getSingleMessage("glow-title"));
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        grayMeta.setDisplayName(" ");
        grayPane.setItemMeta(grayMeta);
        for (int i = 0; i < 9; i++) inv.setItem(i + 27, grayPane);
        Color[] niceColors = {
                Color.fromRGB(255, 0, 0), Color.fromRGB(255, 127, 0), Color.fromRGB(255, 255, 0),
                Color.fromRGB(0, 255, 0), Color.fromRGB(0, 255, 255), Color.fromRGB(0, 0, 255),
                Color.fromRGB(139, 0, 255), Color.fromRGB(255, 0, 255), Color.fromRGB(255, 255, 255),
                Color.fromRGB(0, 0, 0), Color.fromRGB(128, 128, 128), Color.fromRGB(255, 192, 203),
                Color.fromRGB(210, 105, 30), Color.fromRGB(0, 100, 0), Color.fromRGB(75, 0, 130),
                Color.fromRGB(255, 20, 147), Color.fromRGB(0, 128, 128), Color.fromRGB(255, 215, 0),
                Color.fromRGB(165, 42, 42), Color.fromRGB(220, 20, 60), Color.fromRGB(64, 224, 208)
        };
        for (int i = 0; i < niceColors.length; i++) {
            if (i >= 27) break;
            ItemStack armor = new ItemStack(Material.LEATHER_CHESTPLATE);
            LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
            meta.setColor(niceColors[i]);
            meta.setDisplayName(ChatColor.YELLOW + "Цвет #" + (i + 1));
            armor.setItemMeta(meta);
            inv.setItem(i, armor);
        }
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.setDisplayName(ChatColor.RED + "Снять подсветку");
        reset.setItemMeta(resetMeta);
        inv.setItem(26, reset);
        List<String> members = plugin.getClanManager().getClanMembers(clan);
        int slot = 36;
        for (String memberUuidStr : members) {
            if (slot >= 54) break;
            UUID memberUuid = UUID.fromString(memberUuidStr);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUuid));
            skullMeta.setDisplayName(ChatColor.GREEN + Bukkit.getOfflinePlayer(memberUuid).getName());
            skullMeta.setLore(Arrays.asList(ChatColor.GRAY + "Нажмите, чтобы выбрать игрока", ChatColor.GRAY + "для смены цвета"));
            skull.setItemMeta(skullMeta);
            inv.setItem(slot, skull);
            slot++;
        }
        player.openInventory(inv);
    }

    public void openShop(Player player, String clan, int level) {
        Inventory shop = Bukkit.createInventory(null, 54, plugin.getConfigManager().getSingleMessage("shop-title", "%level%", String.valueOf(level)));
        ItemStack yellowPane = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta yellowMeta = yellowPane.getItemMeta();
        yellowMeta.setDisplayName(" ");
        yellowPane.setItemMeta(yellowMeta);
        ItemStack orangePane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta orangeMeta = orangePane.getItemMeta();
        orangeMeta.setDisplayName(" ");
        orangePane.setItemMeta(orangeMeta);
        for (int i = 0; i < 9; i++) {
            shop.setItem(i, yellowPane);
            shop.setItem(45 + i, yellowPane);
        }
        for (int i = 1; i < 5; i++) {
            shop.setItem(i * 9, orangePane);
            shop.setItem(i * 9 + 8, orangePane);
        }
        if (level >= 3) addBasicSet(shop, clan);
        if (level >= 7) addWarriorSet(shop, clan);
        if (level >= 10) addLegendSet(shop, clan);
        player.openInventory(shop);
    }

    private void addBasicSet(Inventory inv, String clan) {
        inv.setItem(10, createShopItem(Material.DIAMOND_HELMET, plugin.getConfigManager().getSingleMessage("basic-helmet-name"), 15000, clan, 0));
        inv.setItem(11, createShopItem(Material.DIAMOND_CHESTPLATE, plugin.getConfigManager().getSingleMessage("basic-chestplate-name"), 20000, clan, 1));
        inv.setItem(12, createShopItem(Material.DIAMOND_LEGGINGS, plugin.getConfigManager().getSingleMessage("basic-leggings-name"), 18000, clan, 2));
        inv.setItem(13, createShopItem(Material.DIAMOND_BOOTS, plugin.getConfigManager().getSingleMessage("basic-boots-name"), 12000, clan, 3));
        inv.setItem(14, createShopItem(Material.DIAMOND_SWORD, plugin.getConfigManager().getSingleMessage("basic-sword-name"), 25000, clan, 4));
        inv.setItem(17, createShopItem(Material.BARRIER, plugin.getConfigManager().getSingleMessage("basic-full-set-name"), 90000, clan, 5));
    }

    private void addWarriorSet(Inventory inv, String clan) {
        inv.setItem(19, createShopItem(Material.DIAMOND_HELMET, plugin.getConfigManager().getSingleMessage("warrior-helmet-name"), 30000, clan, 10));
        inv.setItem(20, createShopItem(Material.DIAMOND_CHESTPLATE, plugin.getConfigManager().getSingleMessage("warrior-chestplate-name"), 40000, clan, 11));
        inv.setItem(21, createShopItem(Material.DIAMOND_LEGGINGS, plugin.getConfigManager().getSingleMessage("warrior-leggings-name"), 35000, clan, 12));
        inv.setItem(22, createShopItem(Material.DIAMOND_BOOTS, plugin.getConfigManager().getSingleMessage("warrior-boots-name"), 25000, clan, 13));
        inv.setItem(23, createShopItem(Material.DIAMOND_SWORD, plugin.getConfigManager().getSingleMessage("warrior-sword-name"), 50000, clan, 14));
        inv.setItem(26, createShopItem(Material.BARRIER, plugin.getConfigManager().getSingleMessage("warrior-full-set-name"), 180000, clan, 15));
    }

    private void addLegendSet(Inventory inv, String clan) {
        inv.setItem(28, createShopItem(Material.NETHERITE_HELMET, plugin.getConfigManager().getSingleMessage("legend-helmet-name"), 80000, clan, 20));
        inv.setItem(29, createShopItem(Material.NETHERITE_CHESTPLATE, plugin.getConfigManager().getSingleMessage("legend-chestplate-name"), 100000, clan, 21));
        inv.setItem(30, createShopItem(Material.NETHERITE_LEGGINGS, plugin.getConfigManager().getSingleMessage("legend-leggings-name"), 90000, clan, 22));
        inv.setItem(31, createShopItem(Material.NETHERITE_BOOTS, plugin.getConfigManager().getSingleMessage("legend-boots-name"), 70000, clan, 23));
        inv.setItem(32, createShopItem(Material.NETHERITE_SWORD, plugin.getConfigManager().getSingleMessage("legend-sword-name"), 120000, clan, 24));
        inv.setItem(35, createShopItem(Material.BARRIER, plugin.getConfigManager().getSingleMessage("legend-full-set-name"), 460000, clan, 25));
    }

    private ItemStack createShopItem(Material mat, String name, int cost, String clan, int id) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String processedName = name.replace(";", "");
        meta.setDisplayName(ChatColor.AQUA + processedName);
        String costLore = plugin.getConfigManager().getSingleMessage("cost-lore", "%cost%", String.valueOf(cost)).replace(";", "");
        String idLore = plugin.getConfigManager().getSingleMessage("id-lore", "%id%", String.valueOf(id)).replace(";", "");
        meta.setLore(Arrays.asList(costLore, idLore));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public int getCostById(int id) {
        return switch (id) {
            case 0 -> 15000; case 1 -> 20000; case 2 -> 18000; case 3 -> 12000; case 4 -> 25000; case 5 -> 90000;
            case 10 -> 30000; case 11 -> 40000; case 12 -> 35000; case 13 -> 25000; case 14 -> 50000; case 15 -> 180000;
            case 20 -> 80000; case 21 -> 100000; case 22 -> 90000; case 23 -> 70000; case 24 -> 120000; case 25 -> 460000;
            default -> 0;
        };
    }

    public ItemStack[] getRewardsById(int id) {
        return switch (id) {
            case 0 -> new ItemStack[]{enchanted(Material.DIAMOND_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2)};
            case 1 -> new ItemStack[]{enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2)};
            case 2 -> new ItemStack[]{enchanted(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2)};
            case 3 -> new ItemStack[]{enchanted(Material.DIAMOND_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2)};
            case 4 -> new ItemStack[]{enchanted(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 3, Enchantment.FIRE_ASPECT, 1, Enchantment.DURABILITY, 3)};
            case 5 -> new ItemStack[]{
                    enchanted(Material.DIAMOND_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2),
                    enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2),
                    enchanted(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2),
                    enchanted(Material.DIAMOND_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 2),
                    enchanted(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 3, Enchantment.FIRE_ASPECT, 1, Enchantment.DURABILITY, 3)
            };
            case 10 -> new ItemStack[]{enchanted(Material.DIAMOND_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 11 -> new ItemStack[]{enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 12 -> new ItemStack[]{enchanted(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 13 -> new ItemStack[]{enchanted(Material.DIAMOND_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 14 -> new ItemStack[]{enchanted(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.DURABILITY, 3)};
            case 15 -> new ItemStack[]{
                    enchanted(Material.DIAMOND_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.DIAMOND_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.DURABILITY, 3)
            };
            case 20 -> new ItemStack[]{enchanted(Material.NETHERITE_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 21 -> new ItemStack[]{enchanted(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 22 -> new ItemStack[]{enchanted(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 23 -> new ItemStack[]{enchanted(Material.NETHERITE_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3)};
            case 24 -> new ItemStack[]{enchanted(Material.NETHERITE_SWORD, Enchantment.DAMAGE_ALL, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.DURABILITY, 3)};
            case 25 -> new ItemStack[]{
                    enchanted(Material.NETHERITE_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.NETHERITE_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3),
                    enchanted(Material.NETHERITE_SWORD, Enchantment.DAMAGE_ALL, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.DURABILITY, 3)
            };
            default -> new ItemStack[]{};
        };
    }

    private ItemStack enchanted(Material mat, Object... pairs) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i < pairs.length; i += 2) {
            Enchantment ench = (Enchantment) pairs[i];
            int level = (Integer) pairs[i + 1];
            meta.addEnchant(ench, level, true);
        }
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public Map<UUID, UUID> getGlowTargetSelection() { return glowTargetSelection; }
}
