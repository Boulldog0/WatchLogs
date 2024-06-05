package fr.Boulldogo.WatchLogs.Utils;

import com.google.gson.Gson;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class ItemDataSerializer {

    private static final Gson gson = new Gson();

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

        @SuppressWarnings("deprecation")
		public ItemData(ItemStack itemStack) {
            this.type = itemStack.getType().name();
            this.amount = itemStack.getAmount();
            this.durability = itemStack.getDurability() <= 0 ? 1 : itemStack.getDurability();
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                this.displayName = meta.getDisplayName();
                this.lore = meta.getLore() != null ? meta.getLore().toArray(new String[0]) : new String[0];
                this.enchantments = new HashMap<>();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    this.enchantments.put(entry.getKey().getName(), entry.getValue());
                }
            } else {
                this.displayName = null;
                this.lore = new String[0];
                this.enchantments = new HashMap<>();
            }
        }

        public ItemStack toItemStack() {
            Material material = Material.getMaterial(type);
            if (material == null) {
                return new ItemStack(Material.AIR);
            }

            @SuppressWarnings("deprecation")
			ItemStack itemStack = new ItemStack(material, amount, durability);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                meta.setLore(java.util.Arrays.asList(lore));
                for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                    @SuppressWarnings("deprecation")
					Enchantment enchantment = Enchantment.getByName(entry.getKey());
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, entry.getValue(), true);
                    }
                }
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
    }
}
