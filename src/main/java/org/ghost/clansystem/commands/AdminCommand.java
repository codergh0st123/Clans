package org.ghost.clansystem.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.ghost.clansystem.ClanSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final ClanSystem plugin;

    public AdminCommand(ClanSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("clan.admin")) {
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-no-permission"));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("log")) {
            if (args[1].equalsIgnoreCase("on")) {
                if (sender instanceof Player player) {
                    plugin.getClanManager().getClanMsgLoggers().add(player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("admin-log-on"));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-log-on"));
                }
                return true;
            } else if (args[1].equalsIgnoreCase("off")) {
                if (sender instanceof Player player) {
                    plugin.getClanManager().getClanMsgLoggers().remove(player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("admin-log-off"));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-log-off"));
                }
                return true;
            }
        }
        if (args.length != 3 || !args[0].equalsIgnoreCase("addlevel")) {
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-addlevel-usage"));
            return true;
        }
        String clanName = args[1];
        int newLevel;
        try {
            newLevel = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-invalid-level"));
            return true;
        }
        if (newLevel < 1 || newLevel > 10) {
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-invalid-level"));
            return true;
        }
        if (!plugin.getClanManager().clanExists(clanName)) {
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-clan-not-found"));
            return true;
        }
        plugin.getClanManager().updateClanLevel(clanName, newLevel);
        sender.sendMessage(plugin.getConfigManager().getSingleMessage("admin-level-set", "%clan%", clanName, "%level%", String.valueOf(newLevel)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(args[0], Arrays.asList("addlevel", "log"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("log")) {
            return filterCompletions(args[1], Arrays.asList("on", "off"));
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletions(String arg, List<String> completions) {
        List<String> result = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(arg.toLowerCase())) {
                result.add(completion);
            }
        }
        return result;
    }
}
