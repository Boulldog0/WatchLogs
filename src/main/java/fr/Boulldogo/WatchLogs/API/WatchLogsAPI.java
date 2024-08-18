package fr.Boulldogo.WatchLogs.API;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Utils.ActionUtils;
import fr.Boulldogo.WatchLogs.Utils.ItemDataSerializer;

public class WatchLogsAPI {
	
	private final WatchLogsPlugin plugin;
	private final DatabaseManager dbManager;

	public static WatchLogsPlugin getWatchLogsPlugin() {
		return WatchLogsPlugin.getPlugin();
	}
	
	public WatchLogsAPI(WatchLogsPlugin plugin) {
		this.plugin = plugin;
		this.dbManager = plugin.databaseManager;
	}
	
	public boolean customActionsAreEnable() {
		return plugin.getConfig().getBoolean("allow-external-actions");
	}
	
	public void addCustomAction(Plugin requestPlugin, String actionName, String formattedName) {
		if(plugin.getConfig().getBoolean("allow-external-actions")) {
			if(ActionUtils.customActions.containsKey(actionName)) {
				plugin.getLogger().warning("A plugin tryed to add custom action, but an action with this name is already loaded !");
				return;
			}
			ActionUtils.customActions.put(actionName, formattedName);
			
			if(!plugin.getConfig().contains("plugins-integrations." + requestPlugin.getDescription().getName() + "." + actionName)) {
				plugin.getConfig().set("plugins-integrations." + requestPlugin.getDescription().getName() + "." + actionName, true);
				plugin.getLogger().info("Adding new action with plugin integration for plugin " + requestPlugin.getDescription().getName() + " : " + actionName);
				plugin.saveConfig();
			}
		} else {
			plugin.getLogger().warning("This server does not allow the external actions. The action " + actionName + " was not registed.");
			return;
		}
	}
	
	public void addCustomLog(String actionName, Player player, String location, String worldName, String result) {
		if(plugin.getConfig().getBoolean("allow-external-actions")) {
			if(!ActionUtils.actions().contains(actionName)) {
				plugin.getLogger().warning("A custom action tried to add logs into the WatchLogs system, but this action is not register in the actions list !");
				return;
			}
			dbManager.insertLog(player.getName(), actionName, location, worldName, result);
		} else {
			plugin.getLogger().warning("This server does not allow the external actions. The action " + actionName + " was not registed.");
			return;
		}
	}
	
	public void addItemReborn(ItemStack item) {
		if(plugin.getConfig().getBoolean("allow-external-actions")) {	
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	ItemDataSerializer dataSerializer = new ItemDataSerializer(plugin);
	        	dbManager.addItemEntry(dataSerializer.serializeItemStack(item), false, -1);
	        }
		} 
	}
	
	public boolean customActionIsEnable(String pluginName, String actionName) {
		return plugin.getConfig().contains("plugins-integrations." + pluginName + "." + actionName)
			&& plugin.getConfig().getBoolean("plugins-integrations." + pluginName + "." + actionName);
	}
}
