package fr.Boulldogo.WatchLogs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import fr.Boulldogo.WatchLogs.Commands.MainCommand;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Database.JsonDatabase;
import fr.Boulldogo.WatchLogs.Discord.SetupDiscordBot;
import fr.Boulldogo.WatchLogs.Listener.MinecraftListener;
import fr.Boulldogo.WatchLogs.Listener.PlayerListener;
import fr.Boulldogo.WatchLogs.Listener.ToolListener;
import fr.Boulldogo.WatchLogs.Listener.WatchLogsListener;
import fr.Boulldogo.WatchLogs.Utils.*;
import fr.Boulldogo.WatchLogs.Web.WebServer;

public class WatchLogsPlugin extends JavaPlugin {
	
	public boolean EnableWorldGuard = true;
    private Map<Player, PlayerSession> playerSessions;
	boolean useMySQLDatabase = this.getConfig().getBoolean("use-mysql");
	public String version = this.getDescription().getVersion();
	private boolean isUpToDate = true;
	public DatabaseManager databaseManager;
    private SetupDiscordBot discordBot;
    private YamlConfiguration lang;
	private String bukkitVersion;
    private MaterialUtils materialUtils;
    private ItemDataSerializer dataSerializer;
    private JsonDatabase jsonDatabase;
    private WebUtils webUtils;
    private Random random = new Random();
    private PermissionChecker permissionChecker; 
    private TraceItemUtils traceItemUtils;
    private static WatchLogsPlugin plugin;
	private final List<String> servers = new ArrayList<>();
	private A2FUtils A2fUtils;
	private WebServer webServer;
	public List<String> linkedPlugins;
    
	public void onEnable() {
		this.getLogger().info("==============[Enable Start of WatchLogs]==============");
		long ms = System.currentTimeMillis();
        Server mcServer = getServer();
        Pattern pattern = Pattern.compile("(^[^\\-]*)");
        Matcher matcher = pattern.matcher(mcServer.getBukkitVersion());
        if(!matcher.find()) {
            this.getLogger().severe("Could not find Bukkit version... Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        bukkitVersion = matcher.group(1);
        
        File file = new File(this.getDataFolder(), "config.yml");
        if(!file.exists()) {	
        	this.getLogger().info("Creating new config.yml ! The server will restart to apply the changes. Consider configuring the configuration");
    		saveDefaultConfig();
    		Bukkit.getServer().spigot().restart();
        }
        
        permissionChecker = new PermissionChecker(Bukkit.getPluginManager().isPluginEnabled("LuckPerms"));
        if(!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
        	this.getLogger().warning("LuckPerms not found. It is recommended for Website Permissions ( Without it, the permissions works only if player is online ).");
        	this.getLogger().warning("Download LuckPerms here : https://luckperms.net");
        }
		
	    playerSessions = new HashMap<>();
	    
	    new Metrics(this, 22048);
	    
	    this.webUtils = new WebUtils();
	    
	    this.materialUtils = new MaterialUtils(this);
	    this.dataSerializer = new ItemDataSerializer(this);
	    this.A2fUtils = new A2FUtils(this);
	    WatchLogsPlugin.plugin = this;
	    
	    YamlUpdater updater = new YamlUpdater(this);
		
		String lang = this.getConfig().getString("lang_file");
		
		if(!lang.equals("US") && !lang.equals("FR")) {
			this.getLogger().warning("Invalid lang file set in config.yml. Available : US, FR. Plugin has modified invalid lang value to default lang value (US)");
			this.getConfig().set("lang_file", "US");
			this.saveConfig();
		}
		
		if(lang.equals("US")) {
	        saveDefaultLangFile("en_US.yml");

	        loadLangFile("en_US.yml");
		    String[] fileToUpdate = {"config.yml", "en_US.yml"};
		    updater.updateYamlFiles(fileToUpdate);
		} else {
	        saveDefaultLangFile("fr_FR.yml");

	        loadLangFile("fr_FR.yml");
		    String[] fileToUpdate = {"config.yml", "fr_FR.yml"};
		    updater.updateYamlFiles(fileToUpdate);
		}
	    		
		this.getLogger().info("                                         \r\n"
				+ " __    __      _       _       __               \r\n"
				+ "/ / /\\ \\ \\__ _| |_ ___| |__   / /  ___   __ _ ___ \r\n"
				+ "\\ \\/  \\/ / _` | __/ __| '_ \\ / /  / _ \\ / _` / __|\r\n"
				+ " \\  /\\  / (_| | || (__| | | / /__| (_) | (_| \\__ \\\r\n"
				+ "  \\/  \\/ \\__,_|\\__\\___|_| |_\\____/\\___/ \\__, |___/\r\n"
				+ "                                        |___/     ");
		this.getLogger().info("WatchLogs running on Spigot/Bukkit version " + bukkitVersion);
		if(useMySQLDatabase) {
			   String connectAdress = this.getConfig().getString("mysql.ip");
			   int port = this.getConfig().getInt("mysql.port");
			   String username = this.getConfig().getString("mysql.username");
			   String password = this.getConfig().getString("mysql.password");
			   String database = this.getConfig().getString("mysql.database");
			
			   String url = "jdbc:mysql://" + connectAdress + ":" + port + "/" + database + "?useSSL=false";
			
			    this.databaseManager = new DatabaseManager(url, username, password, this.getLogger(), useMySQLDatabase, this, dataSerializer);
		        databaseManager.connect();
			} else {
		        String sqliteFile = this.getConfig().getString("sqlite.file");
		        String url = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/" + sqliteFile;
				String username = this.getConfig().getString("mysql.username");
				String password = this.getConfig().getString("mysql.password");
			    this.databaseManager = new DatabaseManager(url, username, password, this.getLogger(), useMySQLDatabase, this, dataSerializer);
		        databaseManager.connect();
		    }

		if (this.getConfig().getBoolean("discord.discord-module-enabled")) {
		    discordBot = new SetupDiscordBot(plugin, databaseManager);

		    new BukkitRunnable() {
		        @Override
		        public void run() {
		            try {
		                discordBot.startBot();
		                getLogger().info("Discord bot started successfully.");
		            } catch (LoginException e) {
		                getLogger().severe("Failed to login to Discord: " + e.getMessage());
		            }
		        }
		    }.runTaskAsynchronously(this);
		} else {
		    this.getLogger().warning("[Discord-Module] Discord Bot Module is disabled. No bot started with the plugin.");
		}
		
		if (this.getConfig().getBoolean("website.enable_website_module")) {
			
			File secretFile = new File(getDataFolder(), "web_secret.yml");
			
			if(!secretFile.exists()) {
				try {
					secretFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			YamlConfiguration secret = YamlConfiguration.loadConfiguration(secretFile);
			
			if(!secret.contains("secret_key")) {
		        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789*$ù^è-_-!:;/.,§$&€";
		        StringBuilder builder = new StringBuilder(48);

		        for (int i = 0; i < 48; i++) {
		            int rdm = random.nextInt(characters.length());
		            builder.append(characters.charAt(rdm));
		        }
		        secret.set("secret_key", builder.toString());
				try {
					secret.save(secretFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				getLogger().warning("[Web-Module] New authentification security key created with success !");
			}
			
			String sKey = secret.getString("secret_key");
			
			this.webServer = new WebServer(this, databaseManager, sKey);
			
			webServer.start();
		} else {
		    getLogger().warning("[Web-Module] Web module is disabled. No web server started.");
		}
		
		File importFolder = new File(this.getDataFolder(), "import");
		
		if(!importFolder.exists()) {
			importFolder.mkdir();
		}
		
		new BukkitRunnable() {

			@Override
			public void run() {
		        GithubVersion versionChecker = new GithubVersion(plugin);
		        versionChecker.checkForUpdates();
			}
		}.runTaskAsynchronously(this);
		
	    this.jsonDatabase = new JsonDatabase(this, databaseManager);
	    this.traceItemUtils = new TraceItemUtils(this, databaseManager);
		
		this.getLogger().info("Spigot project : https://www.spigotmc.org/resources/⚙%EF%B8%8F-watchlogs-⚙%EF%B8%8F-ultimate-all-in-one-log-solution-1-7-1-20-6.117128/");
		this.getLogger().info("Plugin WatchLogs v" + version + " by Boulldogo loaded correctly !");
		
		
		this.getServer().getPluginManager().registerEvents(new MinecraftListener(this, databaseManager, materialUtils, dataSerializer), this);
		this.getServer().getPluginManager().registerEvents(new ToolListener(this, databaseManager), this);
		this.getServer().getPluginManager().registerEvents(new WatchLogsListener(this, databaseManager), this);
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
		this.getCommand("watchlogs").setExecutor(new MainCommand(this, databaseManager, jsonDatabase));
		
		long finalTime = System.currentTimeMillis() - ms;
		this.getLogger().info("==================[Enable WatchLogs finished in " + finalTime + "ms]==================");
		return;
	}
	
	public void onDisable() {	
		this.getLogger().info("                                         \r\n"
				+ " __    __      _       _       __               \r\n"
				+ "/ / /\\ \\ \\__ _| |_ ___| |__   / /  ___   __ _ ___ \r\n"
				+ "\\ \\/  \\/ / _` | __/ __| '_ \\ / /  / _ \\ / _` / __|\r\n"
				+ " \\  /\\  / (_| | || (__| | | / /__| (_) | (_| \\__ \\\r\n"
				+ "  \\/  \\/ \\__,_|\\__\\___|_| |_\\____/\\___/ \\__, |___/\r\n"
				+ "                                        |___/     ");
		String version = this.getDescription().getVersion();
		this.getLogger().info("Plugin WatchLogs v" + version + " by Boulldogo unloaded correctly !");
		
        if(discordBot != null && discordBot.getJDA() != null) {
            discordBot.getJDA().shutdown();
            getLogger().info("Discord bot shut down.");
        }
        return;
	}
	
	public void processPluginRegistry(String pluginName) {
		if(this.linkedPlugins.contains(pluginName)) return;
		this.linkedPlugins.add(pluginName);
	}
	
	public List<String> getLinkedPlugins() {
		return linkedPlugins;
	}
	
	public String getVersion() {
		return version;
	}
	
	public int getSpigotVersionAsInt() {
	    String versionOnly = Bukkit.getBukkitVersion().split("-")[0];
	    
	    String[] versionParts = versionOnly.split("\\.");

	    if(versionParts.length == 2) {
	        versionOnly += ".0";
	    }

	    String versionAsString = versionOnly.replace(".", "");
	    int versionAsInt;
	    try {
	        versionAsInt = Integer.parseInt(versionAsString);
	    } catch (NumberFormatException e) {
	        versionAsInt = 0;
	    }

	    return versionAsInt;
	}


	public void setUpToDate(boolean isUpToDate) {
		this.isUpToDate = isUpToDate;
	}
	
	public boolean isUpToDate() {
		return this.isUpToDate;
	}
	
	public List<String> getServerList() {
		return servers;
	}
	
	public void addServer(String server) {
		servers.add(server);
	}
	
	public static WatchLogsPlugin getPlugin() {
		return plugin;
	}
	
	public PermissionChecker getPermissionChecker() {
		return permissionChecker;
	}
	
	public WebUtils getWebUtils() {
		return webUtils;
	}
	
	public SetupDiscordBot getDiscordBot() {
		return discordBot;
	}
	
	public TraceItemUtils getTraceItemUtils() {
		return traceItemUtils;
	}
	
	public A2FUtils getA2FUtils() {
		return A2fUtils;
	}
	
	public WebServer getWebServer() {
		return webServer;
	}
	
	public boolean isVersionLessThanOrEqual(String versionToCompare) {
	    String bukkitVersion = Bukkit.getServer().getBukkitVersion();
	    Pattern pattern = Pattern.compile("(^[^\\-]*)");
	    Matcher matcher = pattern.matcher(bukkitVersion);
	    if(matcher.find()) {
	        String[] versionParts = matcher.group(1).split("\\.");
	        String[] compareParts = versionToCompare.split("\\.");
	        for (int i = 0; i < Math.min(versionParts.length, compareParts.length); i++) {
	            int currentVersionPart = Integer.parseInt(versionParts[i]);
	            int currentComparePart = Integer.parseInt(compareParts[i]);
	            if(currentVersionPart < currentComparePart) {
	                return true;
	            } else if(currentVersionPart > currentComparePart) {
	                return false;
	            }
	        }
	        return true;
	    } else {
	        return false;
	    }
	}
	
	private void saveDefaultLangFile(String fileName) {
        File langFile = new File(getDataFolder(), fileName);
        if(!langFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource(fileName); OutputStream out = new FileOutputStream(langFile)) {
                if(in == null) {
                    getLogger().severe("Could not find default language file in resources: " + fileName);
                    return;
                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                getLogger().info("Default language file " + fileName + " has been saved.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	private void loadLangFile(String fileName) {
        File langFile = new File(getDataFolder(), fileName);
        if(langFile.exists()) {
            YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
            lang = langConfig;
        } else {
            getLogger().severe("Language file not found: " + fileName);
        }
    }
    
    public YamlConfiguration getLang() {
    	return lang;
    }
	
    public PlayerSession getPlayerSession(Player player) {
        return playerSessions.computeIfAbsent(player, k -> new PlayerSession());
    }

    public void removePlayerSession(Player player) {
    	if(playerSessions.containsKey(player)) {
            playerSessions.remove(player);
    	}
    }
	
	public void runFileVerification() {
		new BukkitRunnable() {
			
			public void run() {
				Calendar calendar = Calendar.getInstance();
				if(calendar.get(Calendar.HOUR) == 0 && calendar.get(Calendar.MINUTE) == 0) {
					runDeleteDatabase();
				}
			}
		}.runTaskTimerAsynchronously(plugin, 0, 1200L);
	}
	
	private void runDeleteDatabase() {
		int daysToKeepDb = this.getConfig().getInt("database-log-delete");

	    if(this.getConfig().getBoolean("delete-database-log")) {
	         databaseManager.clearOldData(daysToKeepDb);
	    }
	}
}
