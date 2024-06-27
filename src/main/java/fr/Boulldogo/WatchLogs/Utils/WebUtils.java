package fr.Boulldogo.WatchLogs.Utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class WebUtils {

    private final Map<Player, String> playerCode;

    public WebUtils() {
        this.playerCode = new HashMap<>();
    }

    public boolean isPlayerExists(Player player) {
        if(player == null) {
            return false;
        }
        return playerCode.containsKey(player);
    }

    public String getPlayerCode(Player player) {
        if(player == null) {
            return null;
        }
        return playerCode.get(player);
    }

    public void setPlayerCode(Player player, String code) {
        if(player == null || code == null) {
            throw new IllegalArgumentException("Player and code cannot be null");
        }
        playerCode.put(player, code);
    }

    public void deletePlayerCode(Player player) {
        if(player != null) {
            playerCode.remove(player);
        }
    }
}
