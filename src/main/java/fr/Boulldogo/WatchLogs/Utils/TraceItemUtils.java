package fr.Boulldogo.WatchLogs.Utils;

import java.sql.SQLException;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import de.tr7zw.nbtapi.NBTItem;
import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;

public class TraceItemUtils {

    private final Main plugin;
    private final DatabaseManager dbManager;
    private final boolean usePersistentDataContainer; 

    public TraceItemUtils(Main plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.usePersistentDataContainer = isPersistentDataContainerAvailable();  
    }

    private boolean isPersistentDataContainerAvailable() {
    	return plugin.getSpigotVersionAsInt() >= 1130;
    }

    public String createUUIDForItem() {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder uuid = new StringBuilder();
        Random random = new Random();

        for(int i = 0; i < 16; i++) {
            int index = random.nextInt(characters.length());
            uuid.append(characters.charAt(index));
        }

        if(dbManager.UUIDExists(uuid.toString())) {
            return createUUIDForItem();
        }

        return uuid.toString();
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
            return container.has(new NamespacedKey(plugin, "wlti"), PersistentDataType.STRING);
        } else {
            return hasTagUsingNBT(item);
        }
    }

    public String getWltiTagValue(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return null;
        }

        if(usePersistentDataContainer) {
            ItemMeta meta = item.getItemMeta();
            if(meta == null) {
                return null;
            }
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.get(new NamespacedKey(plugin, "wlti"), PersistentDataType.STRING);
        } else {
            return getTagValueUsingNBT(item);
        }
    }

    public ItemStack setTagToItemStack(ItemStack item, String tag) {
        if(item == null || item.getType() == Material.AIR) {
            return null;
        }

        if(usePersistentDataContainer) {
            ItemMeta meta = item.getItemMeta();
            if(meta == null) {
                return null;
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "wlti"), PersistentDataType.STRING, tag);
            item.setItemMeta(meta);
            return item;
        } else {
            return setTagUsingNBT(item, tag);
        }
    }

    public ItemStack removeTagOnItemStack(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return item;
        }

        if(usePersistentDataContainer) {
            String UUID = getWltiTagValue(item);

            ItemMeta meta = item.getItemMeta();
            if(meta == null) {
                return item;
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.remove(new NamespacedKey(plugin, "wlti"));

            try {
                dbManager.deleteUUID(UUID);
            } catch(SQLException e) {
                e.printStackTrace();
            }

            item.setItemMeta(meta);
            return item;
        } else {
            return removeTagUsingNBT(item); 
        }
    }

    private boolean hasTagUsingNBT(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return false;
        }

        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey("wlti");
    }

    private String getTagValueUsingNBT(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return null;
        }

        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.getString("wlti");
    }

    private ItemStack setTagUsingNBT(ItemStack item, String tag) {
        if(item == null || item.getType() == Material.AIR) {
            return null;
        }

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString("wlti", tag);
        return nbtItem.getItem();
    }

    private ItemStack removeTagUsingNBT(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return item;
        }

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.removeKey("wlti");

        String UUID = getTagValueUsingNBT(item);
        try {
            dbManager.deleteUUID(UUID);
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return nbtItem.getItem();
    }
}
