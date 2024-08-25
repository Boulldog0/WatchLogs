package fr.Boulldogo.WatchLogs.Listener;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.Bukkit;
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
import org.bukkit.event.entity.EntityExplodeEvent;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Events.TracedItemActionEvent;
import fr.Boulldogo.WatchLogs.Utils.ItemDataSerializer;
import fr.Boulldogo.WatchLogs.Utils.MaterialUtils;
import fr.Boulldogo.WatchLogs.Utils.TraceItemUtils;

@SuppressWarnings("deprecation")
public class MinecraftListener implements Listener {
	
	public final WatchLogsPlugin plugin;
	public DatabaseManager databaseManager;
	public MaterialUtils materialUtils;
	public ItemDataSerializer dataSerializer;
	private TraceItemUtils tiu;
	
	public MinecraftListener(WatchLogsPlugin plugin, DatabaseManager databaseManager, MaterialUtils materialUtils, ItemDataSerializer dataSerializer) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
		this.materialUtils = materialUtils;
		this.dataSerializer = dataSerializer;
		this.tiu = plugin.getTraceItemUtils();
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
		
		if(isLogEnable("teleport")) {
			Location location = e.getTo();
			databaseManager.insertLog(player.getName(), "teleport", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Teleportation type: " + e.getCause().toString());
		}
	}
	
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if(e.isCancelled()) return;
        if(e.getBlock().getType() == Material.AIR) return;
        if(isLogResearch(e.getPlayer())) return;
        Player player = e.getPlayer();
        Block block = e.getBlock();
        String blockName = materialUtils.getBlockName(block);
        int data = block.getData();
        if(isLogEnable("block-place") && !isLogResearch(player) && materialUtils.isBlockActivated(blockName)) {
            Location location = block.getLocation();
            databaseManager.insertLog(player.getName(), "block-place", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Block Type: " + blockName +(data != 0 ? ":" +  data : ""));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if(e.isCancelled()) return;
        if(e.getBlock().getType() == Material.AIR) return;
        Player player = e.getPlayer();
        Block block = e.getBlock();
        String blockName = materialUtils.getBlockName(block);
        int data = block.getData();
        if(isLogEnable("block-break") && materialUtils.isBlockActivated(blockName)) {
            Location location = block.getLocation();
            databaseManager.insertLog(player.getName(), "block-break", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Block Type: " + blockName +(data != 0 ? ":" +  data : ""));
        }
    }
	
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
	    if(e.isCancelled()) return;
	    Inventory inventory = e.getInventory();
	    Player player =(Player) e.getPlayer();
	    if(isContainer(inventory) && !plugin.getPlayerSession(player).isSessionActive()) {
	        if(isLogEnable("container-open")) {
	            Location location = e.getPlayer().getLocation();
	            String inventoryType = getContainerName(inventory.getType());
	            String inventoryName = e.getView().getTitle();
	            databaseManager.insertLog(player.getName(), "container-open", (location == null ? "Unknow" : location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ()), player.getWorld().getName(), "Container: " + inventoryType + " | Container Name : " + inventoryName);
	        }
	    }
	}
	
    @EventHandler
    public void onBlockExplosion(BlockExplodeEvent e) {
        handleExplosion(e.blockList(), e.getBlock().getWorld().getName(), e.getBlock().toString());
    }

    @EventHandler
    public void onEntityExplosion(EntityExplodeEvent e) {
        handleExplosion(e.blockList(), e.getLocation().getWorld().getName(), e.getEntity().getName());
    }

    private void handleExplosion(List<Block> blocks, String worldName, String cause) {
        for(Block block : blocks) {
            if(block.getType() == Material.AIR) continue;
            String blockName = materialUtils.getBlockName(block);
            int data = block.getData();
            if(isLogEnable("block-explosion") && materialUtils.isBlockActivated(blockName)) {
                Location location = block.getLocation();
                databaseManager.insertLog("Explosion", "block-explosion", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), worldName, "Block Type: " + blockName +(data != 0 ? ":" +  data : "" + " | Cause : " + cause)
                );
            }
        }
    }

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		new BukkitRunnable() {
			
			@Override
			public void run() {
			    if(event.isCancelled()) return;
			    if(event.getClickedInventory() == null) return;
			    if(event.getWhoClicked() instanceof Player) {
			        Player player =(Player) event.getWhoClicked();
			        Inventory clickedInventory = event.getClickedInventory();
			        Inventory topInventory = event.getView().getTopInventory();
			        if(clickedInventory != null && isContainer(topInventory)) {
			            InventoryAction action = event.getAction();
			            ItemStack currentItem = event.getCurrentItem();
			            Location location = event.getInventory().getLocation();
			            String containerName = getContainerName(topInventory.getType());
			            String name = event.getView().getTitle();

			            switch(action) {
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
			                    logTransfer(player, currentItem, clickedInventory, topInventory, location, containerName, name);
			                    break;
			                default:
			                    break;
			            }
			        }
			    }
			}
		}.runTaskAsynchronously(plugin);
	}

	private void logTransfer(Player player, ItemStack item, Inventory fromInventory, Inventory toInventory, Location location, String containerName, String name) {
		if(item == null) return;
        String id = materialUtils.getItemName(item);
	    if(isLogEnable("container-transaction") && materialUtils.isItemActivated(id)) {
	        int data = getItemData(item);
	        int amount = item.getAmount();
	        databaseManager.insertLog(player.getName(), "container-transaction",(location == null ? "Unknow" : location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ()), player.getWorld().getName(), containerName + ", " + id +(data != 0 ? ":" +  data : "") + "(x" + amount + ")" + " | Container name: " + name);
	        
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	dataSerializer.serializeItemStack(item, stack -> {
		        	databaseManager.addItemEntry(stack, false, -1);
	        	});
	        }
	    }    
	    
	    if(plugin.getConfig().getBoolean("trace-item.enable")) {
	    	if(tiu.hasTag(item)) {
	    		String UUID = tiu.getWltiTagValue(item);
	    		String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.servername") : "Unknow (Multi Server Disable)";
	    		TracedItemActionEvent event = new TracedItemActionEvent(player, item, UUID, serverName, databaseManager.getLastLogId(), location);
	    		Bukkit.getServer().getPluginManager().callEvent(event);
	    		if(databaseManager.UUIDExists(UUID)) {
	    			String logString = "";
					try {
						logString = databaseManager.getActionString(UUID);
					} catch (SQLException e) {
						e.printStackTrace();
					}
	    			databaseManager.setItemStringLog(UUID, logString + "," + databaseManager.getLastLogId());
	    		} else {
	    			databaseManager.registerFirstItemLog(UUID, databaseManager.getLastLogId());
	    		}
	    	}
	    }
	}
    
    @EventHandler
    public void ItemDropEvent(PlayerDropItemEvent e) {
		if(e.isCancelled()) return;
		Player player = e.getPlayer();
		ItemStack item = e.getItemDrop().getItemStack();
		String id = materialUtils.getItemName(item);
		Location location = e.getItemDrop().getLocation();
		if(isLogEnable("item-drop") && materialUtils.isItemActivated(id)) {
			int data = getItemData(e.getItemDrop().getItemStack());
			databaseManager.insertLog(player.getName(), "item-drop", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id +(data != 0 ? ":" +  data : ""));
			
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	dataSerializer.serializeItemStack(item, stack -> {
		        	databaseManager.addItemEntry(stack, false, -1);
	        	});
	        }
		}		
	    
	    if(plugin.getConfig().getBoolean("trace-item.enable")) {
	    	if(tiu.hasTag(item)) {
	    		String UUID = tiu.getWltiTagValue(item);
	    		String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.servername") : "Unknow (Multi Server Disable)";
	    		TracedItemActionEvent event = new TracedItemActionEvent(player, item, UUID, serverName, databaseManager.getLastLogId(), location);
	    		Bukkit.getServer().getPluginManager().callEvent(event);
	    		if(databaseManager.UUIDExists(UUID)) {
	    			String logString = "";
					try {
						logString = databaseManager.getActionString(UUID);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
	    			databaseManager.setItemStringLog(UUID, logString + "," + databaseManager.getLastLogId());
	    		} else {
	    			databaseManager.registerFirstItemLog(UUID, databaseManager.getLastLogId());
	    		}
	    	}
	    }
    }
    
    @EventHandler
    public void ItemPickupEvent(PlayerPickupItemEvent e) {
		if(e.isCancelled()) return;
		Player player = e.getPlayer();
		ItemStack item = e.getItem().getItemStack();
		String id = materialUtils.getItemName(item);
		Location location = e.getItem().getLocation();
		if(isLogEnable("item-pickup") && materialUtils.isItemActivated(id)) {
			int data = getItemData(e.getItem().getItemStack());
			databaseManager.insertLog(player.getName(), "item-pickup", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id +(data != 0 ? ":" +  data : ""));
			
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	dataSerializer.serializeItemStack(item, stack -> {
		        	databaseManager.addItemEntry(stack, false, -1);
	        	});
	        }
		}
		
	    if(plugin.getConfig().getBoolean("trace-item.enable")) {
	    	if(tiu.hasTag(item)) {
	    		String UUID = tiu.getWltiTagValue(item);
	    		String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.servername") : "Unknow (Multi Server Disable)";
	    		TracedItemActionEvent event = new TracedItemActionEvent(player, item, UUID, serverName, databaseManager.getLastLogId(), location);
	    		Bukkit.getServer().getPluginManager().callEvent(event);
	    		if(databaseManager.UUIDExists(UUID)) {
	    			String logString = "";
					try {
						logString = databaseManager.getActionString(UUID);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
	    			databaseManager.setItemStringLog(UUID, logString + "," + databaseManager.getLastLogId());
	    		} else {
	    			databaseManager.registerFirstItemLog(UUID, databaseManager.getLastLogId());
	    		}
	    	}
	    }
    }
    
    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent e) {
		Player player = e.getPlayer();
		String id = materialUtils.getItemName(e.getBrokenItem());
		ItemStack item = e.getBrokenItem();
		Location location = e.getPlayer().getLocation();
		if(isLogEnable("item-break") && materialUtils.isItemActivated(id)) {
			int data = getItemData(e.getBrokenItem());
			databaseManager.insertLog(player.getName(), "item-break", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id +(data != 0 ? ":" +  data : ""));
			
	        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
	        	dataSerializer.serializeItemStack(item, stack -> {
		        	databaseManager.addItemEntry(stack, false, -1);
	        	});
	        }
		}	
		
	    if(plugin.getConfig().getBoolean("trace-item.enable")) {
	    	if(tiu.hasTag(item)) {
	    		String UUID = tiu.getWltiTagValue(item);
	    		String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.servername") : "Unknow (Multi Server Disable)";
	    		TracedItemActionEvent event = new TracedItemActionEvent(player, item, UUID, serverName, databaseManager.getLastLogId(), location);
	    		Bukkit.getServer().getPluginManager().callEvent(event);
	    		if(databaseManager.UUIDExists(UUID)) {
	    			String logString = "";
					try {
						logString = databaseManager.getActionString(UUID);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
	    			databaseManager.setItemStringLog(UUID, logString + "," + databaseManager.getLastLogId());
	    		} else {
	    			databaseManager.registerFirstItemLog(UUID, databaseManager.getLastLogId());
	    		}
	    	}
	    }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		Location location = e.getEntity().getLocation();
		if(isLogEnable("player-death")) {
			databaseManager.insertLog(player.getName(), "player-death", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Death type: " + e.getDeathMessage());
		}	

		if(isLogEnable("player-death-loot")) {
			int deathId = databaseManager.getLastDeathId() + 1;
			if(!e.getDrops().isEmpty()) {
				for(int i = 0; i < e.getDrops().size(); i++) {
			        if(plugin.getConfig().getBoolean("use-item-reborn-system")) {
			        	dataSerializer.serializeItemStack(e.getDrops().get(i), stack -> {
				        	databaseManager.addItemEntry(stack, true, deathId);
			        	});
			        }
			        boolean rebornSystem = plugin.getConfig().getBoolean("use-item-reborn-system");
					String id = e.getDrops().get(i).getType().toString();
					if(materialUtils.isItemActivated(id)) {
						int data = getItemData(e.getDrops().get(i));
						databaseManager.insertLog(player.getName(), "player-death-loot", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Item: " + id +(data != 0 ? ":" +  data : "") + (rebornSystem ? " | Death ID : " + deathId : ""));	
					}
				}
			}
		}
		
		if(!e.getDrops().isEmpty()) {
			for(int i = 0; i < e.getDrops().size(); i++) {
				ItemStack item = e.getDrops().get(i);
			    if(plugin.getConfig().getBoolean("trace-item.enable")) {
			    	if(tiu.hasTag(item)) {
			    		String UUID = tiu.getWltiTagValue(item);
			    		String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.servername") : "Unknow (Multi Server Disable)";
			    		TracedItemActionEvent event = new TracedItemActionEvent(player, item, UUID, serverName, databaseManager.getLastLogId(), location);
			    		Bukkit.getServer().getPluginManager().callEvent(event);
			    		if(databaseManager.UUIDExists(UUID)) {
			    			String logString = "";
							try {
								logString = databaseManager.getActionString(UUID);
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
			    			databaseManager.setItemStringLog(UUID, logString + "," + databaseManager.getLastLogId());
			    		} else {
			    			databaseManager.registerFirstItemLog(UUID, databaseManager.getLastLogId());
			    		}
			    	}
			    }
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
            } else if(plugin.getConfig().getStringList("blacklist-commands").isEmpty()) {
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
        if(e.getClickedBlock().getType() == Material.AIR) return;
        if(isLogResearch(e.getPlayer())) return;
        Player player = e.getPlayer();
        ItemStack stack = e.getItem();
        Block block = e.getClickedBlock();
        if(stack == null && block == null) return;
        Action action = e.getAction(); 
        Location location = e.getClickedBlock().getLocation();
        
         if(block != null && block.getType() != Material.AIR && (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK)) {
        	 if(materialUtils.getBlockName(block).equals("AIR")) return;
             if(isLogEnable("interact-block") && materialUtils.isBlockActivated(materialUtils.getBlockName(block))) {
            	 databaseManager.isBlockInteract(location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getName(), result -> {
                     String id = materialUtils.getBlockName(block);
                     int data = block.getData();
                     databaseManager.insertLog(player.getName(), "interact-block", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(), "Block ID: " + id +(data != 0 ? ":" +  data : ""));
            	 });
             }
         }
        
        if(stack != null) {
        	if(action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                if(isLogEnable("interact-item") && materialUtils.isItemActivated(materialUtils.getItemName(stack))) {
                    Location location2 = player.getLocation();
        			String id = materialUtils.getItemName(stack);
                    int data = getItemData(stack);
                    databaseManager.insertLog(player.getName(), "interact-item", location2.getBlockX() + "/" + location2.getBlockY() + "/" + location2.getBlockZ(), player.getWorld().getName(), "Item: " + id +(data != 0 ? ":" +  data : ""));
                }
                
        	    if(plugin.getConfig().getBoolean("trace-item.enable")) {
        	    	if(tiu.hasTag(stack)) {
        	    		String UUID = tiu.getWltiTagValue(stack);
        	    		String serverName = plugin.getConfig().getBoolean("multi-server.enable") ? plugin.getConfig().getString("multi-server.servername") : "Unknow (Multi Server Disable)";
        	    		TracedItemActionEvent event = new TracedItemActionEvent(player, stack, UUID, serverName, databaseManager.getLastLogId(), location);
        	    		Bukkit.getServer().getPluginManager().callEvent(event);
        	    		if(databaseManager.UUIDExists(UUID)) {
        	    			String logString = "";
        					try {
        						logString = databaseManager.getActionString(UUID);
        					} catch (SQLException e1) {
        						e1.printStackTrace();
        					}
        	    			databaseManager.setItemStringLog(UUID, logString + "," + databaseManager.getLastLogId());
        	    		} else {
        	    			databaseManager.registerFirstItemLog(UUID, databaseManager.getLastLogId());
        	    		}
        	    	}
        	    }
        	}
        }
    }
    
    @EventHandler
    public void onPlayerInteractWithEntity(PlayerInteractEntityEvent e) {
    	if(e.isCancelled()) return;
    	if(e.getRightClicked() instanceof Player) return;
    	Player player = e.getPlayer();
    	Entity entity = e.getRightClicked();
    	if(isLogEnable("interact-entity")) {
			Location location = player.getLocation();
			databaseManager.insertLog(player.getName(), "interact-entity", location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ(), player.getWorld().getName(),"Entity name: " + entity.getName() + "(ID: " + entity.getEntityId() + ")");
    	}
    }  
    
    public ItemStack createLogBlock() {
        String id = plugin.getConfig().getString("block-tool.id");

        Material material = Material.getMaterial(id);
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if(meta != null) {
            stack.setItemMeta(meta);
        }
        return stack;
    }

	private String getContainerName(InventoryType type) {
		if(type == null) return "Unknow container";
	    switch(type) {
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
	        case ENDER_CHEST:
	        	return "Ender Chest";
	        default:
	            return "No-Vanilla Container";
	    }
	}
    
    private boolean isLogResearch(Player player) {
        String id = plugin.getConfig().getString("block-tool.id");
        if(!plugin.getPlayerSession(player).isSessionActive()) return false;
    	return String.valueOf(player.getItemInHand().getType()).equals(id);
    }

    private boolean isContainer(Inventory inventory) {
        return inventory.getType() == InventoryType.CHEST || inventory.getType() == InventoryType.FURNACE || inventory.getType() == InventoryType.ENDER_CHEST || inventory.getType() ==  InventoryType.DROPPER || inventory.getType() == InventoryType.DISPENSER || inventory.getType() ==  InventoryType.HOPPER;
    }
    
    public static Integer getItemData(ItemStack item) {
        if(item != null) {
            MaterialData data = item.getData();
            if(data != null) {
                return(int)data.getData();
            }
        }
        return null; 
    }
	
	public boolean isLogEnable(String logName) {
		return plugin.getConfig().getBoolean("enable-logs." + logName);
	}
}
