package fr.Boulldogo.WatchLogs.Web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Events.WebsiteLoginEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteLogoutEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteLogsRequestEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteRegisterEvent;
import fr.Boulldogo.WatchLogs.Utils.ActionUtils;
import fr.Boulldogo.WatchLogs.Utils.PermissionChecker;
import fr.Boulldogo.WatchLogs.Utils.WebUtils;

import static spark.Spark.*;

public class WebServer {

    private final WatchLogsPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Algorithm algorithm;

    public WebServer(WatchLogsPlugin plugin, DatabaseManager databaseManager, String securityKey) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.algorithm = Algorithm.HMAC256(securityKey);
    }

    private String readHtmlFile(String fileName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public void start() {
        int port = plugin.getConfig().getInt("website.website_port");
        boolean logIp = plugin.getConfig().getBoolean("website.log-request-ips");
        
        port(port);
        staticFiles.location("/public");
        plugin.getLogger().info("[Web-Module] Web Server start on port " + port);

        get("/login", (req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("login.html");
            } catch (IOException e) {
                res.status(400);
                return "Internal server error (login.html not found !)";
            }
        });
        
        get("/account", (req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("account.html");
            } catch (IOException e) {
                res.status(400);
                return "Internal server error (account.html not found !)";
            }
        });

        post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if (plugin.getConfig().getBoolean("website.use-login-whitelist")) {
                if (!plugin.getConfig().getStringList("website-login-whitelist").contains(username)) {
                    res.status(405);
                    return "This account is not whitelist on the website !";
                }
            }

            if (databaseManager.authenticateUser(username, password)) {
                String token = JWT.create()
                        .withSubject(username)
                        .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                        .sign(algorithm);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String ip = logIp ? req.ip() : "Hide by administrator.";

                    WebsiteLoginEvent event = new WebsiteLoginEvent(username, ip);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                });
                  
                req.session(true);
                req.session().attribute("user", username);
                res.cookie("token", token, 24 * 60 * 60);
                res.redirect("/logs");
                return null;
            } else {
                res.status(401);
                return "Invalid credentials";
            }
        });

        before("/logs", (req, res) -> {
            String token = req.cookie("token");
            if (token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
            }
        });
        
        before("/account", (req, res) -> {
            String token = req.cookie("token");
            if (token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
            }
        });
        
        post("/account/delete_account", (req, res) -> {
            String token = req.cookie("token");
            String username = getUsernameFromToken(token);
            
            databaseManager.deleteUser(username);
            plugin.getLogger().info("[Web-Module] User " + username + " deleted their account!");
            return null;
        });
        
        post("/account/change_password", (req, res) -> {
            String currentPassword = req.queryParams("currentPassword");
            String newPassword = req.queryParams("newPassword");
            
            String token = req.cookie("token");
            String username = getUsernameFromToken(token);
            
            if(databaseManager.authenticateUser(username, currentPassword)) {
                databaseManager.changeUserPassword(username, newPassword);
                res.redirect("/login");
            } else {
                res.status(400);
                res.body("Current password is incorrect");
            }
            return null;
        });

        before("/logout", (req, res) -> {
            String token = req.cookie("token");
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                String ip = logIp ? req.ip() : "Hide by administrator.";
                
                WebsiteLogoutEvent event;
                if (token != null) {
                    res.removeCookie("token");
                    event = new WebsiteLogoutEvent(getUsernameFromToken(token), ip);
                } else {
                    event = new WebsiteLogoutEvent("Unknow", ip);
                }
                
                Bukkit.getServer().getPluginManager().callEvent(event);
            });

            res.redirect("/login");
            halt();
        });

        get("/logs", (req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("logs.html");
            } catch (IOException e) {
                res.status(500);
                return "Internal server error (logs.html not found !)";
            }
        });

        get("/logs/all", (req, res) -> {
            String token = req.cookie("token");
            String ip = logIp ? req.ip() : "Hide by administrator.";
        	String username = getUsernameFromToken(token);
        	
            Bukkit.getScheduler().runTask(plugin, () -> {
                WebsiteLogsRequestEvent event = new WebsiteLogsRequestEvent(username, ip, "All (default request)");
                Bukkit.getServer().getPluginManager().callEvent(event);
            });
            
            PermissionChecker permissionChecker = plugin.getPermissionChecker();
            boolean canViewLocation = permissionChecker.hasPermission(username, "watchlogs.website.view_location");
            
            List<String> logs = databaseManager.getAllLogs(true, canViewLocation);    	
            res.type("application/json");
            return new Gson().toJson(logs);
        });
        
        get("/logs/limit", (req, res) -> {
        	String limit = plugin.getConfig().getString("website.website-log-time-limit-showed");
        	return new Gson().toJson(limit);
        });
        
        get("/logs/multi_server_enable", (req, res) -> {
        	boolean enable = plugin.getConfig().getBoolean("multi-server.enable");
        	
            Map<String, Boolean> ena = new HashMap<>();
            ena.put("enable", enable);
        	return new Gson().toJson(enable);
        });
        
        get("/logs/server_list", (req, res) -> {
        	List<String> servers = new ArrayList<>();
        	servers.add("");
        	servers.addAll(databaseManager.getServerInNetwork());
        	return new Gson().toJson(servers);
        });

        get("/logs/search", (req, res) -> {
            String world = req.queryParams("world").equals("") ? "undefined" : req.queryParams("world");
            String player = req.queryParams("player");
            String xCoord = req.queryParams("xCoord");
            String yCoord = req.queryParams("yCoord");
            String zCoord = req.queryParams("zCoord");
            String action = req.queryParams("action").equals("") ? "undefined" : req.queryParams("action");
            String resultFilter = req.queryParams("resultFilter");
            String timeFilter = req.queryParams("timeFilter");
            boolean useTimestamp = !timeFilter.equals("undefined");
            String serverName = req.queryParams("server").equals("") ? "undefined" : req.queryParams("server");

            String location = (xCoord.equals("undefined") ? "%" : xCoord) + "/"
                    + (yCoord.equals("undefined") ? "%" : yCoord) + "/"
                    + (zCoord.equals("undefined") ? "%" : zCoord);

            String token = req.cookie("token");
            String ip = logIp ? req.ip() : "Hide by administrator.";
        	String username = getUsernameFromToken(token);
        	
            Bukkit.getScheduler().runTask(plugin, () -> {
                String search = "";
                
                if(!world.equals("undefined")) {
                	search = search + "World : " + world + " | ";
                }
                
                if(!player.equals("undefined")) {
                	search = search + "Player : " + player + " | ";
                }
                
                if(!xCoord.equals("undefined") && !yCoord.equals("undefined") && !zCoord.equals("undefined")) {
                	search = search + "Location : " + xCoord + "/" + yCoord + "/" + zCoord + " | ";
                }
                
                if(!action.equals("undefined")) {
                	search = search + "Action : " + action + " | ";
                }
                
                if(!resultFilter.equals("undefined")) {
                	search = search + "Result filter : " + resultFilter + " | ";
                }
                
                if(!timeFilter.equals("undefined")) {
                	search = search + "TimeFilter : " + player + " | ";
                }
                
                if(!serverName.equals("undefined")) {
                	search = search + "Server : " + serverName + " | ";
                }
                
                if(world.equals("undefined") && player.equals("undefined") && xCoord.equals("undefined") 
                	&& yCoord.equals("undefined") && zCoord.equals("undefined") && action.equals("undefined") 
                	&& resultFilter.equals("undefined") && timeFilter.equals("undefined")) {
                	search = "All (Search with no settings).";
                }
                
                WebsiteLogsRequestEvent event = new WebsiteLogsRequestEvent(username, ip, search);
                Bukkit.getServer().getPluginManager().callEvent(event);
            });
            
            PermissionChecker permissionChecker = plugin.getPermissionChecker();
            boolean canViewLocation = permissionChecker.hasPermission(username, "watchlogs.website.view_location");

            List<String> filteredLogs = databaseManager.getWebJsonLogs(world, player, location, action, resultFilter, timeFilter, useTimestamp, canViewLocation, serverName);

            res.type("application/json");
            return new Gson().toJson(filteredLogs);
        });
        
        before("/user_permissions", (req, res) -> {
            String token = req.cookie("token");
            if (token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
            }
        });
        
        get("/user_permissions", (req, res) -> {
            String searchPermission = "watchlogs.website.search_permission";
            String deletePermission = "watchlogs.website.delete_permission";
            String token = req.cookie("token");
            String username = getUsernameFromToken(token);
            
            PermissionChecker permissionChecker = plugin.getPermissionChecker();

            boolean canSearch = permissionChecker.hasPermission(username, searchPermission);
            boolean canDelete = permissionChecker.hasPermission(username, deletePermission);

            Map<String, Boolean> permissions = new HashMap<>();
            permissions.put("canSearch", canSearch);
            permissions.put("canDelete", canDelete);

            res.type("application/json");
            return new Gson().toJson(permissions);
        });

        get("/logs/actions", (req, res) -> {
            List<String> actions = new ArrayList<>();
            actions.add("");
            actions.addAll(ActionUtils.actions());
            res.type("application/json");
            return new Gson().toJson(actions);
        });

        get("/logs/worlds", (req, res) -> {
            List<String> worlds = new ArrayList<>();

            worlds.add("");
            for (World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }

            res.type("application/json");
            return new Gson().toJson(worlds);
        });

        get("/username", (req, res) -> {
            String token = req.cookie("token");
            if (token != null && isTokenValid(token)) {
                String username = getUsernameFromToken(token);
                res.type("application/json");
                return new Gson().toJson(username);
            } else {
                res.type("application/json");
                return new Gson().toJson("Unknow");
            }
        });

        get("/register", (req, res) -> {
            try {
                return readHtmlFile("register.html");
            } catch (IOException e) {
                res.status(500);
                return "Internal server error (register.html not found !)";
            }
        });

        post("/register", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String confirm_password = req.queryParams("confirm_pass");
            String code = req.queryParams("code");

            if (username == null || username.isEmpty() || password == null || password.isEmpty() || confirm_password == null || confirm_password.isEmpty() || code == null || code.isEmpty()) {
                res.status(401);
                return "You cannot send register sheet with empty parms !";
            }

            Player player = Bukkit.getPlayer(username);

            if (password.length() < plugin.getConfig().getInt("website.minimum_length_of_password")) {
                res.status(402);
                return "Password must have a minimum of " + plugin.getConfig().getInt("website.minimum_length_of_password") + " characters.</h1><a href=\"/register\">Back to register";
            }

            if (!password.equals(confirm_password)) {
                res.status(400);
                return "Confirmation password does not match the first password.";
            }

            if (player == null || !player.isOnline()) {
                res.status(400);
                return "User must be connected to the server.";
            }

            if (plugin.getConfig().getBoolean("website.use-register-whitelist")) {
                if (!plugin.getConfig().getStringList("website-register-whitelist").contains(username)) {
                    res.status(400);
                    return "This account is not whitelist.";
                }
            }

            WebUtils utils = plugin.getWebUtils();

            if (!utils.getPlayerCode(player).equals(code)) {
                res.status(400);
                return "Invalid Auth Code! Please type /wl website givecode to obtain a valid code.";
            }

            if (req.cookie("token") != null && isTokenValid(req.cookie("token"))) {
                String uname = req.session().attribute("user");
                if (uname != null || databaseManager.isUserRegistered(uname)) {
                    res.status(400);
                    return "You have already an account ! Please use /login.";
                }
            }

            if (databaseManager.isUserRegistered(username)) {
                res.status(400);
                return "User already exists.";
            }

            if (!plugin.getConfig().getBoolean("website.enable_register")) {
                res.status(400);
                return "Registration is disabled.";
            }

            if (plugin.getConfig().getInt("website.max_account") > 0) {
                if (databaseManager.getNumberOfAccounts() >= plugin.getConfig().getInt("website.max_account")) {
                    res.status(400);
                    return "Maximum allowed number of registrations reached.";
                }
            }

            if (plugin.getConfig().getBoolean("website.player_must_be_op_for_register")) {
                if (!player.isOp()) {
                    res.status(400);
                    return "OP status is required to register an account.";
                }
            }

            try {
            	
                Bukkit.getScheduler().runTask(plugin, () -> {
                	boolean isOp = player.isOp();
                    String ip = logIp ? req.ip() : "Hide by administrator.";
                    
                	WebsiteRegisterEvent event = new WebsiteRegisterEvent(username, ip, isOp);
                	Bukkit.getServer().getPluginManager().callEvent(event);
                });
            	
                databaseManager.createUser(username, confirm_password);
                plugin.getLogger().info("[Web-Module] New user " + username + " registered on the website.");
                res.redirect("/login");
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                res.status(400);
                return "Error when trying to create user. Please check the console for more informations.";
            }
        });

        notFound((req, res) -> {
            String token = req.cookie("token");
            if (token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
                return null;
            } else if (token != null && isTokenValid(token)) {
                res.redirect("/logs");
                return null;
            }
            return null;
        });
    }

    private String getUsernameFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            return jwt.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTokenValid(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            String username = jwt.getSubject();
            return username != null && databaseManager.isUserRegistered(username);
        } catch (Exception e) {
            return false;
        }
    }
}
