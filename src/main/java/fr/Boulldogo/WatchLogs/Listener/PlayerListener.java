package fr.Boulldogo.WatchLogs.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;
import fr.Boulldogo.WatchLogs.Utils.WebUtils;
import net.md_5.bungee.api.ChatColor;

public class PlayerListener implements Listener {

    private WatchLogsPlugin plugin;

    public PlayerListener(WatchLogsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.removePlayerSession(player);
        
        WebUtils webUtils = plugin.getWebUtils();
        
        if(webUtils.isPlayerExists(player)) {
        	webUtils.deletePlayerCode(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
    	Player player = e.getPlayer();
    	
    	if(player.hasPermission("watchlogs.notify_update")) {
    		if(!plugin.isUpToDate()) {
    			player.sendMessage(ChatColor.RED + "A new version of WatchLogs is available ! ");
    			player.sendMessage(ChatColor.RED + "Download it at : https://www.spigotmc.org/resources/⚙%EF%B8%8F-watchlogs-⚙%EF%B8%8F-ultimate-all-in-one-log-solution-1-7-1-20-6.117128/");
    		}
    	}
    }
}

