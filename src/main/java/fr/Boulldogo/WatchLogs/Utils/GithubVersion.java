package fr.Boulldogo.WatchLogs.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.bukkit.plugin.java.JavaPlugin;

public class GithubVersion {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Boulldog0/WatchLogs/releases/latest";
    private JavaPlugin plugin;

    public GithubVersion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        try {
            String currentVersion = plugin.getDescription().getVersion();
            String latestVersion = getLatestVersion();

            if (latestVersion == null) {
                plugin.getLogger().warning("Unable to check for updates of WatchLogs.");
                return;
            }

            if (isVersionOutOfDate(currentVersion, latestVersion)) {
                plugin.getLogger().warning("New version of plugin WatchLogs is available. : " + latestVersion);
                plugin.getLogger().warning("Downloat it at : https://www.spigotmc.org/resources/⚙%EF%B8%8F-watchlogs-⚙%EF%B8%8F-ultimate-all-in-one-log-solution-1-7-1-20-6.117128/");
            } else {
                plugin.getLogger().info("WatchLogs plugin is up-to-date.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error when trying to check updates for WatchLogs : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getLatestVersion() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        if (connection.getResponseCode() != 200) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("tag_name");
    }

    private boolean isVersionOutOfDate(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (currentPart < latestPart) {
                return true;
            } else if (currentPart > latestPart) {
                return false;
            }
        }

        return false;
    }
}
