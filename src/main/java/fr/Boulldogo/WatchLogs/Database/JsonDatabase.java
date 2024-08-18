package fr.Boulldogo.WatchLogs.Database;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Events.JsonDataExportEvent;
import fr.Boulldogo.WatchLogs.Events.JsonDataImportEvent;

public class JsonDatabase {
	
	private final WatchLogsPlugin plugin;
	private final DatabaseManager databaseManager;
	
	public JsonDatabase(WatchLogsPlugin plugin, DatabaseManager databaseManager) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
	}
	
	public boolean exportDatabaseDatas(int lines, List<String> jsonExport, String settings, Player player) {
		File folder = new File(plugin.getDataFolder(), "export");
		if(!folder.exists()) {
			folder.mkdir();
		}
		
	    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
	    String dateString = dateFormat.format(Calendar.getInstance().getTime());
	    
	    File file = new File(folder, "export-" + dateString + ".json");
	    
	    JSONArray jsonArray = new JSONArray();
	    for(int i = 0; i < lines && i < jsonExport.size(); i++) {
	        jsonArray.put(new JSONObject(jsonExport.get(i)));
	    }
	    
	    try(FileWriter fileWriter = new FileWriter(file)) {
	        fileWriter.write(jsonArray.toString(4)); 
	        JsonDataExportEvent exportEvent = new JsonDataExportEvent(player, "export-" + dateString + ".json", settings, lines == jsonExport.size() ? "all" : String.valueOf(lines));
	        Bukkit.getServer().getPluginManager().callEvent(exportEvent);
	        return true;
	    } catch(IOException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public boolean importDatabaseDatas(Player player, String fileName, boolean useOriginalId) {
	    File folder = new File(plugin.getDataFolder(), "import");
	    if(!folder.exists()) {
	        folder.mkdir();
	        return false;
	    }

	    File file = new File(folder, fileName + ".json");

	    if(!file.exists()) {
	        return false;
	    }

	    try(FileReader reader = new FileReader(file)) {
	        StringBuilder jsonContent = new StringBuilder();
	        int i;
	        while((i = reader.read()) != -1) {
	            jsonContent.append((char) i);
	        }

	        JSONArray jsonArray = new JSONArray(jsonContent.toString());

	        for(int j = 0; j < jsonArray.length(); j++) {
	            JSONObject jsonObject = jsonArray.getJSONObject(j);
	            int id = jsonObject.getInt("id");
	            String pseudo = jsonObject.getString("pseudo");
	            String action = jsonObject.getString("action");
	            String location = jsonObject.getString("location");
	            String world = jsonObject.getString("world");
	            String result = jsonObject.getString("result");
	            String timestamp = jsonObject.getString("timestamp");
	            String serverName = jsonObject.getString("server");

	            databaseManager.insertJsonLog(useOriginalId ? id : databaseManager.getLastLogId(), pseudo, action, location, world, result + "(Data imported by Json import)", timestamp, useOriginalId, serverName);
	        }
	        
	        String idMap = useOriginalId ? "Unavailable(use original IDs)" : databaseManager.getLastLogId() + " -> " +(databaseManager.getLastLogId() + jsonArray.length());
	        
	        JsonDataImportEvent importEvent = new JsonDataImportEvent(player, fileName + ".json", useOriginalId, idMap);
	        Bukkit.getServer().getPluginManager().callEvent(importEvent);

	        return true;
	    } catch(IOException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
}
