package fr.Boulldogo.WatchLogs.Utils;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;

public class MaterialUtils {
	
	private final WatchLogsPlugin plugin;
    
    public MaterialUtils(WatchLogsPlugin plugin) {
    	this.plugin = plugin;
    }
    
    public String getBlockName(Block block) {
        Material material = block.getType();
        return String.valueOf(material);
    }
    
    public String getItemName(ItemStack item) {
        Material material = item.getType();
        return String.valueOf(material);
    }

	public boolean isBlockActivated(String blockName) {
    	return !plugin.getConfig().getStringList("blacklist-blocks").contains(blockName);
    }

	public boolean isItemActivated(String itemsName) {
    	return !plugin.getConfig().getStringList("blacklist-items").contains(itemsName);
    }
}
