package fr.Boulldogo.WatchLogs.Utils;

import java.util.List;

public class PlayerSession {
    private String blockLocation;
    private int currentPage;
    private boolean toolLog;
    private List<String> currentLogs;
    private int limit;
    private int toolLimit;

    public PlayerSession() {
        this.currentPage = 1;
    }
    
    public int getToolLimit() {
    	return toolLimit;
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
}
