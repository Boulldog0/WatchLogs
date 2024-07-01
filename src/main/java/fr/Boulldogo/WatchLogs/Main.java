package fr.Boulldogo.WatchLogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

public class Main extends JavaPlugin {
	
	public boolean EnableWorldGuard = true;
    private Map<Player, PlayerSession> playerSessions;
	boolean useMySQLDatabase = this.getConfig().getBoolean("use-mysql");
	public static String version = "1.2.2";
	private boolean isUpToDate = true;
	public DatabaseManager databaseManager;
    private SetupDiscordBot discordBot;
    @SuppressWarnings("unused")
	private Metrics metrics;
    private YamlConfiguration lang;
    @SuppressWarnings("unused")
	private String bukkitVersion;
    private MaterialUtils materialUtils;
    private ItemDataSerializer dataSerializer;
    private JsonDatabase jsonDatabase;
    private WebUtils webUtils;
    private Random random = new Random();
    private PermissionChecker permissionChecker; 
    
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
	    
	    metrics = new Metrics(this, 22048);
	    
	    this.webUtils = new WebUtils();
	    
	    this.materialUtils = new MaterialUtils(this);
	    this.dataSerializer = new ItemDataSerializer();
	    
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
			
			   String url = "jdbc:mysql://" + connectAdress + ":" + port + "/" + database;
			
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
		
		if(this.getConfig().getBoolean("discord.discord-module-enabled")) {
	        discordBot = new SetupDiscordBot(this, databaseManager);
	        try {
	            discordBot.startBot();
	        } catch (LoginException e) {
	            getLogger().severe("Failed to login to Discord: " + e.getMessage());
	        }
		} else {
			this.getLogger().warning("[Discord-Module] Discord Bot Module are disabled. No bot started with plugin.");
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
			
			WebServer webServer = new WebServer(this, databaseManager, sKey);
			
			webServer.start();
		} else {
		    getLogger().warning("[Web-Module] Web module is disabled. No web server started.");
		}
		
		if(this.getConfig().getBoolean("log-in-file")) {
            SimpleDateFormat todayFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			
			File folder = new File(this.getDataFolder(), "logs");
			File todayFile = new File(folder, todayFileName.toString() + ".yml");
			File ancientFolder = new File(folder, "archives");
			if(!folder.exists()) {
				folder.mkdirs();
			}
			if(!ancientFolder.exists()) {
				ancientFolder.mkdirs();
			}
			if(!todayFile.exists()) {
				try {
					todayFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			runSaveLogFile();
		}
		
		File importFolder = new File(this.getDataFolder(), "import");
		
		if(!importFolder.exists()) {
			importFolder.mkdir();
		}
		
        GithubVersion versionChecker = new GithubVersion(this);
        versionChecker.checkForUpdates();
	    this.jsonDatabase = new JsonDatabase(this, databaseManager);
		
		this.getLogger().info("Spigot project : https://www.spigotmc.org/resources/⚙%EF%B8%8F-watchlogs-⚙%EF%B8%8F-ultimate-all-in-one-log-solution-1-7-1-20-6.117128/");
		this.getLogger().info("Plugin WatchLogs v1.2.2 by Boulldogo loaded correctly !");
		
		
		this.getServer().getPluginManager().registerEvents(new MinecraftListener(this, databaseManager, materialUtils, dataSerializer), this);
		this.getServer().getPluginManager().registerEvents(new ToolListener(this, databaseManager), this);
		this.getServer().getPluginManager().registerEvents(new WatchLogsListener(this, databaseManager), this);
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
		this.getCommand("watchlogs").setExecutor(new MainCommand(this, databaseManager, jsonDatabase));
		
		long finalTime = System.currentTimeMillis() - ms;
		this.getLogger().info("==================[Enable WatchLogs finished in " + finalTime + "ms]==================");
	}
	
	public void onDisable() {	
		this.getLogger().info("                                         \r\n"
				+ " __    __      _       _       __               \r\n"
				+ "/ / /\\ \\ \\__ _| |_ ___| |__   / /  ___   __ _ ___ \r\n"
				+ "\\ \\/  \\/ / _` | __/ __| '_ \\ / /  / _ \\ / _` / __|\r\n"
				+ " \\  /\\  / (_| | || (__| | | / /__| (_) | (_| \\__ \\\r\n"
				+ "  \\/  \\/ \\__,_|\\__\\___|_| |_\\____/\\___/ \\__, |___/\r\n"
				+ "                                        |___/     ");
		this.getLogger().info("Plugin WatchLogs v1.2.0 by Boulldogo unloaded correctly !");
		
        if(discordBot != null && discordBot.getJDA() != null) {
            discordBot.getJDA().shutdown();
            getLogger().info("Discord bot shut down.");
        }
	}
	
	public void setUpToDate(boolean isUpToDate) {
		this.isUpToDate = isUpToDate;
	}
	
	public boolean isUpToDate() {
		return this.isUpToDate;
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
	
	public void runSaveLogFile() {
		new BukkitRunnable() {
			
			@Override
			public void run() {
		        if(getConfig().getBoolean("log-in-file")) {
		            SimpleDateFormat todayFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
		            File folder = new File(getDataFolder(), "logs");
		            File todayFile = new File(folder, todayFileName.toString() + ".yml");

		            if(!folder.exists()) {
		                folder.mkdirs();
		            }

		            YamlConfiguration config;
		            if(todayFile.exists()) {
		                config = YamlConfiguration.loadConfiguration(todayFile);
		            } else {
		                config = new YamlConfiguration();
		            }

		            try {
		                config.save(todayFile);
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
		        }
			}
		}.runTaskTimer(this, 0, 1200L);
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
        playerSessions.remove(player);
    }
	
	public void runFileVerification() {
		new BukkitRunnable() {
			
			public void run() {
				Calendar calendar = Calendar.getInstance();
				if(calendar.get(Calendar.HOUR) == 0 && calendar.get(Calendar.MINUTE) == 0) {
					runSaveFile();
					runDeleteFile();
				}
			}
		}.runTaskTimer(this, 0, 1200);
	}
	
	public void runSaveFile() {
        SimpleDateFormat todayFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        
        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date previousDay = calendar.getTime();
        SimpleDateFormat yesFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String yesterdayFileName = yesFileName.format(previousDay).toString();

	    File folder = new File(this.getDataFolder(), "logs");
	    File todayFile = new File(folder, todayFileName.toString());
	    File yesterdayFile = new File(folder, yesterdayFileName);
	    File ancientFolder = new File(folder, "archives");

	    if(!ancientFolder.exists()) {
	        ancientFolder.mkdirs();
	    }

	    if(yesterdayFile.exists()) {
	        String zipFileName = yesterdayFileName.replace(".yml", ".zip");
	        File zipFile = new File(ancientFolder, zipFileName);
	        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
	             FileInputStream fis = new FileInputStream(yesterdayFile)) {
	            zos.putNextEntry(new ZipEntry(yesterdayFileName));
	            byte[] buffer = new byte[1024];
	            int length;
	            while ((length = fis.read(buffer)) > 0) {
	                zos.write(buffer, 0, length);
	            }
	            zos.closeEntry();
	            if(yesterdayFile.delete()) {
	                getLogger().info("Logs of day correctly saved and moove in: " + yesterdayFileName);
	            } else {
	                getLogger().warning("Failed to save log file: " + yesterdayFileName);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	    if(!todayFile.exists()) {
	        try {
	            if(todayFile.createNewFile()) {
	                getLogger().info("Created new day log file: " + todayFileName);
	            } else {
	                getLogger().warning("Failed to create new day log file: " + todayFileName);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	public void runDeleteFile() {
		int daysToKeep = this.getConfig().getInt("yml-log-file-archive-delete");
		int daysToKeepDb = this.getConfig().getInt("database-log-delete");
	    File folder = new File(this.getDataFolder(), "logs/archives");
	    if(!folder.exists() || !folder.isDirectory()) {
	        return;
	    }

	    File[] files = folder.listFiles();
	    if(files == null) {
	        return;
	    }

	    long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);

	    if(this.getConfig().getBoolean("delete-old-archives")) {
		    for (File file : files) {
		        if(file.isFile() && file.lastModified() < cutoffTime) {
		            if(file.delete()) {
		                getLogger().info("Deleted old archive: " + file.getName());
		            } else {
		                getLogger().warning("Failed to delete old archive: " + file.getName());
		            }
		        }
		    }
	    }
	    
	    if(this.getConfig().getBoolean("delete-database-log")) {
	         databaseManager.clearOldData(daysToKeepDb);
	    }
	}
}
