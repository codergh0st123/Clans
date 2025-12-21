package org.ghost.clansystem.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.ghost.clansystem.ClanSystem;
import java.util.Base64;
import java.util.UUID;

public class ClanListener implements Listener {

    private final ClanSystem plugin;

    public ClanListener(ClanSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        String clan = plugin.getClanManager().getPlayerClan(killer.getUniqueId());
        if (clan == null) return;
        String victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        if (victimClan != null && victimClan.equals(clan) && !plugin.getClanManager().getClanPvp(clan)) {
            return;
        }
        int kills = plugin.getClanManager().getClanKills(clan) + 1;
        plugin.getClanManager().setClanKills(clan, kills);
        checkLevelUp(clan);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        String playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        String damagerClan = plugin.getClanManager().getPlayerClan(damager.getUniqueId());
        if (playerClan != null && damagerClan != null && playerClan.equals(damagerClan) && !plugin.getClanManager().getClanPvp(playerClan)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();
        String glowTitle = plugin.getConfigManager().getSingleMessage("glow-title");

        if (title.equals(plugin.getConfigManager().translateHexColorCodes(glowTitle))) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    plugin.getGuiManager().getGlowTargetSelection().put(player.getUniqueId(), meta.getOwningPlayer().getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("glow-selected", "%player%", meta.getOwningPlayer().getName()));
                }
                return;
            }

            UUID targetUuid = plugin.getGuiManager().getGlowTargetSelection().getOrDefault(player.getUniqueId(), player.getUniqueId());

            if (clicked.getType() == Material.LEATHER_CHESTPLATE) {
                LeatherArmorMeta meta = (LeatherArmorMeta) clicked.getItemMeta();
                Color color = meta.getColor();
                plugin.getClanManager().savePlayerGlow(targetUuid, color.asRGB());
                player.sendMessage(plugin.getConfigManager().getSingleMessage("glow-updated", "%player%", Bukkit.getOfflinePlayer(targetUuid).getName()));
                player.closeInventory();
                plugin.getGuiManager().getGlowTargetSelection().remove(player.getUniqueId());
            } else if (clicked.getType() == Material.BARRIER) {
                plugin.getClanManager().removePlayerGlow(targetUuid);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("glow-updated", "%player%", Bukkit.getOfflinePlayer(targetUuid).getName()));
                player.closeInventory();
                plugin.getGuiManager().getGlowTargetSelection().remove(player.getUniqueId());
            }
            return;
        }

        String chestTitleRaw = plugin.getConfigManager().getStringList("chest-title").get(0);
        String chestPrefixRaw = chestTitleRaw.split("%clan%")[0];
        String chestPrefix = plugin.getConfigManager().translateHexColorCodes(chestPrefixRaw);
        if (title.startsWith(chestPrefix)) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(plugin, () -> saveChest(title.substring(chestPrefix.length()), e.getInventory()));
        } else {
            String shopTitleRaw = plugin.getConfigManager().getStringList("shop-title").get(0);
            String shopPrefixRaw = shopTitleRaw.split("%level%")[0];
            String shopPrefix = plugin.getConfigManager().translateHexColorCodes(shopPrefixRaw);
            if (title.startsWith(shopPrefix)) {
                e.setCancelled(true);
                player.updateInventory();
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getLore() == null || clicked.getItemMeta().getLore().size() < 2) return;
                String loreLine = clicked.getItemMeta().getLore().get(1);
                int id;
                try {
                    id = Integer.parseInt(loreLine.split(": ")[1]);
                } catch (Exception ex) {
                    return;
                }
                String clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                if (clan == null) return;
                double bank = plugin.getClanManager().getClanBank(clan);
                int cost = plugin.getGuiManager().getCostById(id);
                if (cost == 0 || bank < cost) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("insufficient-bank"));
                    return;
                }
                plugin.getClanManager().setClanBank(clan, bank - cost);
                ItemStack[] rewards = plugin.getGuiManager().getRewardsById(id);
                for (ItemStack reward : rewards) {
                    if (reward != null && reward.getType() != Material.AIR) {
                        YamlConfiguration temp = new YamlConfiguration();
                        temp.set("item", reward.serialize());
                        String itemData = Base64.getEncoder().encodeToString(temp.saveToString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        plugin.getClanManager().addToClanChest(clan, itemData);
                    }
                }
                player.sendMessage(plugin.getConfigManager().getSingleMessage("purchased", "%item%", clicked.getItemMeta().getDisplayName(), "%cost%", String.valueOf(cost)));
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();
        String shopTitleRaw = plugin.getConfigManager().getStringList("shop-title").get(0);
        String shopPrefixRaw = shopTitleRaw.split("%level%")[0];
        String shopPrefix = plugin.getConfigManager().translateHexColorCodes(shopPrefixRaw);
        if (title.startsWith(shopPrefix)) {
            e.setCancelled(true);
        }
    }

    private void saveChest(String clan, Inventory inv) {
        plugin.getClanManager().clearClanChest(clan);
        for (ItemStack item : inv.getContents()) {
            if (item != null) {
                YamlConfiguration temp = new YamlConfiguration();
                temp.set("item", item.serialize());
                String itemData = Base64.getEncoder().encodeToString(temp.saveToString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                plugin.getClanManager().addToClanChest(clan, itemData);
            }
        }
    }

    private void checkLevelUp(String clan) {
        int level = plugin.getClanManager().getClanLevel(clan);
        if (level >= 10) return;
        int kills = plugin.getClanManager().getClanKills(clan);
        double deposited = plugin.getClanManager().getClanDeposited(clan);
        int next = level + 1;
        if (kills >= plugin.getClanManager().KILLS_REQUIRED[next - 1] && deposited >= plugin.getClanManager().COINS_REQUIRED[next - 1]) {
            plugin.getClanManager().updateClanLevel(clan, next);
            Bukkit.broadcastMessage(plugin.getConfigManager().getSingleMessage("max-level-upgrade", "%clan%", clan, "%level%", String.valueOf(next)));
            checkLevelUp(clan);
        }
    }
}
