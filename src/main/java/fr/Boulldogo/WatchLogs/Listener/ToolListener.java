package fr.Boulldogo.WatchLogs.Listener;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Utils.MaterialUtils;
import fr.Boulldogo.WatchLogs.Utils.PlayerSession;

public class ToolListener implements Listener {
    
    public final WatchLogsPlugin plugin;
    public DatabaseManager databaseManager;
    
    public ToolListener(WatchLogsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        Player player = e.getPlayer();
        Block block = e.getBlockPlaced();
        ItemStack itemInHand = e.getItemInHand();

        String id = plugin.getConfig().getString("block-tool.id");
        
        MaterialUtils utils = new MaterialUtils(plugin);

        if(utils.getBlockName(block).equals(id)) {
            if(itemInHand != null && itemInHand.hasItemMeta()) {
                if(databaseManager.playerExists(player.getName()) && databaseManager.isToolEnabled(player.getName())) {
                    if(player.hasPermission("watchlogs.use-tool")) {
                        e.setCancelled(true);
                        String blockLoc = block.getX() + "/" + block.getY() + "/" + block.getZ();
                        PlayerSession session = plugin.getPlayerSession(player);
                        int toolLimit = plugin.getConfig().getInt("block-tool.research-limit-default");
                        if(!session.hasToolLimit()) {
                        	session.setToolLimit(toolLimit);
                        }
                        session.setBlockLocation(blockLoc);
                        session.setCurrentPage(1);
                        session.setToolLog(true);
                        showLogs(player, 1);
                    } else {
                        e.setCancelled(true);
                        player.sendMessage(prefix + translateString(plugin.getLang().getString("messages.have-not-permission")));
                        databaseManager.setToolEnabled(player.getName(), false);
                        ItemStack logBlock = createLogBlock();
                        PlayerInventory inventory = player.getInventory();
                        ItemStack[] contents = inventory.getContents();
                        for (int i = 0; i < contents.length; i++) {
                            if(contents[i] != null && contents[i].isSimilar(logBlock)) {
                                inventory.setItem(i, new ItemStack(Material.AIR)); 
                            }
                        }
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        Player player = e.getPlayer();
        Block block = e.getBlock();
        ItemStack itemInHand = player.getItemInHand();

        String id = plugin.getConfig().getString("block-tool.id");
        
        MaterialUtils utils = new MaterialUtils(plugin);

        if(utils.getBlockName(block).equals(id)) {
            if(itemInHand != null && itemInHand.hasItemMeta()) {
                if(databaseManager.playerExists(player.getName()) && databaseManager.isToolEnabled(player.getName())) {
                    if(player.hasPermission("watchlogs.use-tool")) {
                        e.setCancelled(true);
                        String blockLoc = block.getX() + "/" + block.getY() + "/" + block.getZ();
                        PlayerSession session = plugin.getPlayerSession(player);
                        session.setBlockLocation(blockLoc);
                        session.setCurrentPage(1);
                        session.setToolLog(true);
                        showLogs(player, 1);
                    } else {
                        e.setCancelled(true);
                        player.sendMessage(prefix + translateString(plugin.getLang().getString("messages.have-not-permission")));
                        databaseManager.setToolEnabled(player.getName(), false);
                        ItemStack logBlock = createLogBlock();
                        PlayerInventory inventory = player.getInventory();
                        ItemStack[] contents = inventory.getContents();
                        for (int i = 0; i < contents.length; i++) {
                            if(contents[i] != null && contents[i].isSimilar(logBlock)) {
                                inventory.setItem(i, new ItemStack(Material.AIR)); 
                            }
                        }
                    }
                }
            }
        }
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        if(e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();
            Inventory clickedInventory = e.getClickedInventory();
            
            ItemStack stack = e.getCurrentItem();
            String id = plugin.getConfig().getString("block-tool.id");
            
            MaterialUtils utils = new MaterialUtils(plugin);
            
            if(clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER && stack != null && utils.getItemName(stack).equals(id) && databaseManager.playerExists(player.getName()) && databaseManager.isToolEnabled(player.getName())) {
                e.setCancelled(true);
                player.sendMessage(prefix + translateString(plugin.getLang().getString("messages.cant-inventory-interact-tool-item")));
            }   
        }
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        Player player = e.getPlayer();
        Action action = e.getAction();
        Block block = e.getClickedBlock();
        
        if(block != null && block.getType() != Material.AIR) {
            ItemStack stack = e.getItem();
            if(stack != null && stack.getType() != Material.AIR) {
                String id = plugin.getConfig().getString("block-tool.id");
                if(String.valueOf(stack.getType()).equals(id)) {
                    if(databaseManager.playerExists(player.getName()) && databaseManager.isToolEnabled(player.getName())) {
                        if(action == Action.LEFT_CLICK_BLOCK) {
                            if(player.hasPermission("watchlogs.use-tool")) {
                                e.setCancelled(true);
                                String blockLoc = block.getX() + "/" + block.getY() + "/" + block.getZ();
                                PlayerSession session = plugin.getPlayerSession(player);
                                session.setBlockLocation(blockLoc);
                                session.setCurrentPage(1);
                                showLogs(player, 1);
                            } else {
                                e.setCancelled(true);
                                player.sendMessage(prefix + translateString(plugin.getLang().getString("messages.have-not-permission")));
                                databaseManager.setToolEnabled(player.getName(), false);
                                ItemStack logBlock = createLogBlock();
                                PlayerInventory inventory = player.getInventory();
                                ItemStack[] contents = inventory.getContents();
                                for (int i = 0; i < contents.length; i++) {
                                    if(contents[i] != null && contents[i].isSimilar(logBlock)) {
                                        inventory.setItem(i, new ItemStack(Material.AIR)); 
                                    }
                                }
                            }
                        } else if(action == Action.RIGHT_CLICK_BLOCK) {
                            if(isContainer(block)) {
                                if(player.hasPermission("watchlogs.use-tool")) {
                                    e.setCancelled(true);
                                    String blockLoc = block.getX() + "/" + block.getY() + "/" + block.getZ();
                                    PlayerSession session = plugin.getPlayerSession(player);
                                    session.setBlockLocation(blockLoc);
                                    session.setCurrentPage(1);
                                    showContainerLogs(player, 1);
                                } else {
                                    e.setCancelled(true);
                                    player.sendMessage(prefix + translateString(plugin.getLang().getString("messages.have-not-permission")));
                                    databaseManager.setToolEnabled(player.getName(), false);
                                    ItemStack logBlock = createLogBlock();
                                    PlayerInventory inventory = player.getInventory();
                                    ItemStack[] contents = inventory.getContents();
                                    for (int i = 0; i < contents.length; i++) {
                                        if(contents[i] != null && contents[i].isSimilar(logBlock)) {
                                            inventory.setItem(i, new ItemStack(Material.AIR)); 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public boolean isContainer(Block block) {
        BlockState state = block.getState();
        return state instanceof Container;
    }
    
	@EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        Player player = e.getPlayer();
        
        ItemStack stack = e.getItemDrop().getItemStack();
        String id = plugin.getConfig().getString("block-tool.id");
        
        if(String.valueOf(stack.getType()).equals(id) && databaseManager.playerExists(player.getName()) && databaseManager.isToolEnabled(player.getName())) {
            e.setCancelled(true);
            player.sendMessage(prefix + translateString(plugin.getLang().getString("messages.cant-inventory-interact-tool-item"))); 
        }
    }
    
    public void showLogs(Player player, int page) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        PlayerSession session = plugin.getPlayerSession(player);
        String blockLoc = session.getBlockLocation();

        if (blockLoc == null) {
            player.sendMessage(prefix + "No block selected.");
            return;
        }

        String[] pos = blockLoc.split("/");
        int x = Integer.parseInt(pos[0]);
        int y = Integer.parseInt(pos[1]);
        int z = Integer.parseInt(pos[2]);
        String worldName = player.getWorld().getName();
        int limit = session.getToolLimit();

        List<String> logs = databaseManager.getLogs(x, y, z, worldName, limit);
        if (logs.isEmpty()) {
            player.sendMessage(prefix + "No log found here.");
        } else {
        	int entries = plugin.getConfig().getInt("max-entries");
            int totalPage = (logs.size() + (entries - 1)) / entries;
            if(page <= totalPage) {
                player.sendMessage(""); 
                player.sendMessage(prefix + "Logs found (Page " + page + "/" + totalPage + "):"); 
                player.sendMessage("");
                int startIndex = (page - 1) * entries; 
                int endIndex = Math.min(startIndex + entries, logs.size()); 
                for(int i = startIndex; i < endIndex; i++) {
                    player.sendMessage(logs.get(i));
                }
                if(page < totalPage) {
                	player.sendMessage(ChatColor.RED + "Type /wl page " + (page + 1) + " for view next page !");
                }
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Specified page number is higher than the maximum of pages for this research.");
            }
        }
    }
    
    public ItemStack createLogBlock() {
        String id = plugin.getConfig().getString("block-tool.id");

        Material material = Material.getMaterial(id);
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(translateString(plugin.getConfig().getString("block-tool.name")));
            stack.setItemMeta(meta);
        }
        return stack;
    }
    
    public void showContainerLogs(Player player, int page) {
        String prefix = plugin.getConfig().getBoolean("use-prefix") ? translateString(plugin.getConfig().getString("prefix")) : "";
        PlayerSession session = plugin.getPlayerSession(player);
        String blockLoc = session.getBlockLocation();

        if (blockLoc == null) {
            player.sendMessage(prefix + "No container selected.");
            return;
        }

        String[] pos = blockLoc.split("/");
        int x = Integer.parseInt(pos[0]);
        int y = Integer.parseInt(pos[1]);
        int z = Integer.parseInt(pos[2]);
        String worldName = player.getWorld().getName();

        List<String> logs = databaseManager.getContainerLogs(x, y, z, worldName);
        if (logs.isEmpty()) {
            player.sendMessage(prefix + "No log found here.");
        } else {
            int totalPage = (logs.size() + 9) / 10;
            if(page <= totalPage) {
                player.sendMessage(""); 
                player.sendMessage(prefix + "Logs found (Page " + page + "/" + totalPage + "):"); 
                int startIndex = (page - 1) * 10; 
                int endIndex = Math.min(startIndex + 10, logs.size()); 
                for (int i = startIndex; i < endIndex; i++) {
                    player.sendMessage(logs.get(i));
                }
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Specified page number is higher than the maximum of pages for this research.");
            }
        }
    }
   
    public String translateString(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

