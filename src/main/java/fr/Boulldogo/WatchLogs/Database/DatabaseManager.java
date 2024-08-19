package fr.Boulldogo.WatchLogs.Database;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Discord.SetupDiscordBot;
import fr.Boulldogo.WatchLogs.Utils.ActionUtils;
import fr.Boulldogo.WatchLogs.Utils.BCryptUtils;
import fr.Boulldogo.WatchLogs.Utils.ItemDataSerializer;

public class DatabaseManager {

    private Connection connection;
    private String url;
    private String username;
    private String password;
    private Logger logger;
    private boolean useMysql;
    private final WatchLogsPlugin plugin;
	public ItemDataSerializer dataSerializer;

    public DatabaseManager(String url, String username, String password, Logger logger, boolean useMysql, WatchLogsPlugin plugin, ItemDataSerializer dataSerializer) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.logger = logger;
        this.useMysql = useMysql;
        this.plugin = plugin;
        this.dataSerializer = dataSerializer;
    }

    public void connect() {
        try {
            if(connection != null && !connection.isClosed()) {
                return;
            }
            if(useMysql) {
                connection = DriverManager.getConnection(url, username, password);
                logger.info("Connected to the MySQL database.");
            } else {
                connection = DriverManager.getConnection(url);
                logger.info("Connected to the SQLite database.");
            }
            createTableIfNotExists();
            addColumnIfNotExists();
            maintainConnection();           
            if(plugin.getConfig().getBoolean("multi-server.enable")) {
            	String serverName = plugin.getConfig().getString("multi-server.server-name");
            	if(!doesServerExist(serverName)) {
            		String adress = plugin.getServerUtils().getExternalIPAddress();
            		createServer(serverName, adress);
            	} else {
            		setServerStatus(serverName, true);
            	}
            }
        } catch(SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Error when plugin trying to connect with database! WatchLogs are disabled!");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void reconnect() {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
            connect();
        } catch(SQLException e) {
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
                    if(connection == null || connection.isClosed()) {
                        reconnect();
                    } else {
                        Statement stmt = connection.createStatement();
                        stmt.execute("SELECT 1 FROM watchlogs_logs");
                        stmt.close();
                    }
                } catch(SQLException e) {
                    e.printStackTrace();
                    plugin.getLogger().severe("Error maintaining database connection! WatchLogs are disabled!");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L);
    }

    private void createTableIfNotExists() {
        String sql = useMysql
            ? "CREATE TABLE IF NOT EXISTS watchlogs_logs("
            + "id INTEGER PRIMARY KEY AUTO_INCREMENT, "
            + "pseudo TEXT, "
            + "action TEXT, "
            + "location TEXT, "
            + "world TEXT, "
            + "result TEXT, "
            + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);"
            : "CREATE TABLE IF NOT EXISTS watchlogs_logs("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "pseudo TEXT, "
            + "action TEXT, "
            + "location TEXT, "
            + "world TEXT, "
            + "result TEXT, "
            + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "server VARCHAR(255));";
        
        String sql2 = "CREATE TABLE IF NOT EXISTS watchlogs_players("
                + "pseudo TEXT(255),"
                + "tool_enabled BOOLEAN DEFAULT FALSE);";
        
        String sql3 = "undefinded";
        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
            sql3 = "CREATE TABLE IF NOT EXISTS watchlogs_items("
                    + "id INTEGER PRIMARY KEY AUTO_INCREMENT, "
                    + "item_serialize TEXT,"
                    + "death_id INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);";
        }
        
        String sql4 = "CREATE TABLE IF NOT EXISTS watchlogs_accounts("
                 + "username TEXT(255) UNIQUE, "  
                 + "password TEXT(255));";
        
        String sql5 = "undefined";
        if(plugin.getConfig().getBoolean("multi-server.enable")) {
        	sql5 = "CREATE TABLE IF NOT EXISTS watchlogs_servers("
        			+ "server_name TEXT(255) UNIQUE, "
        			+ "server_adress TEXT(255), "
        			+ "online BOOLEAN DEFAULT FALSE, "
        			+ "mark_offline INTEGER DEFAULT 0);";
        }
        
        String sql6 = "undefined";
        if(plugin.getConfig().getBoolean("trace-item.enable")) {
        	sql6 = "CREATE TABLE IF NOT EXISTS watchlogs_traceitem("
        			+ "uuid VARCHAR(64) UNIQUE, "
        			+ "actions_id TEXT);";
        }

        try(Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            stmt.executeUpdate(sql2);
            stmt.executeUpdate(sql4);
            logger.info("Table 'watchlogs_logs' checked/created.");
            logger.info("Table 'watchlogs_players' checked/created.");
            logger.info("Table 'watchlogs_accounts' checked/created.");
            if(!sql3.equals("undefinded")) {
                stmt.executeUpdate(sql3);
                logger.info("Table 'watchlogs_items' checked/created.");
            }
            if(!sql5.equals("undefined")) {
                stmt.executeUpdate(sql5);
                logger.info("Table 'watchlogs_servers' checked/created.");
            }
            if(!sql6.equals("undefined")) {
            	stmt.executeUpdate(sql6);
                logger.info("Table 'watchlogs_traceitem' checked/created.");
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void addColumnIfNotExists() throws SQLException {
    	if(useMysql) {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet resultSet = meta.getColumns(null, null, "watchlogs_logs", "server");

            if(!resultSet.next()) { 
                String sql = "ALTER TABLE watchlogs_logs ADD COLUMN server VARCHAR(255)";
                try(Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                    plugin.getLogger().info("Column server added to watchlogs_logs.");
                } catch(SQLException e) {
                    e.printStackTrace();
                }
            } else {
            	plugin.getLogger().info("Column server already exists in watchlogs_logs.");
            }
    	} else {
            boolean columnExists = false;
            String query = "PRAGMA table_info(watchlogs_logs);";
            
            try(Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while(rs.next()) {
                    String existingColumn = rs.getString("name");
                    if(existingColumn.equals("server")) {
                        columnExists = true;
                        break;
                    }
                }
            } catch(SQLException e) {
                e.printStackTrace();
            }

            if(!columnExists) {
                String sql = "ALTER TABLE watchlogs_logs ADD COLUMN server VARCHAR(255)";
                try(Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                    plugin.getLogger().info("Column server added to watchlogs_logs.");
                } catch(SQLException e) {
                    e.printStackTrace();
                }
            } else {
            	plugin.getLogger().info("Column server already exists in watchlogs_logs.");
            }
    	}
    }
    
    public boolean createServer(String serverName, String serverAddress) {
        String query = "INSERT INTO watchlogs_servers (server_name, server_adress, online) VALUES (?, ?, TRUE)";
        
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, serverName);
            stmt.setString(2, serverAddress);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean setServerStatus(String serverName, boolean isOnline) {
        String query = "UPDATE watchlogs_servers SET online = ? WHERE server_name = ?";
        
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setBoolean(1, isOnline);
            stmt.setString(2, serverName);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void markServerOffline() {
        if(plugin.getConfig().getBoolean("multi-server.enable")) {
        	String serverName = plugin.getConfig().getString("multi-server.server-name");
    		setServerStatus(serverName, false);
        }
    }
    
    public List<String> getServerInNetwork() {
        List<String> servers = new ArrayList<>();
        String query = "SELECT server_name FROM watchlogs_servers";

        try(PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while(rs.next()) {
                servers.add(rs.getString("server_name"));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return servers;
    }
    
    public boolean isServerOnline(String serverName) {
        String query = "SELECT online FROM watchlogs_servers WHERE server_name = ?";
        
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, serverName);
            try(ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("online");
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false; 
    }
    
    public boolean doesServerExist(String serverName) {
        String query = "SELECT 1 FROM watchlogs_servers WHERE server_name = ?";
        
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, serverName);
            try(ResultSet rs = stmt.executeQuery()) {
                return rs.next(); 
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public String getServerAdress(String serverName) {
    	String query = "SELECT server_adress FROM watchlogs_servers WHERE server_name = ?";
    	
    	try(PreparedStatement stmt = connection.prepareStatement(query)) {
    		stmt.setString(1, serverName);
    		try(ResultSet rs = stmt.executeQuery()) {
    			if(rs.next()) {
    				return rs.getString("server_adress");
    			} else {
    				return null;
    			}
    		}
    	} catch(SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean UUIDExists(String UUID) {
    	String query = "SELECT 1 FROM watchlogs_traceitem WHERE uuid = ?";
    	
    	try(PreparedStatement stmt = connection.prepareStatement(query)) {
    		stmt.setString(1, UUID);
    		try(ResultSet rs = stmt.executeQuery()) {
    			return rs.next();
    		}
    	} catch(SQLException e) {
            e.printStackTrace();
        }
    	return false;
    }
    
    public boolean registerFirstItemLog(String UUID, int logId) {
    	String query = "INSERT INTO watchlogs_traceitem (uuid, actions_id) VALUES (?, ?)";
    	
    	try(PreparedStatement stmt = connection.prepareStatement(query)) {
    		stmt.setString(1, UUID);
    		stmt.setString(2, String.valueOf(logId));
            stmt.executeUpdate();
            return true;
    	} catch (SQLException e) {
    		e.printStackTrace();
    		return false;
		}
    }
    
    public void setItemStringLog(String UUID, String logString) {
    	String query = "UPDATE watchlogs_traceitem SET actions_id = ? WHERE uuid = ?";
    	
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, logString);
            pstmt.setString(2, UUID);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public String getActionString(String UUID) throws SQLException {
    	String query = "SELECT actions_id FROM watchlogs_traceitem WHERE uuid = ?";
    	
    	try(PreparedStatement stmt = connection.prepareStatement(query)) {
    		stmt.setString(1, UUID);
    		try(ResultSet rs = stmt.executeQuery()) {
    			if(rs.next()) {
    				return rs.getString("actions_id");
    			} else {
    				return String.valueOf(-1);
    			}
    		}
    	}
    }
    
    public String getUUIDForAction(int id) throws SQLException {
        String query = "SELECT uuid FROM watchlogs_traceitem WHERE actions_id LIKE ?";

        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, "%" + id + "%");
            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) {
                    return rs.getString("uuid");
                } else {
                    return null; 
                }
            }
        }
    }
    
    public boolean deleteUUID(String UUID) throws SQLException {
        String query = "DELETE FROM watchlogs_traceitem WHERE uuid = ?";

        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, UUID);
            stmt.execute();
            return true;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
  
    public Connection getConnection() {
    	return connection;
    }
    
    public String getServerLog(int logId) throws SQLException {
    	String query = "SELECT server FROM watchlogs_logs WHERE id = ?";
    	
    	try(PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1, logId);
    		try(ResultSet rs = pstmt.executeQuery()) {
    			if(rs.next()) {
    				return rs.getString("server");
    			} else {
    				return null;
    			}
    		}
    	}
    }
    
    public boolean authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT * FROM watchlogs_accounts WHERE username = ? AND password = ?";
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
        	String hashedPass = getHashedPassword(username);
            stmt.setString(1, username);
            stmt.setString(2, hashedPass);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && BCryptUtils.checkPassword(password, hashedPass);
        }
    }
    
    public String getHashedPassword(String username) throws SQLException {
        String query = "SELECT password FROM watchlogs_accounts WHERE username = ?";
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) {
                    return rs.getString("password");
                } else {
                    return null;
                }
            }
        }
    }

    public void createUser(String username, String password) throws SQLException {   
        String query = "INSERT INTO watchlogs_accounts(username, password) VALUES(?, ?)";
        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            String hashedPassword = BCryptUtils.hashPassword(password);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.execute();
        } catch(SQLException e) {
            e.printStackTrace();
            throw new SQLException("Failed to create user: " + e.getMessage());
        }
    }
    
    public boolean deleteUser(String username) {
        String query = "DELETE FROM watchlogs_accounts WHERE username = ?";

        try(PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.execute();
            return true;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void changeUserPassword(String username, String password) {
        String query = "UPDATE watchlogs_account SET password = ? WHERE username = ?";
        
        String hashedPassword = BCryptUtils.hashPassword(password);
        
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isUserRegistered(String username) {
        String query = "SELECT 1 FROM watchlogs_accounts WHERE username = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            try(ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); 
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public int getNumberOfAccounts() {
        String query = "SELECT COUNT(*) FROM watchlogs_accounts";
        try(Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if(rs.next()) {
                return rs.getInt(1); 
            } else {
                return 0;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public ItemStack getItemById(int id) {
        String sql = "SELECT item_serialize FROM watchlogs_items WHERE id = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try(ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    String itemSerialize = rs.getString("item_serialize");
                    return dataSerializer.deserializeItemStack(itemSerialize);
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean itemIdExists(int id) {
        String sql = "SELECT 1 FROM watchlogs_items WHERE id = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try(ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public List<ItemStack> getItemsByDeathId(int deathId) {
        List<ItemStack> items = new ArrayList<>();
        String sql = "SELECT item_serialize FROM watchlogs_items WHERE death_id = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deathId);
            try(ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    String itemSerialize = rs.getString("item_serialize");
                    ItemStack item = dataSerializer.deserializeItemStack(itemSerialize);
                    if(item != null) {
                        items.add(item);
                    }
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    @Nullable
    public void addItemEntry(String itemSerialize, boolean isDeath, int deathId) {
        String sql = "INSERT INTO watchlogs_items(item_serialize, death_id, timestamp) VALUES(?, ?, ?)";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemSerialize);
            pstmt.setInt(2,(isDeath ? deathId : -1)); 
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis())); 
            pstmt.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public int getLastDeathId() {
        String sql = "SELECT MAX(death_id) AS max_id FROM watchlogs_items";
        try(Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if(rs.next()) {
                return rs.getInt("max_id");
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public int getDatabaseSize() {
        String sql = "SELECT COUNT(*) AS total FROM watchlogs_logs";
        try(Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if(rs.next()) {
                return rs.getInt("total");
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean playerExists(String playerName) {
        String sql = "SELECT 1 FROM watchlogs_players WHERE pseudo = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    
    public void checkOrCreatePlayer(String playerName) {
        if(playerExists(playerName)) {
            return;
        }
        
        String sql = "INSERT INTO watchlogs_players(pseudo) VALUES(?)";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isToolEnabled(String playerName) {
        String sql = "SELECT tool_enabled FROM watchlogs_players WHERE pseudo = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                return rs.getBoolean("tool_enabled");
            }
        } catch(SQLException e) {
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
    	    String action, String resultFilter, String timeFilter, boolean useTimestamp, int limit, String serverNameResearch
    	) {
    	    String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
    	    List<String> logs = new ArrayList<>();
    	    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM watchlogs_logs WHERE 1 = 1");
    	    String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.server-name") : "undefined";
    	    boolean authorizeMultiServerResearch = plugin.getConfig().getBoolean("multi-server.enable-log-research-in-multiserver") && !serverName.equals("undefined");

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
    	    
    	    if(!authorizeMultiServerResearch) {
        	    if(!serverName.equals("undefined")) {
            	    sqlBuilder.append(" AND server = ?");
            	    parameters.add(serverName);
        	    }
    	    } else {
    	    	if(!serverNameResearch.equals("undefined")) {
            	    sqlBuilder.append(" AND server = ?");
            	    parameters.add(serverNameResearch);
    	    	}
    	    }

    	    if(useRayon) {
    	        int minX = centerX - radius;
    	        int minY = centerY - radius;
    	        int minZ = centerZ - radius;
    	        int maxX = centerX + radius;
    	        int maxY = centerY + radius;
    	        int maxZ = centerZ + radius;

    	        List<String> locationConditions = new ArrayList<>();

    	        for(int x = minX; x <= maxX; x++) {
    	            for(int y = minY; y <= maxY; y++) {
    	                for(int z = minZ; z <= maxZ; z++) {
    	                    String location = x + "/" + y + "/" + z;
    	                    locationConditions.add("location = ?");
    	                    parameters.add(location);
    	                }
    	            }
    	        }

    	        if(!locationConditions.isEmpty()) {
    	            sqlBuilder.append(" AND(");
    	            sqlBuilder.append(String.join(" OR ", locationConditions));
    	            sqlBuilder.append(")");
    	        }
    	    }

    	    sqlBuilder.append(" ORDER BY id DESC LIMIT " + limit);

    	    String sql = sqlBuilder.toString();

    	    try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
    	        for(int i = 0; i < parameters.size(); i++) {
    	            pstmt.setObject(i + 1, parameters.get(i));
    	        }

    	        try(ResultSet rs = pstmt.executeQuery()) {
    	            while(rs.next()) {
    	                int logId = rs.getInt("id");
    	                String pseudo = rs.getString("pseudo");
    	                String action2 = rs.getString("action");
    	                String location = rs.getString("location");
    	                String worldName = rs.getString("world");
    	                String result = rs.getString("result");
    	                String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
    	                String server = rs.getString("server") != null ? rs.getString("server") : "Unknow";
    	                logs.add(prefix + translateString("&cID &7" + logId + "&c | " + pseudo + " : &7" + action2 + " at " + timestamp + "\n"
    	                		+ prefix + "&cServer Name: &7" + server + "\n"
    	                        + prefix + "&cWorld Information: &7" + worldName + "(" + location + ") \n"
    	                        + prefix + "&cOther Information : &7" + result + "\n&7]---------------------------["));
    	            }
    	        }
    	    } catch(SQLException e) {
    	        e.printStackTrace();
    	    }

    	    return logs;
    	}
    
    public List<String> getJsonLogs(
    	    String world, String player, boolean useRayon, int centerX, int centerY, int centerZ, int radius,
    	    String action, String resultFilter, String timeFilter, boolean useTimestamp
    	) {
    	    List<String> logs = new ArrayList<>();
    	    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM watchlogs_logs WHERE 1 = 1");
    	    String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.server-name") : "undefined";

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
    	    
    	    if(!serverName.equals("undefined")) {
        	    sqlBuilder.append(" AND server = ?");
        	    parameters.add(serverName);
    	    }

    	    if(useRayon) {
    	        int minX = centerX - radius;
    	        int minY = centerY - radius;
    	        int minZ = centerZ - radius;
    	        int maxX = centerX + radius;
    	        int maxY = centerY + radius;
    	        int maxZ = centerZ + radius;

    	        List<String> locationConditions = new ArrayList<>();

    	        for(int x = minX; x <= maxX; x++) {
    	            for(int y = minY; y <= maxY; y++) {
    	                for(int z = minZ; z <= maxZ; z++) {
    	                    String location = x + "/" + y + "/" + z;
    	                    locationConditions.add("location = ?");
    	                    parameters.add(location);
    	                }
    	            }
    	        }

    	        if(!locationConditions.isEmpty()) {
    	            sqlBuilder.append(" AND(");
    	            sqlBuilder.append(String.join(" OR ", locationConditions));
    	            sqlBuilder.append(")");
    	        }
    	    }

    	    sqlBuilder.append(" ORDER BY id DESC");

    	    String sql = sqlBuilder.toString();

    	    try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
    	        for(int i = 0; i < parameters.size(); i++) {
    	            pstmt.setObject(i + 1, parameters.get(i));
    	        }

    	        try(ResultSet rs = pstmt.executeQuery()) {
    	            while(rs.next()) {
    	                int logId = rs.getInt("id");
    	                String pseudo = rs.getString("pseudo");
    	                String action2 = rs.getString("action");
    	                String location = rs.getString("location");
    	                String worldName = rs.getString("world");
    	                String result = rs.getString("result");
    	                String timestamp = String.valueOf(rs.getTimestamp("timestamp"));

    	                JSONObject logJson = new JSONObject();
    	                logJson.put("id", logId);
    	                logJson.put("pseudo", pseudo);
    	                logJson.put("action", action2);
    	                logJson.put("location", location);
    	                logJson.put("world", worldName);
    	                logJson.put("result", result);
    	                logJson.put("timestamp", timestamp);
    	                if(!serverName.equals("undefined")) {
    	                	logJson.put("server", serverName);
    	                } else {
    	                	logJson.put("server", "undefined");
    	                }

    	                logs.add(logJson.toString());
    	            }
    	        }
    	    } catch(SQLException e) {
    	        e.printStackTrace();
    	    }

    	    return logs;
    	}
    
    
    public List<String> getWebJsonLogs(String world, String player, String loca,  String action, String resultFilter, String timeFilter, boolean useTimestamp, boolean canViewLocation, String serverName) {
        List<String> logs = new ArrayList<>();
        
        boolean hideLocation = plugin.getConfig().getBoolean("website.hide-coordinates-in-logs") || !canViewLocation;
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM watchlogs_logs WHERE 1 = 1");

        List<Object> parameters = new ArrayList<>();

        if(world != null && !world.equals("undefined")) {
            sqlBuilder.append(" AND world = ?");
            parameters.add(world);
        }
        if(player != null && !player.equals("undefined")) {
            sqlBuilder.append(" AND pseudo = ?");
            parameters.add(player);
        }
        if(loca != null && !loca.equals("%/%/%")) {
            sqlBuilder.append(" AND location = ?");
            parameters.add(loca);
        }
        if(action != null && !action.equals("undefined")) {
            sqlBuilder.append(" AND action = ?");
            parameters.add(action);
        }
        if(resultFilter != null && !resultFilter.equals("undefined")) {
            sqlBuilder.append(" AND result LIKE ?");
            parameters.add("%" + resultFilter + "%");
        }
        
        if(useTimestamp) {
            sqlBuilder.append(" AND timestamp >= ?");
            parameters.add(calculateTimeThreshold(timeFilter));
        } else {
            String configTimeLimit = plugin.getConfig().getString("log-time-limit", "7d");
            Timestamp timeThreshold = calculateTimeThreshold(configTimeLimit);
            if(timeThreshold != null) {
                sqlBuilder.append(" AND timestamp >= ?");
                parameters.add(timeThreshold);
            }
        }
        
	    if(!serverName.equals("undefined")) {
    	    sqlBuilder.append(" AND server = ?");
    	    parameters.add(serverName);
	    }

        sqlBuilder.append(" ORDER BY id DESC");

        String sql = sqlBuilder.toString();

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for(int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            try(ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    int logId = rs.getInt("id");
                    String pseudo = rs.getString("pseudo");
                    String action2 = rs.getString("action");
                    String location = hideLocation ? "Hide by administrator" : rs.getString("location");
                    String worldName = rs.getString("world");
                    String result = rs.getString("result");
                    String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
                    String server = rs.getString("server");

                    JSONObject logJson = new JSONObject();
                    logJson.put("id", logId);
                    logJson.put("pseudo", pseudo);
                    logJson.put("action", action2);
                    logJson.put("location", location);
                    logJson.put("world", worldName);
                    logJson.put("result", result);
                    logJson.put("timestamp", timestamp);
                    logJson.put("server", server);

                    logs.add(logJson.toString());
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    public Timestamp calculateTimeThreshold(String timeFilter) {
        Calendar cal = Calendar.getInstance();
        try {
            int amount = Integer.parseInt(timeFilter.substring(0, timeFilter.length() - 1));
            char unit = timeFilter.charAt(timeFilter.length() - 1);

            switch(unit) {
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
        } catch(StringIndexOutOfBoundsException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void insertLog(String pseudo, String action, String location, String world, String result) {
	    String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.server-name") : "undefined";
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = !serverName.equals("undefined") ? "INSERT INTO watchlogs_logs(pseudo, action, location, world, result, timestamp, server) VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)" :
                	"INSERT INTO watchlogs_logs(pseudo, action, location, world, result, timestamp) VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, pseudo);
                    pstmt.setString(2, action);
                    pstmt.setString(3, location);
                    pstmt.setString(4, world);
                    pstmt.setString(5, result);
                    if(!serverName.equals("undefined")) {
                    	if(action.equals("website-login") || action.equals("website-logout") 
                    		|| action.equals("website-logs-search") || action.equals("website-register")) {
                        	pstmt.setString(6, "Unknow (Non-ingame event)");
                    	} else {
                        	pstmt.setString(6, serverName);
                    	}
                    }
                    pstmt.executeUpdate();
                } catch(SQLException e) {
                    e.printStackTrace();
                }

                if(plugin.getConfig().getBoolean("log-in-file")) {
                    logToFile(pseudo, action, location, world, result);
                }

                if(plugin.getConfig().getBoolean("discord.discord-module-enabled")) {
                    logToDiscord(pseudo, action, location, world, result, serverName);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    private void logToFile(String pseudo, String action, String location, String world, String result) {
        SimpleDateFormat todayFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        File folder = new File(plugin.getDataFolder(), "logs");
        File todayFile = new File(folder, todayFileName.format(new java.util.Date()) + ".yml");
        int logs = 0;

        if(!folder.exists()) {
            folder.mkdirs();
        }

        YamlConfiguration config;
        if(todayFile.exists()) {
            config = YamlConfiguration.loadConfiguration(todayFile);
        } else {
            config = new YamlConfiguration();
        }

        config.set("[WatchLogs][" + todayFileName.format(new java.util.Date()) + "][" +(getLastLogId() + 1) + "][" + System.currentTimeMillis() + "]", "Action : " + action + "; Player : " + pseudo + "; Information : " + result + "; Location : " + location + "; World : " + world + " .");
        logs++;

        if(logs >= 1000) {
            logs = 0;
            try {
                config.save(todayFile);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void logToDiscord(String pseudo, String action, String location, String world, String result, String serverName) {
        SetupDiscordBot bot = plugin.getDiscordBot();
        if(bot.isBotOnline()) {
            if(isDiscordLogEnable(action)) {
                if(plugin.getConfig().getBoolean("discord.enable_live_logs") && plugin.getConfig().contains("discord.live_logs_channel_id")) {
                    bot.sendDirectLogs(getLastLogId() + 1, ActionUtils.getFormattedNameForActions(action), result, pseudo, world, location, action, serverName);
                }
            }
        }
    }

    public List<String> getAllLogs(boolean isWebsiteRequest, boolean canViewLocation) throws SQLException {
        if(connection == null || connection.isClosed() || !connection.isValid(2)) {
            reconnect(); 
        }
        List<String> logs = new ArrayList<>();

        boolean hideLocation = plugin.getConfig().getBoolean("website.hide-coordinates-in-logs") || !canViewLocation;
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM watchlogs_logs");
        List<Object> parameters = new ArrayList<>();

        if(isWebsiteRequest) {
            String configTimeLimit = plugin.getConfig().getString("website.website-log-time-limit-showed", "7d");
            Timestamp timeThreshold = calculateTimeThreshold(configTimeLimit);
            if(timeThreshold != null) {
                sqlBuilder.append(" WHERE timestamp >= ?");
                parameters.add(timeThreshold);
            }
        }

        sqlBuilder.append(" ORDER BY id DESC");
        String sql = sqlBuilder.toString();

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for(int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            try(ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    int logId = rs.getInt("id");
                    String pseudo = rs.getString("pseudo");
                    String action = rs.getString("action");
                    String location = hideLocation ? "Hide by administrator" : rs.getString("location");
                    String worldName = rs.getString("world");
                    String result = rs.getString("result");
                    String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
                    String serverName = rs.getString("server");

                    JSONObject logJson = new JSONObject();
                    logJson.put("id", logId);
                    logJson.put("pseudo", pseudo);
                    logJson.put("action", action);
                    logJson.put("location", location);
                    logJson.put("world", worldName);
                    logJson.put("result", result);
                    logJson.put("timestamp", timestamp);
                    if(serverName != null) {
                    	logJson.put("server", serverName);
                    } else {
                    	logJson.put("server", "undefined");
                    }

                    logs.add(logJson.toString());
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }
    
    public boolean isBlockInteract(String location, String playerName) {
	    String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.server-name") : "undefined";

        String sql = !serverName.equals("undefined") ? "SELECT action FROM watchlogs_logs WHERE location = ? AND pseudo = ? AND server = ? ORDER BY id DESC LIMIT 1"
        		: "SELECT action FROM watchlogs_logs WHERE location = ? AND pseudo = ? ORDER BY id DESC LIMIT 1";
        List<Object> parameters = new ArrayList<>();
        parameters.add(location);
        parameters.add(playerName);
        if(!serverName.equals("undefined")) {
        	parameters.add(serverName);
        }
        
        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for(int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            
            try(ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    String action = rs.getString("action");
                    return !action.equals("block-break") && !action.equals("block-place");
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public void insertJsonLog(int id, String pseudo, String action, String location, String world, String result, String timestamp, boolean forceId, String serverName) {
    	if(!forceId) {
            String sql = "INSERT INTO watchlogs_logs(id, pseudo, action, location, world, result, timestamp, server) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
            try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, id + 1);
                pstmt.setString(2, pseudo);
                pstmt.setString(3, action);
                pstmt.setString(4, location);
                pstmt.setString(5, world);
                pstmt.setString(6, result);
                pstmt.setTimestamp(7, Timestamp.valueOf(timestamp));
                pstmt.setString(8, serverName);
                pstmt.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();
            }
    	} else {
    		String sql = "INSERT INTO watchlogs_logs(id, pseudo, action, location, world, result, timestamp, server) " +
    	             "VALUES(?, ?, ?, ?, ?, ?, ?, ?) " +
    	             "ON DUPLICATE KEY UPDATE pseudo = VALUES(pseudo), action = VALUES(action), location = VALUES(location), world = VALUES(world), result = VALUES(result), timestamp = VALUES(timestamp)";

    	try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
    	    pstmt.setInt(1, id);
    	    pstmt.setString(2, pseudo);
    	    pstmt.setString(3, action);
    	    pstmt.setString(4, location);
    	    pstmt.setString(5, world);
    	    pstmt.setString(6, result);
            pstmt.setTimestamp(7, Timestamp.valueOf(timestamp));
            pstmt.setString(8, serverName);
    	    pstmt.executeUpdate();
    	} catch(SQLException e) {
    	    e.printStackTrace();
    	   }
    	}
    }
    
    private boolean isDiscordLogEnable(String logName) {
    	if(!ActionUtils.customActions.isEmpty() && ActionUtils.customActions.containsKey(logName)) {
    		for(String server : plugin.getServerList()) {
    			if(plugin.getConfig().contains("enable-discord-custom-logs." + server + "." + logName)) {
    				return plugin.getConfig().getBoolean("enable-discord-custom-logs." + server + "." + logName);
    			}
    		}
    	}
		return plugin.getConfig().getBoolean("enable-discord-logs." + logName);
	}
    
    public List<String> getLogs(int x, int y, int z, String world, int limit) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        List<String> logs = new ArrayList<>();
	    String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.server-name") : "undefined";
        String sql = "SELECT * FROM watchlogs_logs WHERE location = ? AND world = ? AND(action = 'block-place' OR action = 'block-break' OR action = 'block-explosion')" + (serverName.equals("undefined") ? "" : " AND server = ?") + " ORDER BY id DESC LIMIT " + limit;

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, x + "/" + y + "/" + z);
            pstmt.setString(2, world);
            if(!serverName.equals("undefined")) {
            	pstmt.setString(3, serverName);
            }
            try(ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String pseudo = rs.getString("pseudo");
                    String action = rs.getString("action");
                    String location = rs.getString("location");
                    String worldName = rs.getString("world");
                    String result = rs.getString("result");
                    String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
                    logs.add(prefix + translateString("&cID &7" + id + "&c | " + pseudo + " : &7" + action + " at " + timestamp + "\n"
                    		+ prefix + "&cWorld Information: &7" + worldName + "(" + location + ") \n"
                    		+ prefix + "&cOther Information : &7" + result + "\n&7]---------------------------["));
                    
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    public List<String> getContainerLogs(int x, int y, int z, String world) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        List<String> logs = new ArrayList<>();
	    String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.server-name") : "undefined";
        String sql = "SELECT * FROM watchlogs_logs WHERE location = ? AND world = ? AND(action = 'container-transaction' OR action = 'container-open')" + (serverName.equals("undefined") ? "" :  "AND server = ?") + " ORDER BY id DESC";

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, x + "/" + y + "/" + z);
            pstmt.setString(2, world);
            if(!serverName.equals("undefined")) {
            	pstmt.setString(3, serverName);
            }
            try(ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String pseudo = rs.getString("pseudo");
                    String action = rs.getString("action");
                    String location = rs.getString("location");
                    String worldName = rs.getString("world");
                    String result = rs.getString("result");
                    String timestamp = String.valueOf(rs.getTimestamp("timestamp"));
                    logs.add(prefix + translateString("&cID &7" + id + "&c | " + pseudo + " : &7" + action + " at " + timestamp + "\n"
                    		+ prefix + "&cWorld Information: &7" + worldName + "(" + location + ") \n"
                    		+ prefix + "&cOther Information : &7" + result + "\n&7]---------------------------["));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    public String getLocationOfId(int id) {
        String sql = "SELECT location FROM watchlogs_logs WHERE id = ?";
        String location = null;

        try(PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try(ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    location = rs.getString("location");
                }
            }
        } catch(SQLException e) {
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
    	} catch(SQLException e) {
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
            } catch(SQLException e) {
                info.append("Connection Status: Disconnected(Exception: ").append(e.getMessage()).append(")\n");
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
            } catch(SQLException e) {
                info.append("Ping: N/A(Exception: ").append(e.getMessage()).append(")\n");
            }
        } else {
            info.append("Ping: N/A(Connection is null)\n");
        }

        return info.toString();
    }

    public int getLastLogId() {
        String sql = "SELECT MAX(id) AS max_id FROM watchlogs_logs";
        try(Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if(rs.next()) {
                return rs.getInt("max_id");
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String translateString(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}