package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class TracedItemActionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ItemStack stack;
    private final String UUID;
    private final String server;
    private final int actionId;
    private final Location location;

    public TracedItemActionEvent(Player player, ItemStack stack, String UUID, String server, int actionId, Location location) {
        this.player = player;
        this.stack = stack;
        this.UUID = UUID;
        this.server = server;
        this.actionId = actionId;
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }
    
    public ItemStack getItemStack() {
    	return stack;
    }
    
    public String getItemUUID() {
    	return UUID;
    }
    
    public String getServer() {
    	return server;
    }
    
    public int getActionId() {
    	return actionId;
    }
    
    public Location getLocation() {
    	return location;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

