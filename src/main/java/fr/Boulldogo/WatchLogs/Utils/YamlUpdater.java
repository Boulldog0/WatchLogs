package fr.Boulldogo.WatchLogs.Utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;

import java.io.*;
import java.util.*;

public class YamlUpdater {
    private final WatchLogsPlugin plugin;
    private final Yaml yamlLoader;
    private final Yaml yamlDumper;

    public YamlUpdater(WatchLogsPlugin plugin) {
        this.plugin = plugin;
        this.yamlLoader = new Yaml();

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yamlDumper = new Yaml(options);
    }

    public void updateYamlFiles(String[] fileNames) {
        for(String fileName : fileNames) {
            try {
                File dataFolderFile = new File(plugin.getDataFolder(), fileName);

                if(!dataFolderFile.exists()) {
                    plugin.getLogger().warning("[YAML-Updater] File not found in data folder: " + fileName);
                    continue;
                }

                try(InputStream defaultYamlStream = getClass().getResourceAsStream("/" + fileName)) {
                    if(defaultYamlStream == null) {
                        plugin.getLogger().warning("[YAML-Updater] Default file not found in WatchLogs.jar: " + fileName);
                        continue;
                    }

                    Map<String, Object> defaultYamlMap = loadYaml(defaultYamlStream);
                    Map<String, Object> dataFolderYamlMap = loadYaml(new FileInputStream(dataFolderFile));

                    boolean updated = addMissingEntries(defaultYamlMap, dataFolderYamlMap);

                    if(updated) {
                        saveYaml(dataFolderFile, dataFolderYamlMap);
                        plugin.getLogger().info("[YAML-Updater] File updated: " + fileName);
                    } else {
                        plugin.getLogger().info("[YAML-Updater] All keys are present in: " + fileName);
                    }
                }
            } catch(IOException e) {
                plugin.getLogger().severe("[YAML-Updater] Error updating file: " + fileName);
                e.printStackTrace();
            }
        }
    }

    private Map<String, Object> loadYaml(InputStream inputStream) {
        return yamlLoader.load(inputStream);
    }

    @SuppressWarnings("unchecked")
    private boolean addMissingEntries(Map<String, Object> source, Map<String, Object> target) {
        boolean updated = false;

        for(String key : source.keySet()) {
            if(source.get(key) instanceof Map) {
                if(!(target.get(key) instanceof Map)) {
                    target.put(key, new HashMap<String, Object>());
                    updated = true;
                }
                Map<String, Object> subSource =(Map<String, Object>) source.get(key);
                Map<String, Object> subTarget =(Map<String, Object>) target.get(key);
                boolean subUpdated = addMissingEntries(subSource, subTarget);
                if(subUpdated) {
                    updated = true;
                }
            } else if(source.get(key) instanceof List) {
                if(!(target.get(key) instanceof List)) {
                    target.put(key, new ArrayList<Object>((List<Object>) source.get(key)));
                    updated = true;
                } else {
                    List<Object> sourceList =(List<Object>) source.get(key);
                    List<Object> targetList =(List<Object>) target.get(key);

                    if(!isStringList(sourceList)) {
                        for(Object item : sourceList) {
                            if(!targetList.contains(item)) {
                                targetList.add(item);
                                updated = true;
                            }
                        }
                    }
                }
            } else {
                if(!target.containsKey(key)) {
                    target.put(key, source.get(key));
                    updated = true;
                }
            }
        }

        return updated;
    }

    private boolean isStringList(List<Object> list) {
        for(Object item : list) {
            if(!(item instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private void saveYaml(File file, Map<String, Object> yamlMap) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(file);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream)) {
            yamlDumper.dump(yamlMap, outputStreamWriter);
        }
    }
}
