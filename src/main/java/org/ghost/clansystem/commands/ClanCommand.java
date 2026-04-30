package org.ghost.clansystem.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.ghost.clansystem.ClanSystem;
import org.ghost.clansystem.database.DatabaseManager;
import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final ClanSystem plugin;

    public ClanCommand(ClanSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().getSingleMessage("only-players"));
                return true;
            }
            String clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) {
                plugin.getConfigManager().sendList(player, "no-clan-help");
            } else {
                plugin.getConfigManager().sendList(player, "yes-clan-help-usage", "%role%", getRoleDisplay(player.getUniqueId(), clan));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("clan.admin")) {
            plugin.getConfigManager().reload();
            plugin.getClanManager().loadClansFromDatabase();
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("reload"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getSingleMessage("only-players"));
            return true;
        }
        UUID uuid = player.getUniqueId();
        String clan = plugin.getClanManager().getPlayerClan(uuid);
        switch (args[0].toLowerCase()) {
            case "yuko":
                Bukkit.broadcastMessage("YuKo_san lox");
                break;
            case "menu":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                plugin.getGuiManager().openMainMenu(player, clan);
                break;
            case "create":
                if (clan != null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("already-in-clan")); return true; }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("create-usage")); return true; }
                String rawName = args[1];
                String cleanName = stripColorCodes(rawName);
                if (cleanName.length() > 16) { player.sendMessage(plugin.getConfigManager().getSingleMessage("create-name-too-long")); return true; }
                String name = rawName;
                if (plugin.getClanManager().clanExists(name)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("clan-exists")); return true; }
                if (plugin.getEconomy().getBalance(player) < 500000) { player.sendMessage(plugin.getConfigManager().getSingleMessage("create-cost")); return true; }
                plugin.getEconomy().withdrawPlayer(player, 500000);
                plugin.getClanManager().createClan(name, uuid);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("clan-created", "%clan%", name));
                Bukkit.broadcastMessage(plugin.getConfigManager().getSingleMessage("clan-created-broadcast", "%player%", player.getName(), "%clan%", name));
                break;
            case "delete":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("not-leader"));
                    return true;
                }
                if (args.length != 1) { player.sendMessage(plugin.getConfigManager().getSingleMessage("delete-usage")); return true; }
                plugin.getClanManager().deleteClan(clan);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("clan-deleted", "%clan%", clan));
                break;
            case "leave":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (plugin.getClanManager().isLeader(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("leader-cant-leave"));
                    return true;
                }
                if (args.length != 1) { player.sendMessage(plugin.getConfigManager().getSingleMessage("leave-usage")); return true; }
                plugin.getClanManager().removePlayerFromClan(uuid, clan);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("clan-left", "%clan%", clan));
                break;
            case "kick":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan) && !plugin.getClanManager().isModerator(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("no-permission"));
                    return true;
                }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("kick-usage")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-offline")); return true; }
                String targetClan = plugin.getClanManager().getPlayerClan(target.getUniqueId());
                if (!targetClan.equals(clan)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-not-in-clan")); return true; }
                if (target.getUniqueId().equals(UUID.fromString(plugin.getClanManager().getClanOwner(clan)))) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("kick-owner"));
                    return true;
                }
                plugin.getClanManager().removePlayerFromClan(target.getUniqueId(), clan);
                target.sendMessage(plugin.getConfigManager().getSingleMessage("player-left", "%player%", target.getName(), "%clan%", clan));
                player.sendMessage(plugin.getConfigManager().getSingleMessage("player-kicked", "%player%", target.getName(), "%clan%", clan));
                break;
            case "addmoder":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("not-leader"));
                    return true;
                }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("addmoder-usage")); return true; }
                Player targetAdd = Bukkit.getPlayer(args[1]);
                if (targetAdd == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-offline")); return true; }
                String targetAddClan = plugin.getClanManager().getPlayerClan(targetAdd.getUniqueId());
                if (!targetAddClan.equals(clan)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-not-member")); return true; }
                if (targetAdd.getUniqueId().equals(UUID.fromString(plugin.getClanManager().getClanOwner(clan)))) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("addmoder-owner"));
                    return true;
                }
                if (plugin.getClanManager().isModerator(targetAdd.getUniqueId(), clan)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-already-moder")); return true; }
                plugin.getClanManager().setPlayerRole(targetAdd.getUniqueId(), clan, "moderator");
                player.sendMessage(plugin.getConfigManager().getSingleMessage("moder-added", "%player%", targetAdd.getName()));
                targetAdd.sendMessage(plugin.getConfigManager().getSingleMessage("moder-added", "%player%", targetAdd.getName()));
                break;
            case "removemoder":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("not-leader"));
                    return true;
                }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("removemoder-usage")); return true; }
                Player targetRemove = Bukkit.getPlayer(args[1]);
                if (targetRemove == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-offline")); return true; }
                String targetRemoveClan = plugin.getClanManager().getPlayerClan(targetRemove.getUniqueId());
                if (!targetRemoveClan.equals(clan)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-not-member")); return true; }
                if (!plugin.getClanManager().isModerator(targetRemove.getUniqueId(), clan)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-not-moder")); return true; }
                plugin.getClanManager().setPlayerRole(targetRemove.getUniqueId(), clan, "member");
                player.sendMessage(plugin.getConfigManager().getSingleMessage("moder-removed", "%player%", targetRemove.getName()));
                targetRemove.sendMessage(plugin.getConfigManager().getSingleMessage("moder-removed", "%player%", targetRemove.getName()));
                break;
            case "msg":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (args.length < 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("msg-usage")); return true; }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                List<String> msgMembers = plugin.getClanManager().getClanMembers(clan);
                for (String memberStr : msgMembers) {
                    Player member = Bukkit.getPlayer(UUID.fromString(memberStr));
                    if (member != null) {
                        String role = getRoleDisplay(member.getUniqueId(), clan);
                        member.sendMessage(plugin.getConfigManager().getSingleMessage("clan-message", "%player%", player.getName(), "%role%", role, "%message%", message));
                        if (plugin.getClanManager().getClanMsgLoggers().contains(member.getUniqueId())) {
                            member.sendMessage(ChatColor.GRAY + "[LOG] " + plugin.getConfigManager().getSingleMessage("clan-message", "%player%", player.getName(), "%role%", role, "%message%", message));
                        }
                    }
                }
                for (Player admin : Bukkit.getOnlinePlayers()) {
                    if (admin.hasPermission("clan.admin") && plugin.getClanManager().getClanMsgLoggers().contains(admin.getUniqueId()) && !msgMembers.contains(admin.getUniqueId().toString())) {
                        String role = getRoleDisplay(player.getUniqueId(), clan);
                        String formattedMessage = plugin.getConfigManager().getSingleMessage("clan-message", "%player%", player.getName(), "%role%", role, "%message%", message);
                        admin.sendMessage(ChatColor.GRAY + "[LOG] " + formattedMessage);
                    }
                }
                break;
            case "sethome":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan) && !plugin.getClanManager().isModerator(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("no-permission"));
                    return true;
                }
                plugin.getClanManager().setClanHome(clan, player.getLocation());
                player.sendMessage(plugin.getConfigManager().getSingleMessage("sethome-success"));
                break;
            case "delhome":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan) && !plugin.getClanManager().isModerator(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("no-permission"));
                    return true;
                }
                plugin.getClanManager().deleteClanHome(clan);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("delhome-success"));
                break;
            case "home":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                Location homeLoc = plugin.getClanManager().getClanHome(clan);
                if (homeLoc == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("no-home")); return true; }
                player.teleport(homeLoc);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("home-teleport"));
                break;
            case "leader":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("not-leader"));
                    return true;
                }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("leader-usage")); return true; }
                Player newLeader = Bukkit.getPlayer(args[1]);
                if (newLeader == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-offline")); return true; }
                String newLeaderClan = plugin.getClanManager().getPlayerClan(newLeader.getUniqueId());
                if (!newLeaderClan.equals(clan)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-not-member")); return true; }
                plugin.getClanManager().setClanOwner(clan, newLeader.getUniqueId());
                player.sendMessage(plugin.getConfigManager().getSingleMessage("leader-transferred", "%player%", newLeader.getName()));
                newLeader.sendMessage(plugin.getConfigManager().getSingleMessage("leader-transferred", "%player%", newLeader.getName()));
                break;
            case "pvp":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan) && !plugin.getClanManager().isModerator(uuid, clan)) {
                    return true;
                }
                boolean pvp = plugin.getClanManager().getClanPvp(clan);
                plugin.getClanManager().setClanPvp(clan, !pvp);
                List<String> pvpMembers = plugin.getClanManager().getClanMembers(clan);
                for (String memberStr : pvpMembers) {
                    Player member = Bukkit.getPlayer(UUID.fromString(memberStr));
                    if (member != null) {
                        if (!pvp) {
                            member.sendMessage(plugin.getConfigManager().getSingleMessage("pvp-enabled"));
                        } else {
                            member.sendMessage(plugin.getConfigManager().getSingleMessage("pvp-disabled"));
                        }
                    }
                }
                break;
            case "glow":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                plugin.getGuiManager().getGlowTargetSelection().remove(player.getUniqueId());
                plugin.getGuiManager().openGlowGui(player, clan);
                break;
            case "help":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                plugin.getConfigManager().sendList(player, "in-clan-help", "%role%", getRoleDisplay(player.getUniqueId(), clan));
                break;
            case "invite":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan) && !plugin.getClanManager().isModerator(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("no-permission"));
                    return true;
                }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("invite-usage")); return true; }
                Player targetInvite = Bukkit.getPlayer(args[1]);
                if (targetInvite == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-offline")); return true; }
                if (plugin.getClanManager().getPlayerClan(targetInvite.getUniqueId()) != null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("player-in-clan")); return true; }
                plugin.getClanManager().getPendingInvites().put(targetInvite.getUniqueId(), clan);
                String ownerName = player.getName();
                net.md_5.bungee.api.chat.TextComponent main = new net.md_5.bungee.api.chat.TextComponent(
                        plugin.getConfigManager().getSingleMessage("invite-received", "%clan%", clan, "%owner%", ownerName) + "\n"
                );
                net.md_5.bungee.api.chat.TextComponent accept = new net.md_5.bungee.api.chat.TextComponent("  §a§l✓ ПРИНЯТЬ ПРИГЛАШЕНИЕ\n\n");
                accept.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                accept.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/clan accept"));
                accept.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent("Принять приглашение")}));
                net.md_5.bungee.api.chat.TextComponent deny = new net.md_5.bungee.api.chat.TextComponent("  §c§l✘ ОТКАЗАТЬСЯ\n");
                deny.setColor(net.md_5.bungee.api.ChatColor.RED);
                deny.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/clan deny"));
                deny.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent("Отклонить приглашение")}));
                net.md_5.bungee.api.chat.TextComponent spacer = new net.md_5.bungee.api.chat.TextComponent("   ");
                targetInvite.spigot().sendMessage(main);
                targetInvite.spigot().sendMessage(accept, spacer, deny);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("invite-sent", "%player%", targetInvite.getName()));
                break;
            case "deposit":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("deposit-usage")); return true; }
                double amount;
                try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) { player.sendMessage(plugin.getConfigManager().getSingleMessage("invalid-amount")); return true; }
                if (amount <= 0) { player.sendMessage(plugin.getConfigManager().getSingleMessage("amount-positive")); return true; }
                if (plugin.getEconomy().getBalance(player) < amount) { player.sendMessage(plugin.getConfigManager().getSingleMessage("insufficient-funds")); return true; }
                plugin.getEconomy().withdrawPlayer(player, amount);
                double bank = plugin.getClanManager().getClanBank(clan);
                double deposited = plugin.getClanManager().getClanDeposited(clan);
                plugin.getClanManager().setClanBank(clan, bank + amount);
                plugin.getClanManager().setClanDeposited(clan, deposited + amount);
                plugin.getClanManager().addQuestProgress(clan, "deposit", (long) amount);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("deposited", "%amount%", String.valueOf(amount)));
                checkLevelUp(clan);
                break;
            case "withdraw":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                if (!plugin.getClanManager().isLeader(uuid, clan) && !plugin.getClanManager().isModerator(uuid, clan)) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("no-permission"));
                    return true;
                }
                if (args.length != 2) { player.sendMessage(plugin.getConfigManager().getSingleMessage("withdraw-usage")); return true; }
                double withdrawAmount;
                try { withdrawAmount = Double.parseDouble(args[1]); } catch (NumberFormatException e) { player.sendMessage(plugin.getConfigManager().getSingleMessage("invalid-amount")); return true; }
                if (withdrawAmount <= 0) { player.sendMessage(plugin.getConfigManager().getSingleMessage("amount-positive")); return true; }
                double clanBank = plugin.getClanManager().getClanBank(clan);
                if (clanBank < withdrawAmount) { player.sendMessage(plugin.getConfigManager().getSingleMessage("insufficient-bank")); return true; }
                plugin.getClanManager().setClanBank(clan, clanBank - withdrawAmount);
                plugin.getEconomy().depositPlayer(player, withdrawAmount);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("withdrawn", "%amount%", String.valueOf(withdrawAmount)));
                break;
            case "money":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                double balance = plugin.getClanManager().getClanBank(clan);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("money-info", "%balance%", String.valueOf(balance)));
                break;
            case "shop":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                int level = plugin.getClanManager().getClanLevel(clan);
                if (level < 3) { player.sendMessage(plugin.getConfigManager().getSingleMessage("shop-level")); return true; }
                plugin.getGuiManager().openShop(player, clan, level);
                break;
            case "upgrade":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                showUpgradeInfo(player, clan);
                break;
            case "info":
                if (args.length == 1) {
                    if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                    showClanInfo(player, clan);
                } else {
                    String targetClan1 = args[1];
                    if (!plugin.getClanManager().clanExists(targetClan1)) { player.sendMessage(plugin.getConfigManager().getSingleMessage("clan-not-found")); return true; }
                    showClanInfo(player, targetClan1);
                }
                break;
            case "chest":
                if (clan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("not-in-clan")); return true; }
                int slots = plugin.getClanManager().CHEST_SLOTS[plugin.getClanManager().getClanLevel(clan) - 1];
                Inventory chestInv = Bukkit.createInventory(null, slots, plugin.getConfigManager().getSingleMessage("chest-title", "%clan%", clan));
                List<String> items = plugin.getClanManager().getClanChestItems(clan);
                for (String s : items) {
                    byte[] data = Base64.getDecoder().decode(s);
                    YamlConfiguration temp = new YamlConfiguration();
                    try { temp.loadFromString(new String(data, java.nio.charset.StandardCharsets.UTF_8)); } catch (Exception ignored) {}
                    Map<String, Object> map = temp.getConfigurationSection("item").getValues(true);
                    chestInv.addItem(ItemStack.deserialize(map));
                }
                player.openInventory(chestInv);
                break;
            case "top":
                showTopClans(player);
                break;
            case "accept":
                if (clan != null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("already-in-clan")); return true; }
                String inviteClan = plugin.getClanManager().getPendingInvites().get(uuid);
                if (inviteClan == null) { player.sendMessage(plugin.getConfigManager().getSingleMessage("no-invite")); return true; }
                plugin.getClanManager().addPlayerToClan(uuid, inviteClan, "member");
                plugin.getClanManager().getPendingInvites().remove(uuid);
                player.sendMessage(plugin.getConfigManager().getSingleMessage("joined-clan", "%clan%", inviteClan));
                break;
            case "deny":
                if (plugin.getClanManager().getPendingInvites().remove(uuid) != null) {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("invite-denied"));
                } else {
                    player.sendMessage(plugin.getConfigManager().getSingleMessage("no-invite"));
                }
                break;
            default:
                player.sendMessage(plugin.getConfigManager().getSingleMessage("unknown-command"));
        }
        return true;
    }

    private String getRoleDisplay(UUID playerUuid, String clan) {
        if (plugin.getClanManager().isLeader(playerUuid, clan)) {
            return "§c§lЛидер";
        } else if (plugin.getClanManager().isModerator(playerUuid, clan)) {
            return "§9§lМодератор";
        } else {
            return "§a§lУчастник";
        }
    }

    private String stripColorCodes(String input) {
        if (input == null) return null;
        return input.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "")
                .replaceAll("&#([A-Fa-f0-9]{6})", "")
                .replaceAll("&x", "")
                .replaceAll("&", "");
    }

    private void showTopClans(Player player) {
        List<DatabaseManager.ClanData> clans = plugin.getClanManager().getAllClans();
        clans.sort((a, b) -> Double.compare(b.bank(), a.bank()));
        plugin.getConfigManager().sendList(player, "top-clans-title");
        for (int i = 0; i < Math.min(10, clans.size()); i++) {
            DatabaseManager.ClanData data = clans.get(i);
            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(data.owner())).getName();
            plugin.getConfigManager().sendList(player, "top-clan-format",
                    "%rank%", String.valueOf(i + 1),
                    "%name%", data.name(),
                    "%owner%", ownerName,
                    "%members%", String.valueOf(plugin.getClanManager().getClanMembers(data.name()).size()),
                    "%balance%", String.valueOf(data.bank()),
                    "%level%", String.valueOf(data.level())
            );
        }
    }

    private void showClanInfo(Player player, String clanName) {
        String owner = plugin.getClanManager().getClanOwner(clanName);
        int level = plugin.getClanManager().getClanLevel(clanName);
        double bank = plugin.getClanManager().getClanBank(clanName);
        int kills = plugin.getClanManager().getClanKills(clanName);
        double deposited = plugin.getClanManager().getClanDeposited(clanName);
        List<String> members = plugin.getClanManager().getClanMembers(clanName);
        List<String> moderators = plugin.getClanManager().getClanModerators(clanName);
        List<String> formattedMembers = new ArrayList<>();
        for (String m : members) {
            formattedMembers.add(Bukkit.getOfflinePlayer(UUID.fromString(m)).getName());
        }
        plugin.getConfigManager().sendList(player, "clan-info",
                "%name%", clanName,
                "%owner%", Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName(),
                "%level%", String.valueOf(level),
                "%bank%", String.valueOf(bank),
                "%kills%", String.valueOf(kills),
                "%deposited%", String.valueOf(deposited),
                "%size%", String.valueOf(members.size()),
                "%members%", String.join(", ", formattedMembers),
                "%moderators%", String.valueOf(moderators.size()));
    }

    private void showUpgradeInfo(Player player, String clan) {
        int level = plugin.getClanManager().getClanLevel(clan);
        int kills = plugin.getClanManager().getClanKills(clan);
        double deposited = plugin.getClanManager().getClanDeposited(clan);
        if (level >= 10) {
            player.sendMessage(plugin.getConfigManager().getSingleMessage("max-level"));
            return;
        }
        int next = level + 1;
        int needKills = plugin.getClanManager().KILLS_REQUIRED[next - 1];
        int needCoins = plugin.getClanManager().COINS_REQUIRED[next - 1];
        plugin.getConfigManager().sendList(player, "upgrade-progress", "%next%", String.valueOf(next), "%kills%", String.valueOf(kills), "%need_kills%", String.valueOf(needKills), "%deposited%", String.valueOf(deposited), "%need_coins%", String.valueOf(needCoins));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("clan.admin")) {
                completions.add("reload");
            }
            String playerClan = null;
            if (sender instanceof Player player) {
                playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            }
            if (playerClan == null) {
                completions.addAll(Arrays.asList("create", "accept", "deny", "yuko"));
            } else {
                completions.addAll(Arrays.asList("menu", "deposit", "withdraw", "money", "shop", "upgrade", "info", "chest", "invite", "delete", "kick", "leave", "addmoder", "removemoder", "msg", "sethome", "delhome", "home", "leader", "pvp", "glow", "top", "yuko", "help"));
            }
            return filterCompletions(args[0], completions);
        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("invite") || subCommand.equals("kick") || subCommand.equals("addmoder") || subCommand.equals("removemoder") || subCommand.equals("leader")) {
                if (sender instanceof Player player) {
                    String playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                    if (playerClan != null) {
                        List<String> members = plugin.getClanManager().getClanMembers(playerClan);
                        for (String memberUuid : members) {
                            Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
                            if (member != null) {
                                completions.add(member.getName());
                            }
                        }
                    }
                }
            } else if (subCommand.equals("info")) {
                List<DatabaseManager.ClanData> allClans = plugin.getClanManager().getAllClans();
                for (DatabaseManager.ClanData data : allClans) {
                    completions.add(data.name());
                }
            }
            return filterCompletions(args[1], completions);
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
