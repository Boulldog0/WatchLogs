package fr.Boulldogo.WatchLogs.Utils;

import java.sql.Timestamp;

public class LogEntry {
	
	private final String action;
	private final String playerName;
	private final String location;
	private final String worldName;
	private final String result;
	private final String server;
	private final Timestamp timestamp;
	
	public LogEntry(String action, String playerName, String location, String worldName, String result, Timestamp timestamp, String server) {
		this.action = action;
		this.playerName = playerName;
		this.location = location;
		this.worldName = worldName;
		this.result = result;
		this.timestamp = timestamp;
		this.server = server;
	}
	
	public String getAction() {
		return action;
	}
	
	public String getPlayer() {
		return playerName;
	}
	
	public String getLocation() {
		return location;
	}
	
	public String getWorld() {
		return worldName;
	}
	
	public String getResult() {
		return result;
	}
	
	public String getServer() {
		return server;
	}
	
	public Timestamp getTimestamp() {
		return timestamp;
	}

}
