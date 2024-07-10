package fr.Boulldogo.WatchLogs.Utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;

public class PermissionChecker {

    private final LuckPerms luckPerms;
    private final boolean hasLuckperms;

    public PermissionChecker(boolean hasLuckPerms) {
        this.hasLuckperms = hasLuckPerms;
        if(hasLuckPerms) {
            this.luckPerms = LuckPermsProvider.get();
        } else {
        	this.luckPerms = null;
        }
    }

    public boolean hasPermission(String playerName, String permission) {
    	if(hasLuckperms && luckPerms != null) {
            @SuppressWarnings("deprecation")
			User user = luckPerms.getUserManager().loadUser(Bukkit.getOfflinePlayer(playerName).getUniqueId()).join();
            if (user != null) {
                QueryOptions queryOptions = QueryOptions.defaultContextualOptions();
                return user.getCachedData().getPermissionData(queryOptions).checkPermission(permission).asBoolean();
            }
    	} else {
    		return Bukkit.getPlayer(playerName) != null && Bukkit.getPlayer(playerName).hasPermission(permission);
    	}
        return false;
    }
}

