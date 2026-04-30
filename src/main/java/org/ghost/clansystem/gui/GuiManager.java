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
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.ghost.clansystem.ClanSystem;
import org.ghost.clansystem.managers.ClanManager;
import java.util.*;

public class GuiManager {

    private final ClanSystem plugin;
    private final Map<UUID, UUID> glowTargetSelection = new HashMap<>();

    public GuiManager(ClanSystem plugin) {
        this.plugin = plugin;
    }

    public static final String MENU_QUEST_NBT_PREFIX = "QUEST:";

    public void openMainMenu(Player player, String clan) {
        String title = plugin.getConfigManager().getSingleMessage("menu-title", "%clan%", clan);
        Inventory menu = Bukkit.createInventory(null, 54, title);

        ItemStack yellowPane = pane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack orangePane = pane(Material.ORANGE_STAINED_GLASS_PANE);

        int[] yellowSlots = {0, 2, 4, 6, 8, 9, 17, 27, 35, 45, 47, 51, 53};
        int[] orangeSlots = {1, 3, 5, 7, 18, 26, 36, 44, 46, 52};
        for (int s : yellowSlots) menu.setItem(s, yellowPane);
        for (int s : orangeSlots) menu.setItem(s, orangePane);

        int level = plugin.getClanManager().getClanLevel(clan);
        double bank = plugin.getClanManager().getClanBank(clan);
        int kills = plugin.getClanManager().getClanKills(clan);
        double deposited = plugin.getClanManager().getClanDeposited(clan);
        boolean pvp = plugin.getClanManager().getClanPvp(clan);
        String ownerUuid = plugin.getClanManager().getClanOwner(clan);
        String ownerName = "?";
        if (ownerUuid != null) {
            String n = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid)).getName();
            if (n != null) ownerName = n;
        }
        int memberCount = plugin.getClanManager().getClanMembers(clan).size();

        ItemStack banner = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta bannerMeta = (SkullMeta) banner.getItemMeta();
        if (ownerUuid != null) {
            bannerMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid)));
        }
        bannerMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-banner-name", "%clan%", clan));
        bannerMeta.setLore(plugin.getConfigManager().getLoreList("menu-banner-lore",
                "%clan%", clan,
                "%owner%", ownerName,
                "%level%", String.valueOf(level),
                "%bank%", formatNumber(bank),
                "%kills%", String.valueOf(kills),
                "%deposited%", formatNumber(deposited),
                "%members%", String.valueOf(memberCount),
                "%pvp%", pvp ? "§a§lВкл" : "§c§lВыкл"));
        banner.setItemMeta(bannerMeta);
        menu.setItem(13, banner);

        int next = Math.min(level + 1, 10);
        int needKills = level < 10 ? plugin.getClanManager().KILLS_REQUIRED[next - 1] : plugin.getClanManager().KILLS_REQUIRED[9];
        int needCoins = level < 10 ? plugin.getClanManager().COINS_REQUIRED[next - 1] : plugin.getClanManager().COINS_REQUIRED[9];
        String killBar = buildBar(kills, needKills);
        String coinBar = buildBar((long) deposited, needCoins);
        ItemStack progressItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta progressMeta = progressItem.getItemMeta();
        progressMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-progress-name", "%level%", String.valueOf(level)));
        progressMeta.setLore(plugin.getConfigManager().getLoreList(level >= 10 ? "menu-progress-max-lore" : "menu-progress-lore",
                "%level%", String.valueOf(level),
                "%next%", String.valueOf(next),
                "%kills%", String.valueOf(kills),
                "%need_kills%", String.valueOf(needKills),
                "%deposited%", formatNumber(deposited),
                "%need_coins%", formatNumber(needCoins),
                "%kill_bar%", killBar,
                "%coin_bar%", coinBar));
        tagAction(progressMeta, "progress");
        progressItem.setItemMeta(progressMeta);
        menu.setItem(20, progressItem);

        ItemStack shopItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta shopMeta = shopItem.getItemMeta();
        shopMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-shop-name"));
        String shopLoreKey = level >= 3 ? "menu-shop-lore" : "menu-shop-locked-lore";
        shopMeta.setLore(plugin.getConfigManager().getLoreList(shopLoreKey, "%level%", String.valueOf(level)));
        tagAction(shopMeta, "shop");
        shopItem.setItemMeta(shopMeta);
        menu.setItem(22, shopItem);

        ItemStack chestItem = new ItemStack(Material.CHEST);
        ItemMeta chestMeta = chestItem.getItemMeta();
        chestMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-chest-name"));
        chestMeta.setLore(plugin.getConfigManager().getLoreList("menu-chest-lore",
                "%slots%", String.valueOf(plugin.getClanManager().CHEST_SLOTS[level - 1])));
        tagAction(chestMeta, "chest");
        chestItem.setItemMeta(chestMeta);
        menu.setItem(24, chestItem);

        ItemStack homeItem = new ItemStack(Material.COMPASS);
        ItemMeta homeMeta = homeItem.getItemMeta();
        homeMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-home-name"));
        homeMeta.setLore(plugin.getConfigManager().getLoreList("menu-home-lore"));
        tagAction(homeMeta, "home");
        homeItem.setItemMeta(homeMeta);
        menu.setItem(29, homeItem);

        ItemStack pvpItem = new ItemStack(pvp ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD);
        ItemMeta pvpMeta = pvpItem.getItemMeta();
        pvpMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-pvp-name"));
        String pvpLoreKey = pvp ? "menu-pvp-on-lore" : "menu-pvp-off-lore";
        pvpMeta.setLore(plugin.getConfigManager().getLoreList(pvpLoreKey));
        if (pvp) pvpMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        tagAction(pvpMeta, "pvp");
        pvpItem.setItemMeta(pvpMeta);
        menu.setItem(31, pvpItem);

        ItemStack glowItem = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta glowMeta = glowItem.getItemMeta();
        glowMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-glow-name"));
        glowMeta.setLore(plugin.getConfigManager().getLoreList("menu-glow-lore"));
        tagAction(glowMeta, "glow");
        glowItem.setItemMeta(glowMeta);
        menu.setItem(33, glowItem);

        List<ClanManager.QuestDef> quests = plugin.getClanManager().QUESTS;
        int[] questSlots = {37, 39, 41, 43};
        for (int i = 0; i < quests.size() && i < questSlots.length; i++) {
            ClanManager.QuestDef q = quests.get(i);
            long stored = plugin.getClanManager().getQuestProgress(clan, q.id);
            boolean alreadyClaimed = stored < 0;
            long progress;
            if (alreadyClaimed) {
                progress = q.target;
            } else if ("grow".equals(q.id)) {
                progress = Math.min(memberCount, q.target);
                if (stored != progress) {
                    plugin.getClanManager().setQuestProgress(clan, q.id, progress);
                }
            } else {
                progress = stored;
            }
            boolean done = progress >= q.target;
            String rewardText = formatReward(q.rewardType, q.rewardAmount);
            Material mat = alreadyClaimed ? Material.MAP : (done ? Material.EMERALD : Material.PAPER);
            ItemStack questItem = new ItemStack(mat);
            ItemMeta questMeta = questItem.getItemMeta();
            questMeta.setDisplayName(plugin.getConfigManager().getSingleMessage(q.nameKey));
            List<String> lore = new ArrayList<>(plugin.getConfigManager().getLoreList(q.loreKey,
                    "%progress%", String.valueOf(Math.min(progress, q.target)),
                    "%target%", String.valueOf(q.target),
                    "%bar%", buildBar(progress, q.target),
                    "%reward%", rewardText));
            String suffixKey;
            if (alreadyClaimed) suffixKey = "menu-quest-already-claimed-lore";
            else if (done) suffixKey = "menu-quest-claim-lore";
            else suffixKey = "menu-quest-incomplete-lore";
            lore.addAll(plugin.getConfigManager().getLoreList(suffixKey));
            questMeta.setLore(lore);
            if (done && !alreadyClaimed) questMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            tagAction(questMeta, MENU_QUEST_NBT_PREFIX + q.id);
            questItem.setItemMeta(questMeta);
            menu.setItem(questSlots[i], questItem);
        }

        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-close-name"));
        closeMeta.setLore(plugin.getConfigManager().getLoreList("menu-close-lore"));
        tagAction(closeMeta, "close");
        closeItem.setItemMeta(closeMeta);
        menu.setItem(49, closeItem);

        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-info-name"));
        infoMeta.setLore(plugin.getConfigManager().getLoreList("menu-info-lore"));
        tagAction(infoMeta, "info");
        infoItem.setItemMeta(infoMeta);
        menu.setItem(48, infoItem);

        ItemStack topItem = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta topMeta = topItem.getItemMeta();
        topMeta.setDisplayName(plugin.getConfigManager().getSingleMessage("menu-top-name"));
        topMeta.setLore(plugin.getConfigManager().getLoreList("menu-top-lore"));
        tagAction(topMeta, "top");
        topItem.setItemMeta(topMeta);
        menu.setItem(50, topItem);

        player.openInventory(menu);
    }

    private void tagAction(ItemMeta meta, String action) {
        NamespacedKey key = new NamespacedKey(plugin, "menu_action");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
    }

    public static String readActionTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        ClanSystem plugin = ClanSystem.getInstance();
        if (plugin == null) return null;
        NamespacedKey key = new NamespacedKey(plugin, "menu_action");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(key, PersistentDataType.STRING)) {
            return pdc.get(key, PersistentDataType.STRING);
        }
        return null;
    }

    private ItemStack pane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    private String formatNumber(double value) {
        if (value >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format(java.util.Locale.ROOT, "%.1fk", value / 1_000.0);
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String buildBar(long progress, long target) {
        int total = 20;
        if (target <= 0) return "§a" + "█".repeat(total);
        int filled = (int) Math.min(total, Math.max(0, (progress * total) / target));
        StringBuilder sb = new StringBuilder();
        sb.append("§6").append("█".repeat(filled));
        sb.append("§7").append("█".repeat(total - filled));
        return sb.toString();
    }

    private String formatReward(String type, long amount) {
        switch (type) {
            case "bank": return "§6" + formatNumber(amount) + "§e монет в банк";
            case "kills": return "§c+" + amount + "§e к убийствам клана";
            case "deposited": return "§b+" + formatNumber(amount) + "§e к внесённым";
            default: return amount + " " + type;
        }
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
