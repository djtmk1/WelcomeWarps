package com.djtmk.wwarps.database;

import com.djtmk.wwarps.WWarps;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class DataBase {
    private final WWarps plugin;
    private Connection connection;
    private final File dbFile;
    private static final int INIT_RETRIES = 3;

    public DataBase(WWarps plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "database/warps.db");
        initializeDatabase();
    }

    private void initializeDatabase() {
        for (int attempt = 1; attempt <= INIT_RETRIES; attempt++) {
            try {
                // Ensure the database directory exists
                File dbDir = new File(plugin.getDataFolder(), "database");
                if (!dbDir.exists()) {
                    if (dbDir.mkdirs()) {
                        plugin.getLogger().info("Created database directory: " + dbDir.getAbsolutePath());
                    } else {
                        plugin.getLogger().severe("Failed to create database directory: " + dbDir.getAbsolutePath());
                        continue;
                    }
                }

                // Check if database file is corrupted or inaccessible
                if (dbFile.exists() && !dbFile.canWrite()) {
                    plugin.getLogger().warning("Database file is not writable: " + dbFile.getAbsolutePath());
                    if (dbFile.delete()) {
                        plugin.getLogger().info("Deleted inaccessible database file to recreate");
                    } else {
                        plugin.getLogger().severe("Failed to delete inaccessible database file");
                        continue;
                    }
                }

                // Load SQLite JDBC driver
                Class.forName("org.sqlite.JDBC");
                plugin.getLogger().info("SQLite JDBC driver loaded successfully (attempt " + attempt + ")");

                // Connect to the database
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                connection.setAutoCommit(true);
                plugin.getLogger().info("Connected to database: " + dbFile.getAbsolutePath());

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
                    plugin.getLogger().info("Warps table created or verified successfully");

                    // Create index on last_used
                    stmt.executeUpdate(
                            "CREATE INDEX IF NOT EXISTS idx_last_used ON warps (last_used)"
                    );
                    plugin.getLogger().info("Index idx_last_used created or verified successfully");
                }

                // Verify table exists
                if (verifyTableExists()) {
                    plugin.getLogger().info("Warps table confirmed to exist");
                    return;
                } else {
                    plugin.getLogger().severe("Warps table creation failed - table does not exist");
                    if (dbFile.delete()) {
                        plugin.getLogger().info("Deleted potentially corrupted database file to retry");
                    }
                    continue;
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("SQLite JDBC driver not found (attempt " + attempt + "): " + e.getMessage());
                e.printStackTrace();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to initialize SQLite database (attempt " + attempt + "): " + e.getMessage());
                e.printStackTrace();
            }
            try {
                Thread.sleep(100); // Brief pause before retry
            } catch (InterruptedException ignored) {}
        }
        plugin.getLogger().severe("Failed to initialize database after " + INIT_RETRIES + " attempts - plugin may not function correctly");
    }

    private boolean verifyTableExists() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='warps'")) {
            boolean exists = rs.next();
            if (!exists && connection != null) {
                plugin.getLogger().warning("Warps table missing - attempting to recreate");
                try (Statement stmt2 = connection.createStatement()) {
                    stmt2.executeUpdate(
                            "CREATE TABLE warps (" +
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
                    plugin.getLogger().info("Recreated warps table during verification");

                    stmt2.executeUpdate(
                            "CREATE INDEX idx_last_used ON warps (last_used)"
                    );
                    plugin.getLogger().info("Recreated index idx_last_used during verification");
                    return true;
                }
            }
            return exists;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error verifying warps table: " + e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
        }
    }

    public void addWarp(UUID playerUUID, String playerName, Location location, long timeoutMs) throws SQLException, TimeoutException {
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot add warp - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
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
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot remove warp - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
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
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot remove warp - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
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
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot load warps - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
        long startTime = System.currentTimeMillis();
        Map<UUID, Location> warpList = new HashMap<>();
        String sql = "SELECT uuid, world, x, y, z FROM warps";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String worldName = rs.getString("world");
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World " + worldName + " not found for warp UUID " + uuid);
                    continue;
                }
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                Location loc = new Location(world, x, y, z);
                warpList.put(uuid, loc);
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new TimeoutException("Database operation timed out while loading warps");
            }
        }
        return warpList;
    }

    public Location getWarp(UUID playerUUID, long timeoutMs) throws SQLException, TimeoutException {
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot get warp - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
        long startTime = System.currentTimeMillis();
        String sql = "SELECT world, x, y, z FROM warps WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("World " + worldName + " not found for warp UUID " + playerUUID);
                        return null;
                    }
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    return new Location(world, x, y, z);
                }
            }
        }
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw new TimeoutException("Database operation timed out while fetching warp for UUID " + playerUUID);
        }
        return null;
    }

    public UUID getWarpOwner(Location location, long timeoutMs) throws SQLException, TimeoutException {
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot get warp owner - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
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
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot list warps - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
        long startTime = System.currentTimeMillis();
        Set<UUID> uuids = new HashSet<>();
        String sql = "SELECT uuid FROM warps";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                uuids.add(UUID.fromString(rs.getString("uuid")));
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new TimeoutException("Database operation timed out while listing warps");
            }
        }
        return uuids;
    }

    public Collection<UUID> listSortedWarps(long timeoutMs) throws SQLException, TimeoutException {
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot list sorted warps - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
        long startTime = System.currentTimeMillis();
        TreeMap<Long, UUID> map = new TreeMap<>();
        String sql = "SELECT uuid, last_used FROM warps";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long lastUsed = rs.getLong("last_used");
                map.put(lastUsed, uuid);
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new TimeoutException("Database operation timed out while listing sorted warps");
            }
        }
        return map.descendingMap().values();
    }

    public void updateLastUsed(UUID playerUUID, long timeoutMs) throws SQLException, TimeoutException {
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot update last used - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
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

    public UUID getUUIDByName(String playerName) throws SQLException {
        if (!verifyTableExists()) {
            plugin.getLogger().severe("Cannot get UUID by name - warps table does not exist");
            throw new SQLException("Warps table does not exist");
        }
        String sql = "SELECT uuid FROM warps WHERE player_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        }
        return null;
    }
}