package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WebsiteLogsRequestEvent extends Event {
	
    private static final HandlerList handlers = new HandlerList();
    
    private final String username;
    private final String ip;
    private final String search;
    
    public WebsiteLogsRequestEvent(String username, String ip, String search) {
    	this.username = username;
    	this.ip = ip;
    	this.search = search;
    }
    
    public String getUsername() {
    	return username;
    }
    
    public String getAdress() {
    	return ip;
    }
    public String getSearch() {
    	return search;
    }

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
