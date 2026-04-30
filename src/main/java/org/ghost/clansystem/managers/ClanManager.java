package org.ghost.clansystem.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.ghost.clansystem.ClanSystem;
import org.ghost.clansystem.database.DatabaseManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ClanManager {

    private final ClanSystem plugin;
    private final Map<UUID, String> pendingInvites = new HashMap<>();
    private final Map<UUID, String> playerClanCache = new HashMap<>();
    private final Map<UUID, Integer> playerGlowCache = new HashMap<>();
    private final Set<UUID> clanMsgLoggers = new HashSet<>();
    public final int[] KILLS_REQUIRED = {0, 10, 25, 50, 75, 100, 150, 225, 350, 500};
    public final int[] COINS_REQUIRED = {0, 5000, 15000, 30000, 50000, 100000, 200000, 350000, 500000, 1000000};
    public final int[] CHEST_SLOTS = {9, 9, 18, 18, 27, 27, 36, 36, 45, 54};

    public static final class QuestDef {
        public final String id;
        public final String nameKey;
        public final String loreKey;
        public final long target;
        public final String rewardType;
        public final long rewardAmount;
        public QuestDef(String id, String nameKey, String loreKey, long target, String rewardType, long rewardAmount) {
            this.id = id;
            this.nameKey = nameKey;
            this.loreKey = loreKey;
            this.target = target;
            this.rewardType = rewardType;
            this.rewardAmount = rewardAmount;
        }
    }

    public final List<QuestDef> QUESTS = Arrays.asList(
            new QuestDef("kills",   "menu-quest-kills-name",   "menu-quest-kills-lore",   50,     "bank",    50000),
            new QuestDef("deposit", "menu-quest-deposit-name", "menu-quest-deposit-lore", 250000, "bank",    50000),
            new QuestDef("pvp",     "menu-quest-pvp-name",     "menu-quest-pvp-lore",     25,     "kills",   10),
            new QuestDef("grow",    "menu-quest-grow-name",    "menu-quest-grow-lore",    5,      "bank",    25000)
    );

    public ClanManager(ClanSystem plugin) {
        this.plugin = plugin;
        loadClansFromDatabase();
    }

    public void loadClansFromDatabase() {
        try {
            playerClanCache.clear();
            playerGlowCache.clear();
            String sql = "SELECT clan_name, player_uuid, glow_color FROM clan_members";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("clan_name");
                String uuidStr = rs.getString("player_uuid");
                int glow = rs.getInt("glow_color");
                UUID uuid = UUID.fromString(uuidStr);
                playerClanCache.put(uuid, name);
                if (!rs.wasNull()) {
                    playerGlowCache.put(uuid, glow);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerClan(UUID uuid) { return playerClanCache.get(uuid); }

    public boolean clanExists(String name) {
        try {
            String sql = "SELECT 1 FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, name);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void createClan(String name, UUID ownerUuid) {
        try {
            String insertClanSql = "INSERT INTO clans (name, owner, bank, level, kills, deposited, pvp, home) VALUES (?, ?, 0.0, 1, 0, 0.0, 0, NULL)";
            try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(insertClanSql)) {
                stmt.setString(1, name);
                stmt.setString(2, ownerUuid.toString());
                stmt.executeUpdate();
            }
            String deleteOldMemberSql = "DELETE FROM clan_members WHERE clan_name = ? AND player_uuid = ?";
            try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(deleteOldMemberSql)) {
                stmt.setString(1, name);
                stmt.setString(2, ownerUuid.toString());
                stmt.executeUpdate();
            }
            String insertOwnerSql = "INSERT INTO clan_members (clan_name, player_uuid, role, glow_color) VALUES (?, ?, 'owner', NULL)";
            try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(insertOwnerSql)) {
                stmt.setString(1, name);
                stmt.setString(2, ownerUuid.toString());
                stmt.executeUpdate();
            }
            playerClanCache.put(ownerUuid, name);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteClan(String name) {
        try {
            List<String> members = getClanMembers(name);
            String sql = "DELETE FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (PreparedStatement clean = plugin.getDatabaseManager().getConnection()
                    .prepareStatement("DELETE FROM clan_quests WHERE clan_name = ?")) {
                clean.setString(1, name);
                clean.executeUpdate();
            }
            for (String memberUuid : members) {
                UUID uuid = UUID.fromString(memberUuid);
                playerClanCache.remove(uuid);
                playerGlowCache.remove(uuid);
                updateAppearance(Bukkit.getPlayer(uuid));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPlayerToClan(UUID playerUuid, String clanName, String role) {
        try {
            String sql = "INSERT INTO clan_members (clan_name, player_uuid, role, glow_color) VALUES (?, ?, ?, NULL)";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            stmt.setString(2, playerUuid.toString());
            stmt.setString(3, role);
            stmt.executeUpdate();
            playerClanCache.put(playerUuid, clanName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlayerFromClan(UUID playerUuid, String clanName) {
        try {
            String sql = "DELETE FROM clan_members WHERE clan_name = ? AND player_uuid = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            stmt.setString(2, playerUuid.toString());
            stmt.executeUpdate();
            playerClanCache.remove(playerUuid);
            playerGlowCache.remove(playerUuid);
            updateAppearance(Bukkit.getPlayer(playerUuid));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPlayerRole(UUID playerUuid, String clanName, String role) {
        try {
            String sql = "UPDATE clan_members SET role = ? WHERE clan_name = ? AND player_uuid = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, role);
            stmt.setString(2, clanName);
            stmt.setString(3, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isLeader(UUID uuid, String clan) {
        String owner = getClanOwner(clan);
        return owner != null && owner.equals(uuid.toString());
    }

    public boolean isModerator(UUID uuid, String clan) {
        try {
            String sql = "SELECT role FROM clan_members WHERE clan_name = ? AND player_uuid = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clan);
            stmt.setString(2, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return "moderator".equals(rs.getString("role"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getClanMembers(String clanName) {
        List<String> members = new ArrayList<>();
        try {
            String sql = "SELECT player_uuid FROM clan_members WHERE clan_name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) members.add(rs.getString("player_uuid"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public void setClanHome(String clanName, Location location) {
        String locStr = location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
        try {
            String sql = "UPDATE clans SET home = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, locStr);
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteClanHome(String clanName) {
        try {
            String sql = "UPDATE clans SET home = NULL WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getClanHome(String clanName) {
        try {
            String sql = "SELECT home FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String locStr = rs.getString("home");
                if (locStr != null) {
                    String[] parts = locStr.split(",");
                    if (parts.length == 6) {
                        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setClanOwner(String clanName, UUID newOwnerUuid) {
        try {
            String sql = "UPDATE clans SET owner = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, newOwnerUuid.toString());
            stmt.setString(2, clanName);
            stmt.executeUpdate();
            String oldOwnerUuid = getClanOwner(clanName);
            if (oldOwnerUuid != null) setPlayerRole(UUID.fromString(oldOwnerUuid), clanName, "member");
            setPlayerRole(newOwnerUuid, clanName, "owner");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getClanOwner(String clanName) {
        try {
            String sql = "SELECT owner FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("owner");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setClanPvp(String clanName, boolean pvp) {
        try {
            String sql = "UPDATE clans SET pvp = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setInt(1, pvp ? 1 : 0);
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean getClanPvp(String clanName) {
        try {
            String sql = "SELECT pvp FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("pvp") == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateClanLevel(String clanName, int newLevel) {
        try {
            String sql = "UPDATE clans SET level = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setInt(1, newLevel);
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getClanLevel(String clanName) {
        try {
            String sql = "SELECT level FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("level");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public double getClanBank(String clanName) {
        try {
            String sql = "SELECT bank FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble("bank");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void setClanBank(String clanName, double newBank) {
        try {
            String sql = "UPDATE clans SET bank = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setDouble(1, newBank);
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getClanDeposited(String clanName) {
        try {
            String sql = "SELECT deposited FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble("deposited");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void setClanDeposited(String clanName, double newDeposited) {
        try {
            String sql = "UPDATE clans SET deposited = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setDouble(1, newDeposited);
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getClanChestItems(String clanName) {
        List<String> items = new ArrayList<>();
        try {
            String sql = "SELECT item_data FROM clan_chests WHERE clan_name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) items.add(rs.getString("item_data"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public int getClanKills(String clanName) {
        try {
            String sql = "SELECT kills FROM clans WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("kills");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setClanKills(String clanName, int newKills) {
        try {
            String sql = "UPDATE clans SET kills = ? WHERE name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setInt(1, newKills);
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getClanModerators(String clanName) {
        List<String> moderators = new ArrayList<>();
        try {
            String sql = "SELECT player_uuid FROM clan_members WHERE clan_name = ? AND role = 'moderator'";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) moderators.add(rs.getString("player_uuid"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return moderators;
    }

    public void savePlayerGlow(UUID uuid, int color) {
        try {
            String sql = "UPDATE clan_members SET glow_color = ? WHERE player_uuid = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setInt(1, color);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
            playerGlowCache.put(uuid, color);
            updateAppearance(Bukkit.getPlayer(uuid));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlayerGlow(UUID uuid) {
        try {
            String sql = "UPDATE clan_members SET glow_color = NULL WHERE player_uuid = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            playerGlowCache.remove(uuid);
            updateAppearance(Bukkit.getPlayer(uuid));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<DatabaseManager.ClanData> getAllClans() {
        List<DatabaseManager.ClanData> clans = new ArrayList<>();
        try {
            String sql = "SELECT name, owner, bank, level, kills, deposited FROM clans";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String owner = rs.getString("owner");
                double bank = rs.getDouble("bank");
                int level = rs.getInt("level");
                int kills = rs.getInt("kills");
                double deposited = rs.getDouble("deposited");
                clans.add(new DatabaseManager.ClanData(name, owner, bank, level, kills, deposited, false, null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clans;
    }

    public void addToClanChest(String clan, String itemData) {
        try {
            String sql = "INSERT INTO clan_chests (clan_name, item_data) VALUES (?, ?)";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clan);
            stmt.setString(2, itemData);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearClanChest(String clan) {
        try {
            String sql = "DELETE FROM clan_chests WHERE clan_name = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clan);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, String> getPendingInvites() { return pendingInvites; }
    public Set<UUID> getClanMsgLoggers() { return clanMsgLoggers; }
    public Integer getPlayerGlow(UUID uuid) { return playerGlowCache.get(uuid); }

    public QuestDef getQuestById(String id) {
        for (QuestDef q : QUESTS) {
            if (q.id.equals(id)) return q;
        }
        return null;
    }

    public long getQuestProgress(String clanName, String questId) {
        try {
            String sql = "SELECT progress FROM clan_quests WHERE clan_name = ? AND quest_id = ?";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            stmt.setString(2, questId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("progress");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public void setQuestProgress(String clanName, String questId, long value) {
        try {
            String sql = "INSERT INTO clan_quests (clan_name, quest_id, progress) VALUES (?, ?, ?) " +
                    "ON CONFLICT(clan_name, quest_id) DO UPDATE SET progress = excluded.progress";
            PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql);
            stmt.setString(1, clanName);
            stmt.setString(2, questId);
            stmt.setLong(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addQuestProgress(String clanName, String questId, long delta) {
        if (clanName == null || delta == 0) return;
        QuestDef def = getQuestById(questId);
        long cap = def != null ? def.target : Long.MAX_VALUE;
        long current = getQuestProgress(clanName, questId);
        if (current < 0) return;
        long next = Math.min(cap, Math.max(0L, current + delta));
        if (next != current) {
            setQuestProgress(clanName, questId, next);
        }
    }

    public void resetQuestProgress(String clanName, String questId) {
        setQuestProgress(clanName, questId, 0L);
    }

    private void updateAppearance(Player target) {
        if (target != null && target.isOnline()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(target)) {
                    online.hidePlayer(plugin, target);
                    online.showPlayer(plugin, target);
                }
            }
        }
    }
}
