package fr.Boulldogo.WatchLogs.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemDataSerializer {

    private static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

    public String serializeItemStack(ItemStack itemStack) {
        ItemData itemData = new ItemData(itemStack);
        return gson.toJson(itemData);
    }

    public ItemStack deserializeItemStack(String json) {
        ItemData itemData = gson.fromJson(json, ItemData.class);
        return itemData.toItemStack();
    }

    private static class ItemData {
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
                this.nbtTags = new HashMap<>();
                PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
                for(NamespacedKey key : dataContainer.getKeys()) {
                    String value = dataContainer.get(key, PersistentDataType.STRING);
                    if(value != null) {
                        this.nbtTags.put(key.toString(), value);
                    }
                }
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

        public ItemStack toItemStack() {
            Material material = Material.getMaterial(type);
            if(material == null) {
                return new ItemStack(Material.AIR);
            }

            @SuppressWarnings("deprecation")
            ItemStack itemStack = new ItemStack(material, amount, durability);
            ItemMeta meta = itemStack.getItemMeta();
            if(meta != null) {
                meta.setDisplayName(displayName);
                meta.setLore(java.util.Arrays.asList(lore));
                for(Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                    @SuppressWarnings("deprecation")
                    Enchantment enchantment = Enchantment.getByName(entry.getKey());
                    if(enchantment != null) {
                        meta.addEnchant(enchantment, entry.getValue(), true);
                    }
                }
                PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
                for(Map.Entry<String, String> entry : nbtTags.entrySet()) {
                    NamespacedKey key = NamespacedKey.fromString(entry.getKey());
                    if(key != null) {
                        dataContainer.set(key, PersistentDataType.STRING, entry.getValue());
                    }
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
    }
}
