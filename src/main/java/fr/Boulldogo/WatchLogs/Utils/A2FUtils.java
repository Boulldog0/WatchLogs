package fr.Boulldogo.WatchLogs.Utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.bukkit.scheduler.BukkitRunnable;
import fr.Boulldogo.WatchLogs.WatchLogsPlugin;

public class A2FUtils {

    private final WatchLogsPlugin plugin;
    private final Map<String, A2FCodeData> playerNameToA2FCode = new HashMap<>();
    private final Random random = new Random();

    public A2FUtils(WatchLogsPlugin plugin) {
        this.plugin = plugin;
        startCodeExpirationChecker();
    }

    private static class A2FCodeData {
        private final String code;
        private final long expirationTime;

        public A2FCodeData(String code, long expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }

        public String getCode() {
            return code;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }

    public String addPlayerA2FCode(String playerName) {
        long currentTime = Timestamp.from(Instant.now()).getTime();
        long expirationTime = currentTime + plugin.getConfig().getInt("website.2fa_codes_expiration") * 1000L; 

        int codeLength = plugin.getConfig().getInt("website.2fa_codes_length");
        
        if(codeLength <= 0) {
            plugin.getLogger().warning("Invalid 2FA code length in config. Using default length of 6.");
            codeLength = 6; 
        }

        String integers = "1234567890";
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < codeLength; i++) {
            int index = random.nextInt(integers.length());
            code.append(integers.charAt(index));
        }

        String generatedCode = code.toString();
        
        if(generatedCode.isEmpty()) {
            plugin.getLogger().warning("Generated 2FA code is empty!");
            return null;
        }

        playerNameToA2FCode.put(playerName, new A2FCodeData(generatedCode, expirationTime));

        return generatedCode;
    }

    public String getPlayerA2FCode(String playerName) {
        A2FCodeData data = playerNameToA2FCode.get(playerName);
        if(data != null) {
            return data.getCode();
        }
        return null;
    }

    public boolean playerHasCode(String playerName) {
        return playerNameToA2FCode.containsKey(playerName);
    }

    public boolean isA2FCodeValid(String playerName, String inputCode) {
        A2FCodeData data = playerNameToA2FCode.get(playerName);
        if(data != null) {
            long currentTime = Timestamp.from(Instant.now()).getTime();

            if(data.getExpirationTime() > currentTime && data.getCode().equals(inputCode)) {
                return true;
            } else {
                removePlayerA2FCode(playerName);  
            }
        }
        return false;
    }

    public void removePlayerA2FCode(String playerName) {
        playerNameToA2FCode.remove(playerName);
        plugin.getLogger().info("Removed 2FA code for player: " + playerName);
    }

    private void startCodeExpirationChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = Timestamp.from(Instant.now()).getTime();

                Iterator<Map.Entry<String, A2FCodeData>> iterator = playerNameToA2FCode.entrySet().iterator();
                while(iterator.hasNext()) {
                    Map.Entry<String, A2FCodeData> entry = iterator.next();
                    A2FCodeData data = entry.getValue();

                    if(currentTime >= data.getExpirationTime()) {
                        plugin.getLogger().info("Code for player " + entry.getKey() + " has expired. Removing...");
                        iterator.remove(); 
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 600);  
    }
}
