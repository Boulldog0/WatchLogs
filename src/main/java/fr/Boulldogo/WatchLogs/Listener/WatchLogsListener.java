package fr.Boulldogo.WatchLogs.Listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Events.JsonDataExportEvent;
import fr.Boulldogo.WatchLogs.Events.JsonDataImportEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteLoginEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteLogoutEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteLogsRequestEvent;
import fr.Boulldogo.WatchLogs.Events.WebsiteRegisterEvent;

public class WatchLogsListener implements Listener {
	
	private final Main plugin;
	private final DatabaseManager databaseManager;
	
	public WatchLogsListener(Main plugin, DatabaseManager databaseManager) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
	}
	
	@EventHandler
	public void onJsonDataImport(JsonDataImportEvent e) {
		Player player = e.getPlayer();
		String fileName = e.getFileName();
		String idMap = e.getIdMap();
		
		if(isLogEnable("json-import")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "json-import", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Json Import of " + fileName + (e.isUsedOriginalIds() ? "Unavailable (Use original IDs)" : " ( Ids Between " + idMap + ")"));
		}
	}
	
	@EventHandler
	public void onJsonDataExport(JsonDataExportEvent e) {
		Player player = e.getPlayer();
		String fileName = e.getFileName();
		String settings = e.getExportSettings();
		
		if(isLogEnable("json-export")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "json-export", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Json Export in " + fileName + " with settings " + settings);
		}
	}
	
	@EventHandler
	public void onWebsiteLoginEvent(WebsiteLoginEvent e) {
		String username = e.getUsername();
		String ip = e.getAdress();
		
		if(isLogEnable("website-login")) {
			databaseManager.insertLog(username, "website-login", "Unknow (Non-ingame event)", "Unknow (Non-ingame event)", "Login with ip adress " + ip);
		}
	}
	
	@EventHandler
	public void onWebsiteLogoutEvent(WebsiteLogoutEvent e) {
		String username = e.getUsername();
		String ip = e.getAdress();
		
		if(isLogEnable("website-logout")) {
			databaseManager.insertLog(username, "website-logout", "Unknow (Non-ingame event)", "Unknow (Non-ingame event)", "Logout with ip adress " + ip);
		}
	}
	
	@EventHandler
	public void websiteLogRequestEvent(WebsiteLogsRequestEvent e) {
		String username = e.getUsername();
		String search = e.getSearch();
		String ip = e.getAdress();
		
		if(isLogEnable("website-logs-search")) {
			databaseManager.insertLog(username, "website-logs-search", "Unknow (Non-ingame event)", "Unknow (Non-ingame event)", "Search request : " + search +  " (Request sent with IP " + ip + ")");
		}
	}
	
	@EventHandler
	public void onWebsiteRegister(WebsiteRegisterEvent e) {
		String username = e.getUsername();
		String ip = e.getAdress();
		boolean isOp = e.isPlayerOp();
		
		if(isLogEnable("website-register")) {
			databaseManager.insertLog(username, "website-logs-search", "Unknow (Non-ingame event)", "Unknow (Non-ingame event)", "Register with ip adress " + ip + "  (Player is OP ? " + String.valueOf(isOp) + ")");
		}
	}

	
	public boolean isLogEnable(String logName) {
		return plugin.getConfig().getBoolean("enable-logs." + logName);
	}
}
