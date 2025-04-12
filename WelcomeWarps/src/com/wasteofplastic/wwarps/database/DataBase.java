package com.wasteofplastic.wwarps.database;

import com.wasteofplastic.wwarps.WWarps;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class DataBase {
    private final WWarps plugin;
    private Connection connection;
    private final File dbFile;

    public DataBase(WWarps plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "database/warps.db");
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Ensure the database directory exists
            File dbDir = new File(plugin.getDataFolder(), "database");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Connect to the database
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true); // Manage transactions manually if needed

            // Create the warps table
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS warps (" +
                                "uuid TEXT NOT NULL," +
                                "player_name TEXT NOT NULL," +
                                "world TEXT NOT NULL," +
                                "x INTEGER NOT NULL," +
                                "y INTEGER NOT NULL," +
                                "z INTEGER NOT NULL," +
                                "created_at INTEGER NOT NULL," +
                                "last_used INTEGER NOT NULL," +
                                "PRIMARY KEY (uuid))"
                );
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite database!");
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
        }
    }

    public void addWarp(UUID playerUUID, String playerName, Location location, long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        String sql = "INSERT OR REPLACE INTO warps (uuid, player_name, world, x, y, z, created_at, last_used) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, location.getWorld().getName());
            pstmt.setInt(4, location.getBlockX());
            pstmt.setInt(5, location.getBlockY());
            pstmt.setInt(6, location.getBlockZ());
            long now = System.currentTimeMillis();
            pstmt.setLong(7, now);
            pstmt.setLong(8, now);
            pstmt.executeUpdate();
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while adding warp for " + playerName);
        }
    }

    public void removeWarp(UUID playerUUID, long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        String sql = "DELETE FROM warps WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while removing warp for UUID " + playerUUID);
        }
    }

    public void removeWarp(Location location, long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        String sql = "DELETE FROM warps WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            pstmt.executeUpdate();
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while removing warp at " + location);
        }
    }

    public Map<UUID, Location> loadWarps(long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        Map<UUID, Location> warpList = new HashMap<>();
        String sql = "SELECT uuid, world, x, y, z FROM warps";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
                warpList.put(uuid, loc);
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    throw new TimeoutException("Database operation timed out while loading warps");
                }
            }
        }
        return warpList;
    }

    public Location getWarp(UUID playerUUID, long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        String sql = "SELECT world, x, y, z FROM warps WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    return new Location(plugin.getServer().getWorld(worldName), x, y, z);
                }
            }
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while fetching warp for UUID " + playerUUID);
        }
        return null;
    }

    public UUID getWarpOwner(Location location, long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        String sql = "SELECT uuid FROM warps WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while fetching warp owner at " + location);
        }
        return null;
    }

    public Set<UUID> listWarps(long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        Set<UUID> uuids = new HashSet<>();
        String sql = "SELECT uuid FROM warps";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                uuids.add(UUID.fromString(rs.getString("uuid")));
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    throw new TimeoutException("Database operation timed out while listing warps");
                }
            }
        }
        return uuids;
    }

    public Collection<UUID> listSortedWarps(long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        TreeMap<Long, UUID> map = new TreeMap<>();
        String sql = "SELECT uuid, last_used FROM warps";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long lastUsed = rs.getLong("last_used");
                map.put(lastUsed, uuid);
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    throw new TimeoutException("Database operation timed out while listing sorted warps");
                }
            }
        }
        return map.descendingMap().values();
    }

    public void updateLastUsed(UUID playerUUID, long timeoutMs) throws SQLException, TimeoutException {
        long startTime = System.currentTimeMillis();
        String sql = "UPDATE warps SET last_used = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while updating last used for UUID " + playerUUID);
        }
    }
}