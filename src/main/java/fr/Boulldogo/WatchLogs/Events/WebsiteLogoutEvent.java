package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WebsiteLogoutEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final String username;
    private final String ip;
    
    public WebsiteLogoutEvent(String username, String ip) {
        this.username = username;
        this.ip = ip;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getAdress() {
        return ip;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
