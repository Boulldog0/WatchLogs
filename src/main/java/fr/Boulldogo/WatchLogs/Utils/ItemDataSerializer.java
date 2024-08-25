package fr.Boulldogo.WatchLogs.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.tr7zw.nbtapi.NBTItem;
import fr.Boulldogo.WatchLogs.WatchLogsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ItemDataSerializer {

    private static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    private final boolean usePersistentDataContainer;
    private final WatchLogsPlugin plugin;

    public ItemDataSerializer(WatchLogsPlugin plugin) {
    	this.plugin = plugin;
        this.usePersistentDataContainer = isPersistentDataContainerAvailable();
    }

    private boolean isPersistentDataContainerAvailable() {
    	return plugin.getSpigotVersionAsInt() >= 1130;
    }

    public void serializeItemStack(ItemStack itemStack, Consumer<String> callback) {
    	new BukkitRunnable() {

			@Override
			public void run() {
		        ItemData itemData = new ItemData(itemStack);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(gson.toJson(itemData));
                    }
                }.runTask(plugin);
			}	
    	}.runTaskAsynchronously(plugin);
    }

    public ItemStack deserializeItemStack(String json) {
        ItemData itemData = gson.fromJson(json, ItemData.class);
        return itemData.toItemStack();
    }

    private class ItemData {
        private final String type;
        private final int amount;
        private final short durability;
        private final String displayName;
        private final String[] lore;
        private final Map<String, Integer> enchantments;
        private final Map<String, String> nbtTags;
        private final String ownerUUID;
        private final String ownerName;

        @SuppressWarnings("deprecation")
        public ItemData(ItemStack itemStack) {
            this.type = itemStack.getType().name();
            this.amount = itemStack.getAmount();
            this.durability = itemStack.getDurability();

            ItemMeta meta = itemStack.getItemMeta();
            if(meta != null) {
                this.displayName = meta.getDisplayName();
                this.lore = meta.getLore() != null ? meta.getLore().toArray(new String[0]) : new String[0];
                this.enchantments = new HashMap<>();
                for(Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    this.enchantments.put(entry.getKey().getName(), entry.getValue());
                }

                this.nbtTags = usePersistentDataContainer ? getNBTFromPersistentData(meta) : getNBTUsingNBTAPI(itemStack);

                if(meta instanceof SkullMeta) {
                    SkullMeta skullMeta =(SkullMeta) meta;
                    if(skullMeta.hasOwner()) {
                        this.ownerUUID = skullMeta.getOwningPlayer() != null ? skullMeta.getOwningPlayer().getUniqueId().toString() : null;
                        this.ownerName = skullMeta.getOwner();
                    } else {
                        this.ownerUUID = null;
                        this.ownerName = null;
                    }
                } else {
                    this.ownerUUID = null;
                    this.ownerName = null;
                }
            } else {
                this.displayName = null;
                this.lore = new String[0];
                this.enchantments = new HashMap<>();
                this.nbtTags = new HashMap<>();
                this.ownerUUID = null;
                this.ownerName = null;
            }
        }

        @SuppressWarnings("deprecation")
		public ItemStack toItemStack() {
            Material material = Material.getMaterial(type);
            if(material == null) {
                return new ItemStack(Material.AIR);
            }

            ItemStack itemStack = new ItemStack(material, amount, durability);
            ItemMeta meta = itemStack.getItemMeta();
            if(meta != null) {
                meta.setDisplayName(displayName);
                meta.setLore(java.util.Arrays.asList(lore));
                for(Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                    Enchantment enchantment = Enchantment.getByName(entry.getKey());
                    if(enchantment != null) {
                        meta.addEnchant(enchantment, entry.getValue(), true);
                    }
                }

                if(usePersistentDataContainer) {
                    setNBTToPersistentData(meta, nbtTags);
                } else {
                    setNBTUsingNBTAPI(itemStack, nbtTags);
                }

                if(meta instanceof SkullMeta) {
                    SkullMeta skullMeta =(SkullMeta) meta;
                    if(ownerUUID != null) {
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)));
                    } else if(ownerName != null) {
                        skullMeta.setOwner(ownerName);
                    }
                }

                itemStack.setItemMeta(meta);
            }

            return itemStack;
        }

        private Map<String, String> getNBTFromPersistentData(ItemMeta meta) {
            Map<String, String> nbtData = new HashMap<>();
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            for(NamespacedKey key : dataContainer.getKeys()) {
                String value = dataContainer.get(key, PersistentDataType.STRING);
                if(value != null) {
                    nbtData.put(key.toString(), value);
                }
            }
            return nbtData;
        }

        private Map<String, String> getNBTUsingNBTAPI(ItemStack itemStack) {
            Map<String, String> nbtData = new HashMap<>();
            NBTItem nbtItem = new NBTItem(itemStack);
            for(String key : nbtItem.getKeys()) {
                nbtData.put(key, nbtItem.getString(key));
            }
            return nbtData;
        }

        private void setNBTToPersistentData(ItemMeta meta, Map<String, String> nbtTags) {
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            for(Map.Entry<String, String> entry : nbtTags.entrySet()) {
                NamespacedKey key = NamespacedKey.fromString(entry.getKey());
                if(key != null) {
                    dataContainer.set(key, PersistentDataType.STRING, entry.getValue());
                }
            }
        }

        private void setNBTUsingNBTAPI(ItemStack itemStack, Map<String, String> nbtTags) {
            NBTItem nbtItem = new NBTItem(itemStack);
            for(Map.Entry<String, String> entry : nbtTags.entrySet()) {
                nbtItem.setString(entry.getKey(), entry.getValue());
            }
            nbtItem.applyNBT(itemStack); 
        }
    }
}
