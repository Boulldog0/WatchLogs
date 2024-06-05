package fr.Boulldogo.WatchLogs.Utils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Discord.SetupDiscordBot;

public class DatabaseManager {

    private Connection connection;
    private String url;
    private String username;
    private String password;
    private Logger logger;
    private boolean useMysql;
    private final Main plugin;
    private final SetupDiscordBot bot;
	public ItemDataSerializer dataSerializer;

    public DatabaseManager(String url, String username, String password, Logger logger, boolean useMysql, Main plugin, SetupDiscordBot bot, ItemDataSerializer dataSerializer) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.logger = logger;
        this.useMysql = useMysql;
        this.plugin = plugin;
        this.bot = bot;
        this.dataSerializer = dataSerializer;
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            if (useMysql) {
                connection = DriverManager.getConnection(url, username, password);
                logger.info("Connected to the MySQL database.");
            } else {
                connection = DriverManager.getConnection(url);
                logger.info("Connected to the SQLite database.");
            }
            createTableIfNotExists();
            maintainConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Error when plugin trying to connect with database! WatchLogs are disabled!");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void reconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            connect();
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Error when reconnecting to the database! WatchLogs are disabled!");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void maintainConnection() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                        reconnect();
                    } else {
                        Statement stmt = connection.createStatement();
                        stmt.execute("SELECT 1");
                        stmt.close();
                    }
                } catch (SQLException e) {
                    reconnect();
                    e.printStackTrace();
                    plugin.getLogger().severe("Error maintaining database connection! WatchLogs are disabled!");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L);
    }

    private void createTableIfNotExists() {
        String sql = useMysql
            ? "CREATE TABLE IF NOT EXISTS watchlogs_logs ("
            + "id INTEGER PRIMARY KEY AUTO_INCREMENT, "
            + "pseudo TEXT, "
            + "action TEXT, "
            + "location TEXT, "
            + "world TEXT, "
            + "result TEXT, "
            + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);"
            : "CREATE TABLE IF NOT EXISTS watchlogs_logs ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "pseudo TEXT, "
            + "action TEXT, "
            + "location TEXT, "
            + "world TEXT, "
            + "result TEXT, "
            + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);";
        
        String sql2 = "CREATE TABLE IF NOT EXISTS watchlogs_players ("
                + "pseudo TEXT(255),"
                + "tool_enabled BOOLEAN DEFAULT FALSE);";
        
        String sql3 = "undefinded";
        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
            sql3 = "CREATE TABLE IF NOT EXISTS watchlogs_items ("
                    + "id INTEGER PRIMARY KEY AUTO_INCREMENT, "
                    + "item_serialize TEXT,"
                    + "death_id INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            stmt.executeUpdate(sql2);
            if(!sql3.equals("undefinded")) {
                stmt.executeUpdate(sql3);
                logger.info("Table 'watchlogs_items' checked/created.");
            }
            logger.info("Table 'watchlogs_logs' checked/created.");
            logger.info("Table 'watchlogs_players' checked/created.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public ItemStack getItemById(int id) {
        String sql = "SELECT item_serialize FROM watchlogs_items WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String itemSerialize = rs.getString("item_serialize");
                    return dataSerializer.deserializeItemStack(itemSerialize);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean itemIdExists(int id) {
        String sql = "SELECT 1 FROM watchlogs_items WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public List<ItemStack> getItemsByDeathId(int deathId) {
        List<ItemStack> items = new ArrayList<>();
        String sql = "SELECT item_serialize FROM watchlogs_items WHERE death_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deathId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String itemSerialize = rs.getString("item_serialize");
                    ItemStack item = dataSerializer.deserializeItemStack(itemSerialize);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    @Nullable
    public void addItemEntry(int id, String itemSerialize, boolean isDeath, int deathId) {
        String sql = "INSERT INTO watchlogs_items (id, item_serialize, death_id, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, itemSerialize);
            pstmt.setInt(3, (isDeath ? deathId : -1)); 
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis())); 
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getLastDeathId() {
        String sql = "SELECT MAX(death_id) AS max_id FROM watchlogs_items";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public int getDatabaseSize() {
        String sql = "SELECT COUNT(*) AS total FROM watchlogs_logs";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if(rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean playerExists(String playerName) {
        String sql = "SELECT 1 FROM watchlogs_players WHERE pseudo = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    
    public void checkOrCreatePlayer(String playerName) {
        if(playerExists(playerName)) {
            return;
        }
        
        String sql = "INSERT INTO watchlogs_players (pseudo) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isToolEnabled(String playerName) {
        String sql = "SELECT tool_enabled FROM watchlogs_players WHERE pseudo = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                return rs.getBoolean("tool_enabled");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setToolEnabled(String playerName, boolean enabled) {
        String sql = "UPDATE watchlogs_players SET tool_enabled = ? WHERE pseudo = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setString(2, playerName);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }


    public void clearOldData(int days) {
        String sql = useMysql
            ? "DELETE FROM watchlogs_logs WHERE TIMESTAMPDIFF(DAY, timestamp, NOW()) > ?"
            : "DELETE FROM watchlogs_logs WHERE julianday('now') - julianday(timestamp) > ?";
        
        String sql2 = useMysql
                ? "DELETE FROM watchlogs_items WHERE TIMESTAMPDIFF(DAY, timestamp, NOW()) > ?"
                : "DELETE FROM watchlogs_items WHERE julianday('now') - julianday(timestamp) > ?";

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            int affectedRows = pstmt.executeUpdate();
            logger.info("Cleared " + affectedRows + " old log entries in logs table.");
        } catch(SQLException e) {
            e.printStackTrace();
        }
        
        try(PreparedStatement pstmt = connection.prepareStatement(sql2)) {
            pstmt.setInt(1, days);
            int affectedRows = pstmt.executeUpdate();
            logger.info("Cleared " + affectedRows + " old log entries in items table.");
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<String> getLogs(
    	    String world, String player, boolean useRayon, int centerX, int centerY, int centerZ, int radius,
    	    String action, String resultFilter, String timeFilter, boolean useTimestamp
    	) {
    	    String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
    	    List<String> logs = new ArrayList<>();
    	    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM watchlogs_logs WHERE 1 = 1");

    	    List<Object> parameters = new ArrayList<>();

    	    if(!world.equals("undefined")) {
    	        sqlBuilder.append(" AND world = ?");
    	        parameters.add(world);
    	    }
    	    if(!player.equals("undefined")) {
    	        sqlBuilder.append(" AND pseudo = ?");
    	        parameters.add(player);
    	    }
    	    if(!action.equals("undefined")) {
    	        sqlBuilder.append(" AND action = ?");
    	        parameters.add(action);
    	    }
    	    if(!resultFilter.equals("undefined")) {
    	        sqlBuilder.append(" AND result LIKE ?");
    	        parameters.add("%" + resultFilter + "%");
    	    }
    	    if(useTimestamp) {
    	        sqlBuilder.append(" AND timestamp >= ?");
    	        parameters.add(calculateTimeThreshold(timeFilter));
    	    }

    	    if(useRayon) {
    	        int minX = centerX - radius;
    	        int minY = centerY - radius;
    	        int minZ = centerZ - radius;
    	        int maxX = centerX + radius;
    	        int maxY = centerY + radius;
    	        int maxZ = centerZ + radius;

    	        List<String> locationConditions = new ArrayList<>();

    	        for (int x = minX; x <= maxX; x++) {
    	            for (int y = minY; y <= maxY; y++) {
    	                for (int z = minZ; z <= maxZ; z++) {
    	                    String location = x + "/" + y + "/" + z;
    	                    locationConditions.add("location = ?");
    	                    parameters.add(location);
    	                }
    	            }
    	        }

    	        if(!locationConditions.isEmpty()) {
    	            sqlBuilder.append(" AND (");
    	            sqlBuilder.append(String.join(" OR ", locationConditions));
    	            sqlBuilder.append(")");
    	        }
    	    }

    	    sqlBuilder.append(" ORDER BY id DESC");

    	    String sql = sqlBuilder.toString();

    	    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    	        for (int i = 0; i < parameters.size(); i++) {
    	            pstmt.setObject(i + 1, parameters.get(i));
    	        }

    	        try (ResultSet rs = pstmt.executeQuery()) {
    	            while (rs.next()) {
    	                int logId = rs.getInt("id");
    	                String pseudo = rs.getString("pseudo");
    	                String action2 = rs.getString("action");
    	                String location = rs.getString("location");
    	                String worldName = rs.getString("world");
    	                String result = rs.getString("result");
    	                String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
    	                logs.add(prefix + translateString("&cID &7" + logId + "&c | " + pseudo + " : &7" + action2 + " at " + timestamp + "\n"
    	                        + prefix + "&cWorld Information: &7" + worldName + " (" + location + ") \n"
    	                        + prefix + "&cOther Information : &7" + result + "\n&7]---------------------------["));
    	            }
    	        }
    	    } catch (SQLException e) {
    	        e.printStackTrace();
    	    }

    	    return logs;
    	}

    public Timestamp calculateTimeThreshold(String timeFilter) {
        Calendar cal = Calendar.getInstance();
        try {
            int amount = Integer.parseInt(timeFilter.substring(0, timeFilter.length() - 1));
            char unit = timeFilter.charAt(timeFilter.length() - 1);

            switch (unit) {
                case 's':
                    cal.add(Calendar.SECOND, -amount);
                    break;
                case 'm':
                    cal.add(Calendar.MINUTE, -amount);
                    break;
                case 'h':
                    cal.add(Calendar.HOUR, -amount);
                    break;
                case 'd':
                    cal.add(Calendar.DAY_OF_MONTH, -amount);
                    break;
                case 'w':
                    cal.add(Calendar.WEEK_OF_YEAR, -amount);
                    break;
                case 'M':
                    cal.add(Calendar.MONTH, -amount);
                    break;
                case 'y':
                    cal.add(Calendar.YEAR, -amount);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid time unit: " + unit);
            }

            java.util.Date threshold = cal.getTime();
            return new Timestamp(threshold.getTime());
        } catch (StringIndexOutOfBoundsException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void insertLog(String pseudo, String action, String location, String world, String result) {
        String sql = "INSERT INTO watchlogs_logs (id, pseudo, action, location, world, result, timestamp) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        int id = getLastLogId();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id + 1);
            pstmt.setString(2, pseudo);
            pstmt.setString(3, action);
            pstmt.setString(4, location);
            pstmt.setString(5, world);
            pstmt.setString(6, result);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(plugin.getConfig().getBoolean("log-in-file")) {
            Calendar calendar = Calendar.getInstance();
            String todayFileName = calendar.get(Calendar.DAY_OF_MONTH) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
            File folder = new File(plugin.getDataFolder(), "logs");
            File todayFile = new File(folder, todayFileName + ".yml");

            if(!folder.exists()) {
                folder.mkdirs();
            }

            YamlConfiguration config;
            if(todayFile.exists()) {
                config = YamlConfiguration.loadConfiguration(todayFile);
            } else {
                config = new YamlConfiguration();
            }

            config.set("[WatchLogs][" + todayFileName + "][" + (id + 1) + "][" + System.currentTimeMillis() + "]", "Action : " + action + "; Player : " + pseudo + "; Information : " + result + "; Location : " + location + "; World : " + world + " .");
            try {
                config.save(todayFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(plugin.getConfig().getBoolean("discord.discord-module-enabled")) {
            if(bot.isBotOnline()) {
            	if(isDiscordLogEnable(action)) {
                    if(plugin.getConfig().getBoolean("discord.enable_live_logs") && plugin.getConfig().contains("discord.live_logs_channel_id")) {
                        bot.sendDirectLogs(id + 1, getFormattedNameForActions(action), result, Bukkit.getPlayer(pseudo), world, location);
                    }
            	}
            }
        }
    }
    
    private boolean isDiscordLogEnable(String logName) {
		return plugin.getConfig().getBoolean("enable-discord-logs." + logName);
	}

	private String getFormattedNameForActions(String s) {
        switch (s) {
            case "join":
                return "Join";
            case "leave":
                return "Leave";
            case "teleport":
                return "Teleport";
            case "block-place":
                return "Block Place";
            case "block-break":
                return "Block Break";
            case "container-open":
                return "Container Open";
            case "container-transaction":
                return "Container Transaction";
            case "item-drop":
                return "Item Drop";
            case "item-pickup":
                return "Item Pickup";
            case "item-break":
                return "Item Break";
            case "player-death":
                return "Player Death";
            case "player-death-loot":
                return "Player Death Loot";
            case "commands":
                return "Commands";
            case "send-message":
                return "Send Message";
            case "interact-item":
                return "Interact Item";
            case "interact-block":
                return "Interact Block";
            case "interact-entity":
                return "Interact Entity";
            case "block-explosion":
            	return "Block Explosion";
            default:
                return s + " (Formatted Name unknown)";
        }
    }
    
    public List<String> getLogs(int x, int y, int z, String world) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        List<String> logs = new ArrayList<>();
        String sql = "SELECT * FROM watchlogs_logs WHERE location = ? AND world = ? AND (action = 'block-place' OR action = 'block-break' OR action = 'block-explosion') ORDER BY id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, x + "/" + y + "/" + z);
            pstmt.setString(2, world);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String pseudo = rs.getString("pseudo");
                    String action = rs.getString("action");
                    String location = rs.getString("location");
                    String worldName = rs.getString("world");
                    String result = rs.getString("result");
                    String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
                    logs.add(prefix + translateString("&cID &7" + id + "&c | " + pseudo + " : &7" + action + " at " + timestamp + "\n"
                    		+ prefix + "&cWorld Information: &7" + worldName + " (" + location + ") \n"
                    		+ prefix + "&cOther Information : &7" + result + "\n&7]---------------------------["));
                    
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    public List<String> getContainerLogs(int x, int y, int z, String world) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        List<String> logs = new ArrayList<>();
        String sql = "SELECT * FROM watchlogs_logs WHERE location = ? AND world = ? AND (action = 'container-transaction' OR action = 'container-open') ORDER BY id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, x + "/" + y + "/" + z);
            pstmt.setString(2, world);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String pseudo = rs.getString("pseudo");
                    String action = rs.getString("action");
                    String location = rs.getString("location");
                    String worldName = rs.getString("world");
                    String result = rs.getString("result");
                    String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
                    logs.add(prefix + translateString("&cID &7" + id + "&c | " + pseudo + " : &7" + action + " at " + timestamp + "\n"
                    		+ prefix + "&cWorld Information: &7" + worldName + " (" + location + ") \n"
                    		+ prefix + "&cOther Information : &7" + result + "\n&7]---------------------------["));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    public String getLocationOfId(int id) {
        String sql = "SELECT location FROM watchlogs_logs WHERE id = ?";
        String location = null;

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    location = rs.getString("location");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return location;
    }

    public String getWorldOfId(int id) {
    	String sql = "SELECT world FROM watchlogs_logs WHERE id = ?";
    	String world = null;
    	
    	try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
    		pstmt.setInt(1, id);
    		try(ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    world = rs.getString("world");
                }
    		}
    	} catch (SQLException e) {
            e.printStackTrace();
        }
    	
    	return world;
    }
    
    public String getDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Database Information:\n");

        if(connection != null) {
            try {
                if(connection.isValid(5)) {
                    info.append("Connection Status: Connected\n");
                } else {
                    info.append("Connection Status: Disconnected\n");
                }
            } catch (SQLException e) {
                info.append("Connection Status: Disconnected (Exception: ").append(e.getMessage()).append(")\n");
            }
        } else {
            info.append("Connection Status: Disconnected\n");
        }

        if(connection != null) {
            try {
                long start = System.currentTimeMillis();
                connection.createStatement().execute("/* ping */ SELECT 1");
                long ping = System.currentTimeMillis() - start;
                info.append("Ping: ").append(ping).append(" ms\n");
            } catch (SQLException e) {
                info.append("Ping: N/A (Exception: ").append(e.getMessage()).append(")\n");
            }
        } else {
            info.append("Ping: N/A (Connection is null)\n");
        }

        return info.toString();
    }

    public int getLastLogId() {
        String sql = "SELECT MAX(id) AS max_id FROM watchlogs_logs";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if(rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String translateString(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}