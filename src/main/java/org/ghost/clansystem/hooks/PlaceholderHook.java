package org.ghost.clansystem.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.ghost.clansystem.ClanSystem;
import java.util.List;
import java.util.UUID;

public class PlaceholderHook extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    private final ClanSystem plugin;
    public PlaceholderHook(ClanSystem plugin) { this.plugin = plugin; }
    @Override public String getIdentifier() { return "clan"; }
    @Override public String getAuthor() { return "ClanSystem"; }
    @Override public String getVersion() { return "1.0"; }
    @Override public boolean persist() { return true; }
    @Override public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("name")) {
            String clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            return clan != null ? clan : "Нету";
        }
        if (params.equalsIgnoreCase("role")) {
            String clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) return "Нету роли нищий :3";
            if (plugin.getClanManager().isLeader(player.getUniqueId(), clan)) {
                return "§c§lЛидер";
            } else if (plugin.getClanManager().isModerator(player.getUniqueId(), clan)) {
                return "§9§lМодератор";
            } else {
                return "§a§lУчастник";
            }
        }
        if (params.equalsIgnoreCase("pvp")) {
            String clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) return "Нету пвп";
            return plugin.getClanManager().getClanPvp(clan) ? "§e§lВключено" : "§c§lВыключено";
        }
        if (params.equalsIgnoreCase("online")) {
            String clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) return "0";
            int onlineCount = 0;
            List<String> members = plugin.getClanManager().getClanMembers(clan);
            for (String memberUuid : members) {
                Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
                if (member != null && member.isOnline()) {
                    onlineCount++;
                }
            }
            return String.valueOf(onlineCount);
        }
        if (params.equalsIgnoreCase("online_max")) {
            return "50";
        }
        return null;
    }
}
