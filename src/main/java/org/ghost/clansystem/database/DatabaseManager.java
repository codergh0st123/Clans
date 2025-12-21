package org.ghost.clansystem.database;

import org.bukkit.Location;
import org.ghost.clansystem.ClanSystem;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final ClanSystem plugin;
    private Connection connection;

    public DatabaseManager(ClanSystem plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/clans.db");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try {
            String clansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                    "name TEXT PRIMARY KEY, " +
                    "owner TEXT, " +
                    "bank REAL, " +
                    "level INTEGER, " +
                    "kills INTEGER, " +
                    "deposited REAL, " +
                    "pvp INTEGER, " +
                    "home TEXT" +
                    ");";
            String membersTable = "CREATE TABLE IF NOT EXISTS clan_members (" +
                    "clan_name TEXT, " +
                    "player_uuid TEXT, " +
                    "role TEXT, " +
                    "glow_color INTEGER, " +
                    "FOREIGN KEY(clan_name) REFERENCES clans(name) ON DELETE CASCADE" +
                    ");";
            String chestsTable = "CREATE TABLE IF NOT EXISTS clan_chests (" +
                    "clan_name TEXT, " +
                    "item_data TEXT, " +
                    "FOREIGN KEY(clan_name) REFERENCES clans(name) ON DELETE CASCADE" +
                    ");";
            connection.createStatement().execute(clansTable);
            connection.createStatement().execute(membersTable);
            connection.createStatement().execute(chestsTable);
            try {
                connection.createStatement().execute("ALTER TABLE clan_members ADD COLUMN glow_color INTEGER;");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public record ClanData(String name, String owner, double bank, int level, int kills, double deposited, boolean pvp, String home) {}
}
