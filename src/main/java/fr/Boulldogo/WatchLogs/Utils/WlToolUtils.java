package fr.Boulldogo.WatchLogs.Utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import de.tr7zw.nbtapi.NBTItem;
import fr.Boulldogo.WatchLogs.WatchLogsPlugin;

public class WlToolUtils {
	
	private final WatchLogsPlugin plugin;
	private boolean usePersistentDataContainer;
	
	public WlToolUtils(WatchLogsPlugin plugin) {
		this.plugin = plugin;
        this.usePersistentDataContainer = isPersistentDataContainerAvailable();  
	}
	
    private boolean isPersistentDataContainerAvailable() {
    	return plugin.getSpigotVersionAsInt() >= 1130;
    }
	
    public boolean hasTag(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return false;
        }

        if(usePersistentDataContainer) {
            ItemMeta meta = item.getItemMeta();
            if(meta == null) {
                return false;
            }
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(new NamespacedKey(plugin, "wltool"), PersistentDataType.STRING);
        } else {
            return hasTagUsingNBT(item);
        }
    }

    public ItemStack setTagToItemStack(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return null;
        }

        if(usePersistentDataContainer) {
            ItemMeta meta = item.getItemMeta();
            if(meta == null) {
                return null;
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "wltool"), PersistentDataType.STRING, "tool");
            item.setItemMeta(meta);
            return item;
        } else {
            return setTagUsingNBT(item);
        }
    }
    
    private boolean hasTagUsingNBT(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return false;
        }

        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey("wltool");
    }

    private ItemStack setTagUsingNBT(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return null;
        }

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString("wltool", "tool");
        return nbtItem.getItem();
    }

}
