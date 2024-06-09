package fr.Boulldogo.WatchLogs.Listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Events.JsonDataExportEvent;
import fr.Boulldogo.WatchLogs.Events.JsonDataImportEvent;

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

	
	public boolean isLogEnable(String logName) {
		return plugin.getConfig().getBoolean("enable-logs." + logName);
	}
}
