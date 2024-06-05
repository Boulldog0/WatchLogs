package fr.Boulldogo.WatchLogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
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
import fr.Boulldogo.WatchLogs.Discord.SetupDiscordBot;
import fr.Boulldogo.WatchLogs.Listener.MinecraftListener;
import fr.Boulldogo.WatchLogs.Listener.PlayerListener;
import fr.Boulldogo.WatchLogs.Listener.ToolListener;
import fr.Boulldogo.WatchLogs.Utils.*;

public class Main extends JavaPlugin {
	
	public boolean EnableWorldGuard = true;
    private Map<Player, PlayerSession> playerSessions;
	boolean useMySQLDatabase = this.getConfig().getBoolean("use-mysql");
	public static String version = "1.0.0";
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
	
	public void onEnable() {
        Server server = getServer();
        Pattern pattern = Pattern.compile("(^[^\\-]*)");
        Matcher matcher = pattern.matcher(server.getBukkitVersion());
        if (!matcher.find()) {
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
		
	    playerSessions = new HashMap<>();
	    
	    metrics = new Metrics(this, 22048);
	    
	    this.materialUtils = new MaterialUtils(this);
	    this.dataSerializer = new ItemDataSerializer();
	    		
		
		this.getLogger().info("                                         \r\n"
				+ " __    __      _       _       __               \r\n"
				+ "/ / /\\ \\ \\__ _| |_ ___| |__   / /  ___   __ _ ___ \r\n"
				+ "\\ \\/  \\/ / _` | __/ __| '_ \\ / /  / _ \\ / _` / __|\r\n"
				+ " \\  /\\  / (_| | || (__| | | / /__| (_) | (_| \\__ \\\r\n"
				+ "  \\/  \\/ \\__,_|\\__\\___|_| |_\\____/\\___/ \\__, |___/\r\n"
				+ "                                        |___/     ");
		this.getLogger().info("WatchLogs running on Spigot/Bukkit version " + bukkitVersion);
		if(this.getConfig().getBoolean("discord.discord-module-enabled")) {
	        discordBot = new SetupDiscordBot(this, databaseManager);
	        try {
	            discordBot.startBot();
	        } catch (LoginException e) {
	            getLogger().severe("Failed to login to Discord: " + e.getMessage());
	        }
		} else {
			this.getLogger().warning("Discord Bot Module are disabled. No bot started with plugin.");
		}
		
		String lang = this.getConfig().getString("lang_file");
		
		if(!lang.equals("US") && !lang.equals("FR")) {
			this.getLogger().warning("Invalid lang file set in config.yml. Available : US, FR. Plugin has modified invalid lang value to default lang value (US)");
			this.getConfig().set("lang_file", "US");
			this.saveConfig();
		}
		
		if(lang.equals("US")) {
	        saveDefaultLangFile("en_US.yml");

	        loadLangFile("en_US.yml");
		} else {
	        saveDefaultLangFile("fr_FR.yml");

	        loadLangFile("fr_FR.yml");
		}
		
		if(this.getConfig().getBoolean("log-in-file")) {
		    Calendar calendar = Calendar.getInstance();
			String todayFileName = calendar.get(Calendar.DAY_OF_MONTH) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
			
			File folder = new File(this.getDataFolder(), "logs");
			File todayFile = new File(folder, todayFileName + ".yml");
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
		}
		
        GithubVersion versionChecker = new GithubVersion(this);
        versionChecker.checkForUpdates();
		
		if(useMySQLDatabase) {
		   String connectAdress = this.getConfig().getString("mysql.ip");
		   int port = this.getConfig().getInt("mysql.port");
		   String username = this.getConfig().getString("mysql.username");
		   String password = this.getConfig().getString("mysql.password");
		   String database = this.getConfig().getString("mysql.database");
		
		   String url = "jdbc:mysql://" + connectAdress + ":" + port + "/" + database;
		
		    this.databaseManager = new DatabaseManager(url, username, password, this.getLogger(), useMySQLDatabase, this, discordBot, dataSerializer);
	        databaseManager.connect();
		} else {
	        String sqliteFile = this.getConfig().getString("sqlite.file");
	        String url = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/" + sqliteFile;
			String username = this.getConfig().getString("mysql.username");
			String password = this.getConfig().getString("mysql.password");
		    this.databaseManager = new DatabaseManager(url, username, password, this.getLogger(), useMySQLDatabase, this, discordBot, dataSerializer);
	        databaseManager.connect();
	    }
		
		this.getLogger().info("Spigot project : ");
		this.getLogger().info("Plugin WatchLogs v1.0.0 by Boulldogo loaded correctly !");
		
		
		this.getServer().getPluginManager().registerEvents(new MinecraftListener(this, databaseManager, materialUtils, dataSerializer), this);
		this.getServer().getPluginManager().registerEvents(new ToolListener(this, databaseManager), this);
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
		this.getCommand("watchlogs").setExecutor(new MainCommand(this, databaseManager));
	}
	
	public void onDisable() {	
		this.getLogger().info("                                         \r\n"
				+ " __    __      _       _       __               \r\n"
				+ "/ / /\\ \\ \\__ _| |_ ___| |__   / /  ___   __ _ ___ \r\n"
				+ "\\ \\/  \\/ / _` | __/ __| '_ \\ / /  / _ \\ / _` / __|\r\n"
				+ " \\  /\\  / (_| | || (__| | | / /__| (_) | (_| \\__ \\\r\n"
				+ "  \\/  \\/ \\__,_|\\__\\___|_| |_\\____/\\___/ \\__, |___/\r\n"
				+ "                                        |___/     ");
		this.getLogger().info("Plugin WatchLogs v1.0.0 by Boulldogo unloaded correctly !");
		
        if (discordBot != null && discordBot.getJDA() != null) {
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
	
	public boolean isVersionLessThanOrEqual(String versionToCompare) {
	    String bukkitVersion = Bukkit.getServer().getBukkitVersion();
	    Pattern pattern = Pattern.compile("(^[^\\-]*)");
	    Matcher matcher = pattern.matcher(bukkitVersion);
	    if (matcher.find()) {
	        String[] versionParts = matcher.group(1).split("\\.");
	        String[] compareParts = versionToCompare.split("\\.");
	        for (int i = 0; i < Math.min(versionParts.length, compareParts.length); i++) {
	            int currentVersionPart = Integer.parseInt(versionParts[i]);
	            int currentComparePart = Integer.parseInt(compareParts[i]);
	            if (currentVersionPart < currentComparePart) {
	                return true;
	            } else if (currentVersionPart > currentComparePart) {
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
        if (!langFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource(fileName); OutputStream out = new FileOutputStream(langFile)) {
                if (in == null) {
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
        if (langFile.exists()) {
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
	    Calendar calendar = Calendar.getInstance();
	    String todayFileName = calendar.get(Calendar.DAY_OF_MONTH) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR) + ".yml";

	    calendar.add(Calendar.DAY_OF_MONTH, -1);
	    String yesterdayFileName = calendar.get(Calendar.DAY_OF_MONTH) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR) + ".yml";

	    File folder = new File(this.getDataFolder(), "logs");
	    File todayFile = new File(folder, todayFileName);
	    File yesterdayFile = new File(folder, yesterdayFileName);
	    File ancientFolder = new File(folder, "archives");

	    if (!ancientFolder.exists()) {
	        ancientFolder.mkdirs();
	    }

	    if (yesterdayFile.exists()) {
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
	            if (yesterdayFile.delete()) {
	                getLogger().info("Logs of day correctly saved and moove in: " + yesterdayFileName);
	            } else {
	                getLogger().warning("Failed to save log file: " + yesterdayFileName);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	    if (!todayFile.exists()) {
	        try {
	            if (todayFile.createNewFile()) {
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
	    if (!folder.exists() || !folder.isDirectory()) {
	        return;
	    }

	    File[] files = folder.listFiles();
	    if (files == null) {
	        return;
	    }

	    long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);

	    if(this.getConfig().getBoolean("delete-old-archives")) {
		    for (File file : files) {
		        if (file.isFile() && file.lastModified() < cutoffTime) {
		            if (file.delete()) {
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
