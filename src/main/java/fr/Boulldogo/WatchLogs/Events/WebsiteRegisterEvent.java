package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WebsiteRegisterEvent extends Event {
	
    private static final HandlerList handlers = new HandlerList();
    
    private final String username;
    private final String ip;
    private final boolean playerIsOp;
    
    public WebsiteRegisterEvent(String username, String ip, boolean isOp) {
    	this.username = username;
    	this.ip = ip;
    	this.playerIsOp = isOp;
    }
    
    public String getUsername() {
    	return username;
    }
    
    public String getAdress() {
    	return ip;
    }
    
    public boolean isPlayerOp() {
    	return playerIsOp;
    }

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
