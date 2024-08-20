package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WebsiteLoginEvent extends Event {
	
    private static final HandlerList handlers = new HandlerList();
    
    private final String username;
    private final String ip;
    private final boolean usingA2F;
    
    public WebsiteLoginEvent(String username, String ip, boolean usingA2F) {
    	this.username = username;
    	this.ip = ip;
    	this.usingA2F = usingA2F;
    }
    
    public String getUsername() {
    	return username;
    }
    
    public String getAdress() {
    	return ip;
    }
    
    public boolean isUsingA2F() {
    	return usingA2F;
    }

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
