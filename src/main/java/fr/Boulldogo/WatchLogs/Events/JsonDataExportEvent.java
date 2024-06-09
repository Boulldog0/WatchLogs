package fr.Boulldogo.WatchLogs.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class JsonDataExportEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String fileName;
    private final String exportSettings;
    private final String exportLines;

    public JsonDataExportEvent(Player player, String fileName, String exportSettings, String exportLines) {
        this.player = player;
        this.fileName = fileName;
        this.exportSettings = exportSettings;
        this.exportLines = exportLines;
    }
    
    public String getFileName() {
    	return fileName;
    }
    
    public String getExportSettings() {
    	return exportSettings;
    }
    
    public String getExportLines() {
    	return exportLines;
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

