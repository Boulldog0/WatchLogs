package fr.Boulldogo.WatchLogs.Utils;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import fr.Boulldogo.WatchLogs.Main;

public class MaterialUtils {
    
    public MaterialUtils(Main plugin) {
    }
    
    public String getBlockName(Block block) {
        Material material = block.getType();
        return String.valueOf(material);
    }
    
    public String getItemName(ItemStack item) {
        Material material = item.getType();
        return String.valueOf(material);
    }
}
