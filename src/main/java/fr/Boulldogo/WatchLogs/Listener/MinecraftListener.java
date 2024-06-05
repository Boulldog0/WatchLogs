package fr.Boulldogo.WatchLogs.Listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Utils.DatabaseManager;
import fr.Boulldogo.WatchLogs.Utils.ItemDataSerializer;
import fr.Boulldogo.WatchLogs.Utils.MaterialUtils;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class MinecraftListener implements Listener {
	
	public final Main plugin;
	public DatabaseManager databaseManager;
	public MaterialUtils materialUtils;
	public ItemDataSerializer dataSerializer;
	
	public MinecraftListener(Main plugin, DatabaseManager databaseManager, MaterialUtils materialUtils, ItemDataSerializer dataSerializer) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
		this.materialUtils = materialUtils;
		this.dataSerializer = dataSerializer;
	}
	
	@EventHandler
	public void playerLoginEvent(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		if(isLogEnable("join")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "join", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Join server");
		}
	}
	
	@EventHandler
	public void playerLogoutEvent(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		if(isLogEnable("leave")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "leave", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Leave server");
		}
	}
	
	@EventHandler
	public void playerTeleportEvent(PlayerTeleportEvent e) {
		if(e.isCancelled()) return;
		Player player = e.getPlayer();
		String eventName = e.getEventName();
		
		if(eventName.equals("PlayerJoinEvent")) return; 
		
		if(isLogEnable("teleport")) {
			Location location = e.getTo();
			databaseManager.insertLog(player.getName(), "teleport", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Teleportation type: " + e.getCause().toString());
		}
	}
	
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String blockName = materialUtils.getBlockName(block);
        int data = block.getData();
        if (isLogEnable("block-place") && !databaseManager.isToolEnabled(player.getName())) {
            Location location = block.getLocation();
            databaseManager.insertLog(player.getName(), "block-place", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Block Type: " + blockName + (data != 0 ? ":" +  data : ""));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String blockName = materialUtils.getBlockName(block);
        int data = block.getData();
        if (isLogEnable("block-break")) {
            Location location = block.getLocation();
            databaseManager.insertLog(player.getName(), "block-break", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Block Type: " + blockName + (data != 0 ? ":" +  data : ""));
        }
    }
	
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
	    if(e.isCancelled()) return;
	    Inventory inventory = e.getInventory();
	    Player player = (Player) e.getPlayer();
	    if(isContainer(inventory) && !databaseManager.isToolEnabled(player.getName())) {
	        if(isLogEnable("container-open")) {
	            Location location = e.getInventory().getLocation();
	            String inventoryType = getContainerName(inventory.getType());
	            databaseManager.insertLog(player.getName(), "container-open", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Container: " + inventoryType);
	        }
	    }
	}
	
	@EventHandler
	public void onBlockExplosion(BlockExplodeEvent e) {
        if (e.isCancelled()) return;
        Block block = e.getBlock();
        String blockName = materialUtils.getBlockName(block);
        int data = block.getData();
        if (isLogEnable("block-explosion")) {
            Location location = block.getLocation();
            databaseManager.insertLog("Explosion", "block-explosion", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), block.getWorld().getName(), "Block Type: " + blockName + (data != 0 ? ":" +  data : ""));
        }
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
	    if(event.isCancelled()) return;
	    if(event.getWhoClicked() instanceof Player) {
	        Player player = (Player) event.getWhoClicked();
	        Inventory clickedInventory = event.getClickedInventory();
	        Inventory topInventory = event.getView().getTopInventory();
	        if(clickedInventory != null && isContainer(topInventory)) {
	            InventoryAction action = event.getAction();
	            ItemStack currentItem = event.getCurrentItem();
	            Location location = event.getInventory().getLocation();
	            String containerName = getContainerName(topInventory.getType());

	            switch (action) {
	                case MOVE_TO_OTHER_INVENTORY:
	                case PICKUP_ONE:
	                case PICKUP_SOME:
	                case PICKUP_HALF:
	                case PICKUP_ALL:
	                case PLACE_ONE:
	                case PLACE_SOME:
	                case PLACE_ALL:
	                case SWAP_WITH_CURSOR:
	                case HOTBAR_SWAP:
	                case HOTBAR_MOVE_AND_READD:
	                    logTransfer(player, currentItem, clickedInventory, topInventory, location, containerName);
	                    break;
	                default:
	                    break;
	            }
	        }
	    }
	}

	private void logTransfer(Player player, ItemStack item, Inventory fromInventory, Inventory toInventory, Location location, String containerName) {
	    if(isLogEnable("container-transaction")) {
	        String id = materialUtils.getItemName(item);
	        int data = getItemData(item);
	        int amount = item.getAmount();
	        String action = (toInventory.equals(player.getInventory())) ? "added" : "removed";
	        String sign = (action.equals("added")) ? ChatColor.GREEN + "+" : ChatColor.RED + "-";
	        databaseManager.insertLog(player.getName(), "container-transaction", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), containerName + ", " + id + (data != 0 ? ":" +  data : "") + " (" + sign + ChatColor.GRAY + " x" + ChatColor.YELLOW + amount + ChatColor.GRAY + ")");
	        
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	databaseManager.addItemEntry(databaseManager.getLastLogId(), dataSerializer.serializeItemStack(item), false, -1);
	        }
	    }
	}

	private String getContainerName(InventoryType type) {
	    switch (type) {
	        case CHEST:
	            return "Chest";
	        case DISPENSER:
	            return "Dispenser";
	        case DROPPER:
	            return "Dropper";
	        case FURNACE:
	            return "Furnace";
	        case HOPPER:
	            return "Hopper";
	        case SHULKER_BOX:
	            return "Shulker Box";
	        case BARREL:
	        	return "Barrel";
	        default:
	            return "No-Vanilla Container";
	    }
	}
    
    @EventHandler
    public void ItemDropEvent(PlayerDropItemEvent e) {
		if(e.isCancelled()) return;
		Player player = e.getPlayer();
		if(isLogEnable("item-drop")) {
			Location location = e.getItemDrop().getLocation();
			String id = materialUtils.getItemName(e.getItemDrop().getItemStack());
			int data = getItemData(e.getItemDrop().getItemStack());
			databaseManager.insertLog(player.getName(), "item-drop", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id + (data != 0 ? ":" +  data : ""));
			
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	databaseManager.addItemEntry(databaseManager.getLastLogId(), dataSerializer.serializeItemStack(e.getItemDrop().getItemStack()), false, -1);
	        }
		}	
    }
    
    @EventHandler
    public void ItemPickupEvent(PlayerPickupItemEvent e) {
		if(e.isCancelled()) return;
		Player player = e.getPlayer();
		if(isLogEnable("item-pickup")) {
			Location location = e.getItem().getLocation();
			String id = materialUtils.getItemName(e.getItem().getItemStack());
			int data = getItemData(e.getItem().getItemStack());
			databaseManager.insertLog(player.getName(), "item-pickup", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id + (data != 0 ? ":" +  data : ""));
			
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	databaseManager.addItemEntry(databaseManager.getLastLogId(), dataSerializer.serializeItemStack(e.getItem().getItemStack()), false, -1);
	        }
		}
    }
    
    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent e) {
		Player player = e.getPlayer();
		if(isLogEnable("item-break")) {
			Location location = e.getPlayer().getLocation();
			String id = materialUtils.getItemName(e.getBrokenItem());
			int data = getItemData(e.getBrokenItem());
			databaseManager.insertLog(player.getName(), "item-break", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id + (data != 0 ? ":" +  data : ""));
			
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	databaseManager.addItemEntry(databaseManager.getLastLogId(), dataSerializer.serializeItemStack(e.getBrokenItem()), false, -1);
	        }
		}	
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		if(isLogEnable("player-death")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "player-death", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Death type: " + e.getDeathMessage());
		}	
		
		if(isLogEnable("player-death-loot")) {
			int deathId = databaseManager.getLastDeathId() + 1;
			for (int i = 0; i < e.getDrops().size(); i++) {
		        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
		        	databaseManager.addItemEntry(databaseManager.getLastLogId(), dataSerializer.serializeItemStack(e.getDrops().get(i)), true, deathId);
		        }
		        boolean rebornSystem = plugin.getConfig().getBoolean("use-item-reborn-system");
		        plugin.getLogger().info("Value of reborn : " + String.valueOf(rebornSystem));
				Location location = e.getEntity().getLocation();
				String id = materialUtils.getItemName(e.getDrops().get(i));
				int data = getItemData(e.getDrops().get(i));
				databaseManager.insertLog(player.getName(), "player-death-loot", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id + (data != 0 ? ":" +  data : "") + (rebornSystem ? " | Death ID : " + deathId : ""));	
			}
		}
    }
    
    @EventHandler
    public void onPlayerExecuteCommand(PlayerCommandPreprocessEvent e) {
        if(e.isCancelled()) return;
        Player player = e.getPlayer();
        if(isLogEnable("commands")) {
            String command = e.getMessage();
            String[] commandAliases = command.split("\\s+");
            String commandName = commandAliases[0];
            if(!plugin.getConfig().getStringList("blacklist-commands").isEmpty() && !plugin.getConfig().getStringList("blacklist-commands").contains(commandName)) {
                Location location = player.getLocation();
                databaseManager.insertLog(player.getName(), "commands", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Executed command: " + command);
            }
        }
    }
    
    @EventHandler
    public void onPlayerSendMessage(AsyncPlayerChatEvent e) {
    	if(e.isCancelled()) return;
    	Player player = e.getPlayer();
    	if(isLogEnable("send-message")) {
			Location location = player.getLocation();
			String command = e.getMessage();
			databaseManager.insertLog(player.getName(), "send-message", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Message sent: " + command);
    	}
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(e.isCancelled()) return;
        if(isLogResearch(e.getPlayer())) return;
        Player player = e.getPlayer();
        ItemStack stack = e.getItem();
        Block block = e.getClickedBlock();
        Action action = e.getAction(); 
        String event = e.getEventName();
        
        if(!event.equals("BlockPlaceEvent") || !event.equals("BlockBreakEvent")) {
            if(block != null && block.getType() != Material.AIR && (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK)) {
                if(isLogEnable("interact-block")) {
                    Location location = player.getLocation();
        			String id = materialUtils.getBlockName(block);
                    int data = block.getData();
                    databaseManager.insertLog(player.getName(), "interact-block", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Block ID: " + id + (data != 0 ? ":" +  data : ""));
                }
            }
        }
        
        if(stack != null && block == null) {
            if(isLogEnable("interact-item")) {
                Location location = player.getLocation();
    			String id = materialUtils.getItemName(stack);
                int data = getItemData(stack);
                databaseManager.insertLog(player.getName(), "interact-item", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Item: " + id + (data != 0 ? ":" +  data : ""));
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteractWithEntity(PlayerInteractEntityEvent e) {
    	if(e.isCancelled()) return;
    	Player player = e.getPlayer();
    	Entity entity = e.getRightClicked();
    	if(isLogEnable("interact-entity")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "interact-entity", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Entity name: " + entity.getName() + " (ID: " + entity.getEntityId() + ")");
    	}
    }
    
    private boolean isLogResearch(Player player) {
        String id = plugin.getConfig().getString("block-tool.id");
    	return databaseManager.isToolEnabled(player.getName()) && String.valueOf(player.getItemInHand().getType()).equals(id);
    }

    private boolean isContainer(Inventory inventory) {
        return inventory.getType() == InventoryType.CHEST || inventory.getType() == InventoryType.FURNACE || inventory.getType() == InventoryType.ENDER_CHEST || inventory.getType() ==  InventoryType.DROPPER || inventory.getType() == InventoryType.DISPENSER || inventory.getType() ==  InventoryType.HOPPER;
    }
    
    public static Integer getItemData(ItemStack item) {
        if(item != null) {
            MaterialData data = item.getData();
            if(data != null) {
                return (int)data.getData();
            }
        }
        return null; 
    }
	
	public boolean isLogEnable(String logName) {
		return plugin.getConfig().getBoolean("enable-logs." + logName);
	}
}
