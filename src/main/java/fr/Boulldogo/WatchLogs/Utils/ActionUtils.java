package fr.Boulldogo.WatchLogs.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionUtils {
	
	public static List<String> actions() {
		List<String> actionList = new ArrayList<String>();
		actionList.addAll(Arrays.asList( "join", "leave", "teleport", "block-place", "block-break", "container-open", "container-transaction",
           "item-drop", "item-pickup", "item-break", "player-death", "player-death-loot", "commands", "send-message",
           "interact-item", "interact-block", "interact-entity", "block-explosion", "json-import", "json-export", 
           "website-login", "website-logout", "website-logs-search", "website-register"));
		return actionList;
	}
	
	public static String getFormattedNameForActions(String s) {
        switch (s) {
            case "join":
                return "Join";
            case "leave":
                return "Leave";
            case "teleport":
                return "Teleport";
            case "block-place":
                return "Block Place";
            case "block-break":
                return "Block Break";
            case "container-open":
                return "Container Open";
            case "container-transaction":
                return "Container Transaction";
            case "item-drop":
                return "Item Drop";
            case "item-pickup":
                return "Item Pickup";
            case "item-break":
                return "Item Break";
            case "player-death":
                return "Player Death";
            case "player-death-loot":
                return "Player Death Loot";
            case "commands":
                return "Commands";
            case "send-message":
                return "Send Message";
            case "interact-item":
                return "Interact Item";
            case "interact-block":
                return "Interact Block";
            case "interact-entity":
                return "Interact Entity";
            case "block-explosion":
            	return "Block Explosion";
            case "json-import":
            	return "Json Import";
            case "json-export":
            	return "Json Export";
            case "website-login":
            	return "Website Login";
            case "website-logout":
            	return "Website Logout";
            case "website-logs-search":
            	return "Website Logs Search";
            case "website-register":
            	return "Website Register";
            default:
                return s + " (Formatted Name unknown)";
        }
    }

}
