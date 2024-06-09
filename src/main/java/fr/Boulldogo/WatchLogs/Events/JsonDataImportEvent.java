package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class JsonDataImportEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String fileName;
    private final boolean useOriginalIds;
    private final String idMap;

    public JsonDataImportEvent(Player player, String fileName, boolean useOriginalIds, String IdMap) {
        this.player = player;
        this.fileName = fileName;
        this.useOriginalIds = useOriginalIds;
        this.idMap = IdMap;
    }
    
    public String getFileName() {
    	return fileName;
    }
    
    public boolean isUsedOriginalIds() {
    	return useOriginalIds;
    }
    
    public String getIdMap() {
    	return idMap;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

