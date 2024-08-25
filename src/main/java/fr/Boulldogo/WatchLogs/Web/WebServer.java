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
import java.util.concurrent.CompletableFuture;
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
    private final Map<String, String> pending2FACodeUsers = new HashMap<>();
    private final Map<String, Integer> connectionTry = new HashMap<>();
    private final List<String> bannedIps = new ArrayList<>();

    public WebServer(WatchLogsPlugin plugin, DatabaseManager databaseManager, String securityKey) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.algorithm = Algorithm.HMAC256(securityKey);
    }

    private String readHtmlFile(String fileName) throws IOException {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
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

        get("/login",(req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("login.html");
            } catch(IOException e) {
                res.status(400);
                return "Internal server error(login.html not found !)";
            }
        });
        
        get("/account",(req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("account.html");
            } catch(IOException e) {
                res.status(400);
                return "Internal server error(account.html not found !)";
            }
        });
        
        get("/2fa",(req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("2fa.html");
            } catch(IOException e) {
                res.status(400);
                return "Internal server error(2fa.html not found !)";
            }
        });
        
        get("/banned",(req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("banned.html");
            } catch(IOException e) {
                res.status(400);
                return "Internal server error(banned.html not found !)";
            }
        });
        
        before("/banned", (req, res) -> {
        	if(!bannedIps.contains(req.ip())) {
                res.redirect("/login");
                halt();
        	}
        }) ;
        
        before("/login",(req, res) -> {
            if(bannedIps.contains(req.ip())) {
                res.redirect("/banned");
                halt();
            }
        });

        post("/login",(req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if(plugin.getConfig().getBoolean("website.use-login-whitelist") &&
                !plugin.getConfig().getStringList("website-login-whitelist").contains(username)) {
                res.status(405);
                return "This account is not whitelisted on the website!";
            }

            if(databaseManager.authenticateUser(username, password)) {
                connectionTry.remove(req.ip());

                if(plugin.getConfig().getBoolean("website.enable_2fa")) {
                    pending2FACodeUsers.put(req.ip(), username);
                    res.redirect("/2fa");
                    return null;
                } else {
                    String token = JWT.create()
                            .withSubject(username)
                            .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                            .sign(algorithm);
                    
                    Bukkit.getScheduler().runTask(plugin,() -> {
                        String ip = logIp ? req.ip() : "Hide by administrator.";
                        WebsiteLoginEvent event = new WebsiteLoginEvent(username, ip, false);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                    });
                      
                    req.session(true);
                    req.session().attribute("user", username);
                    res.cookie("token", token, 24 * 60 * 60);
                    res.redirect("/logs");
                    return null;
                }
            } else {
                connectionTry.compute(req.ip(),(ip, tries) -> {
                    if(tries == null) {
                        return 1;
                    } else if(tries >= 4) {
                        bannedIps.add(ip);
                        plugin.getLogger().warning("IP " + ip + " was automatically banned from the web panel. Reason: 5 failed login attempts.");
                        res.status(410);
                        res.redirect("/banned");
                    } else {
                        return tries + 1;
                    }
					return tries;
                });

                res.status(401);
                return "Invalid credentials";
            }
        });
        
        before("/2fa",(req, res) -> {
            if(bannedIps.contains(req.ip())) {
                res.redirect("/banned");
                halt();
            }
        });
        
        post("/2fa",(req, res) -> {
            String enteredCode = req.queryParams("2faCode");
            String playerName = pending2FACodeUsers.get(req.ip());
            
            if(plugin.getA2FUtils().isA2FCodeValid(playerName, enteredCode)) {
                connectionTry.remove(req.ip()); 
                
                String token = JWT.create()
                        .withSubject(playerName)
                        .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                        .sign(algorithm);
                
                Bukkit.getScheduler().runTask(plugin,() -> {
                    String ip = logIp ? req.ip() : "Hide by administrator.";
                    WebsiteLoginEvent event = new WebsiteLoginEvent(playerName, ip, true);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                });
                  
                req.session(true);
                req.session().attribute("user", playerName);
                res.cookie("token", token, 24 * 60 * 60);
                pending2FACodeUsers.remove(getIPForPlayerName(playerName));
                plugin.getA2FUtils().removePlayerA2FCode(playerName);
                res.redirect("/logs");
            } else {
                connectionTry.compute(req.ip(),(ip, tries) -> {
                    if(tries == null) {
                        return 1;
                    } else if(tries >= 4) {
                        bannedIps.add(ip);
                        plugin.getLogger().warning("IP " + ip + " was automatically banned from the web panel. Reason: 5 failed 2FA attempts.");
                        res.status(410);
                        res.redirect("/banned");
                    } else {
                        return tries + 1;
                    }
					return tries;
                });

                res.status(401);
                return "Invalid 2FA code.";
            }
            return null;
        });

        before("/logs",(req, res) -> {
            String token = req.cookie("token");
            if(token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
            }
        	if(!bannedIps.isEmpty() && bannedIps.contains(req.ip()))  {
                res.redirect("/banned");
                halt();
        	}
        });
        
        before("/account",(req, res) -> {
            String token = req.cookie("token");
            if(token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
            }
        	if(!bannedIps.isEmpty() && bannedIps.contains(req.ip()))  {
                res.redirect("/banned");
                halt();
        	}
        });
        
        post("/account/delete_account",(req, res) -> {
            String token = req.cookie("token");
            String username = getUsernameFromToken(token);
            
            databaseManager.deleteUser(username);
            plugin.getLogger().info("[Web-Module] User " + username + " deleted their account!");
            return null;
        });
        
        post("/banned", (req, res) -> {
            res.status(410);
            res.body("IP Address banned from the web panel!");
            return null;
        });
        
        post("/account/change_password",(req, res) -> {
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

        before("/logout",(req, res) -> {
            String token = req.cookie("token");
            
            Bukkit.getScheduler().runTask(plugin,() -> {
                String ip = logIp ? req.ip() : "Hide by administrator.";
                
                WebsiteLogoutEvent event;
                if(token != null) {
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

        get("/logs",(req, res) -> {
            res.type("text/html");
            try {
                return readHtmlFile("logs.html");
            } catch(IOException e) {
                res.status(500);
                return "Internal server error(logs.html not found !)";
            }
        });

        get("/logs/all",(req, res) -> {
            String token = req.cookie("token");
            String ip = logIp ? req.ip() : "Hide by administrator.";
        	String username = getUsernameFromToken(token);
        	
            Bukkit.getScheduler().runTask(plugin,() -> {
                WebsiteLogsRequestEvent event = new WebsiteLogsRequestEvent(username, ip, "All(default request)");
                Bukkit.getServer().getPluginManager().callEvent(event);
            });
            
            PermissionChecker permissionChecker = plugin.getPermissionChecker();
            boolean canViewLocation = permissionChecker.hasPermission(username, "watchlogs.website.view_location");
            
            CompletableFuture<List<String>> futureLogs = getAllLogsAsync(canViewLocation);
            List<String> allLogs = futureLogs.join(); 

            res.type("application/json");
            return new Gson().toJson(allLogs);
        });
        
        get("/logs/limit",(req, res) -> {
        	String limit = plugin.getConfig().getString("website.website-log-time-limit-showed");
        	return new Gson().toJson(limit);
        });
        
        get("/logs/multi_server_enable",(req, res) -> {
        	boolean enable = plugin.getConfig().getBoolean("multi-server.enable");
        	
            Map<String, Boolean> ena = new HashMap<>();
            ena.put("enable", enable);
        	return new Gson().toJson(enable);
        });
        
        get("/logs/server_list",(req, res) -> {
        	List<String> servers = new ArrayList<>();
        	servers.add("");
        	servers.addAll(databaseManager.getServerInNetwork());
        	return new Gson().toJson(servers);
        });

        get("/logs/search",(req, res) -> {
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

            String location =(xCoord.equals("undefined") ? "%" : xCoord) + "/"
                    +(yCoord.equals("undefined") ? "%" : yCoord) + "/"
                    +(zCoord.equals("undefined") ? "%" : zCoord);

            String token = req.cookie("token");
            String ip = logIp ? req.ip() : "Hide by administrator.";
        	String username = getUsernameFromToken(token);
        	
            Bukkit.getScheduler().runTask(plugin,() -> {
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
                	search = "All(Search with no settings).";
                }
                
                WebsiteLogsRequestEvent event = new WebsiteLogsRequestEvent(username, ip, search);
                Bukkit.getServer().getPluginManager().callEvent(event);
            });
            
            PermissionChecker permissionChecker = plugin.getPermissionChecker();
            boolean canViewLocation = permissionChecker.hasPermission(username, "watchlogs.website.view_location");
            
            CompletableFuture<List<String>> futureLogs = getLogsAsync(world, player, location, action, resultFilter, timeFilter, useTimestamp, canViewLocation, serverName);
            List<String> filteredLogs = futureLogs.join(); 

            res.type("application/json");
            return new Gson().toJson(filteredLogs);
        });
        
        before("/user_permissions",(req, res) -> {
            String token = req.cookie("token");
            if(token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
            }
        });
        
        get("/user_permissions",(req, res) -> {
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

        get("/logs/actions",(req, res) -> {
            List<String> actions = new ArrayList<>();
            actions.add("");
            actions.addAll(ActionUtils.actions());
            res.type("application/json");
            return new Gson().toJson(actions);
        });

        get("/logs/worlds",(req, res) -> {
            List<String> worlds = new ArrayList<>();

            worlds.add("");
            for(World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }

            res.type("application/json");
            return new Gson().toJson(worlds);
        });

        get("/username",(req, res) -> {
            String token = req.cookie("token");
            if(token != null && isTokenValid(token)) {
                String username = getUsernameFromToken(token);
                res.type("application/json");
                return new Gson().toJson(username);
            } else {
                res.type("application/json");
                return new Gson().toJson("Unknow");
            }
        });

        get("/register",(req, res) -> {
            try {
                return readHtmlFile("register.html");
            } catch(IOException e) {
                res.status(500);
                return "Internal server error(register.html not found !)";
            }
        });

        post("/register",(req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String confirm_password = req.queryParams("confirm_pass");
            String code = req.queryParams("code");

            if(username == null || username.isEmpty() || password == null || password.isEmpty() || confirm_password == null || confirm_password.isEmpty() || code == null || code.isEmpty()) {
                res.status(401);
                return "You cannot send register sheet with empty parms !";
            }

            Player player = Bukkit.getPlayer(username);

            if(password.length() < plugin.getConfig().getInt("website.minimum_length_of_password")) {
                res.status(402);
                return "Password must have a minimum of " + plugin.getConfig().getInt("website.minimum_length_of_password") + " characters.</h1><a href=\"/register\">Back to register";
            }

            if(!password.equals(confirm_password)) {
                res.status(400);
                return "Confirmation password does not match the first password.";
            }

            if(player == null || !player.isOnline()) {
                res.status(400);
                return "User must be connected to the server.";
            }

            if(plugin.getConfig().getBoolean("website.use-register-whitelist")) {
                if(!plugin.getConfig().getStringList("website-register-whitelist").contains(username)) {
                    res.status(400);
                    return "This account is not whitelist.";
                }
            }

            WebUtils utils = plugin.getWebUtils();

            if(!utils.getPlayerCode(player).equals(code)) {
                res.status(400);
                return "Invalid Auth Code! Please type /wl website givecode to obtain a valid code.";
            }

            if(req.cookie("token") != null && isTokenValid(req.cookie("token"))) {
                String uname = req.session().attribute("user");
                if(uname != null || databaseManager.isUserRegistered(uname)) {
                    res.status(400);
                    return "You have already an account ! Please use /login.";
                }
            }

            if(databaseManager.isUserRegistered(username)) {
                res.status(400);
                return "User already exists.";
            }

            if(!plugin.getConfig().getBoolean("website.enable_register")) {
                res.status(400);
                return "Registration is disabled.";
            }

            if(plugin.getConfig().getInt("website.max_account") > 0) {
                if(databaseManager.getNumberOfAccounts() >= plugin.getConfig().getInt("website.max_account")) {
                    res.status(400);
                    return "Maximum allowed number of registrations reached.";
                }
            }

            if(plugin.getConfig().getBoolean("website.player_must_be_op_for_register")) {
                if(!player.isOp()) {
                    res.status(400);
                    return "OP status is required to register an account.";
                }
            }

            try {
                Bukkit.getScheduler().runTask(plugin,() -> {
                	boolean isOp = player.isOp();
                    String ip = logIp ? req.ip() : "Hide by administrator.";
                    
                	WebsiteRegisterEvent event = new WebsiteRegisterEvent(username, ip, isOp);
                	Bukkit.getServer().getPluginManager().callEvent(event);
                });
            	
                databaseManager.createUser(username, confirm_password);
                plugin.getLogger().info("[Web-Module] New user " + username + " registered on the website.");
                res.redirect("/login");
                return null;
            } catch(SQLException e) {
                e.printStackTrace();
                res.status(400);
                return "Error when trying to create user. Please check the console for more informations.";
            }
        });

        notFound((req, res) -> {
            String token = req.cookie("token");
            if(token == null || !isTokenValid(token)) {
                res.redirect("/login");
                halt();
                return null;
            } else if(token != null && isTokenValid(token)) {
                res.redirect("/logs");
                return null;
            }
            return null;
        });
    }
    
    public List<String> getBannedIpsList() {
		return bannedIps;
    	
    }
    
    public void removeBannedIp(String IP) {
    	if(bannedIps.isEmpty()) return;
    	bannedIps.remove(IP);
    }
    
    private String getIPForPlayerName(String playerName) {
        return pending2FACodeUsers.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(playerName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String getUsernameFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            return jwt.getSubject();
        } catch(Exception e) {
            return null;
        }
    }

    private boolean isTokenValid(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            String username = jwt.getSubject();
            return username != null && databaseManager.isUserRegistered(username);
        } catch(Exception e) {
            return false;
        }
    }
    
    public CompletableFuture<List<String>> getLogsAsync(String world, String player, String location, String action, String resultFilter, String timeFilter, boolean useTimestamp, boolean canViewLocation, String serverName) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        databaseManager.getWebJsonLogs(world, player, location, action, resultFilter, timeFilter, useTimestamp, canViewLocation, serverName, future::complete);
        return future;
    }
    
    public CompletableFuture<List<String>> getAllLogsAsync(boolean canViewLocation) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        try {
			databaseManager.getAllLogs(true, canViewLocation, future::complete);
		} catch (SQLException e) {
			e.printStackTrace();
		}
        return future;
    }
}
