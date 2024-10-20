package fr.Boulldogo.WatchLogs.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerSession {
	
	private Map<Player, String[]> spyedPlayers = new HashMap<>();
    private String blockLocation;
    private OfflinePlayer player;
    private int currentPage;
    private boolean toolLog;
    private List<String> currentLogs;
    private int limit;
    private int toolLimit;
    private boolean isActive;

    public PlayerSession() {
        this.currentPage = 1;
    }
    
    public int getToolLimit() {
    	return toolLimit;
    }
    
    public void setPlayer(OfflinePlayer player) {
    	this.player = player;
    }
    
    public OfflinePlayer getPlayer() {
    	return player;
    }
    
    public void setToolLimit(int limit) {
    	this.toolLimit = limit;
    }
    
    public boolean hasToolLimit() {
    	return toolLimit != 0;
    }
    
    public int getLimit() {
    	return limit;
    }
    
    public void setLimit(int limit) {
    	this.limit = limit;
    }

    public String getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(String blockLocation) {
        this.blockLocation = blockLocation;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public List<String> getCurrentLogs() {
        return currentLogs;
    }

    public void setCurrentLogs(List<String> currentLogs) {
        this.currentLogs = currentLogs;
    }
    
    public boolean isToolLog() {
    	return this.toolLog;
    }
    
    public void setToolLog(boolean isToolLog) {
    	this.toolLog = isToolLog;
    }
    
    public boolean isSessionActive() {
    	return isActive;
    }
    
    public void setSessionActivity(boolean isActive) {
    	this.isActive = isActive;
    }
    
    public void addSpyPlayer(Player player, String[] actions) {
    	spyedPlayers.put(player, actions);
    }
    
    public void removeSpyPlayer(Player player) {
    	spyedPlayers.remove(player);
    }
    
    public Map<Player, String[]> getSpyedPlayers() {
    	return spyedPlayers;
    }
    
    public boolean isPlayerSpyed(Player player) {
    	return !spyedPlayers.isEmpty() && spyedPlayers.containsKey(player);
    }
    
    public boolean isActionEnabledForPlayer(Player player, String action) {
    	if(!isPlayerSpyed(player)) return false;
    	
    	String[] actions = spyedPlayers.get(player);
    	for(String act : actions) {
    		if(act.equals(action)) {
    			return true;
    		}
    	}
    	return false;
    }
}
