package de.bonescraft.land;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.YearMonth;
import java.util.*;

public class PlaytimeTracker {

    private final BonescraftLand plugin;
    private final File file;
    private YamlConfiguration yaml;

    // UUID -> session start millis
    private final Map<UUID, Long> onlineSince = new HashMap<>();

    public PlaytimeTracker(BonescraftLand plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playtime.yml");
    }

    public void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            yaml = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load playtime.yml: " + e.getMessage());
            yaml = new YamlConfiguration();
        }
    }

    public void save() {
        try {
            if (yaml == null) yaml = new YamlConfiguration();
            yaml.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save playtime.yml: " + e.getMessage());
        }
    }

    public String currentMonthKey() {
        YearMonth ym = YearMonth.now();
        return ym.toString(); // "2026-02"
    }

    private String path(String monthKey, UUID uuid) {
        return "months." + monthKey + "." + uuid.toString();
    }

    public void onJoin(Player p) {
        onlineSince.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public void onQuit(Player p) {
        Long start = onlineSince.remove(p.getUniqueId());
        if (start == null) return;
        long seconds = Math.max(0, (System.currentTimeMillis() - start) / 1000L);
        addSeconds(currentMonthKey(), p.getUniqueId(), seconds);
    }

    public void flushAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            onQuit(p);
            onJoin(p);
        }
    }

    public void addSeconds(String monthKey, UUID uuid, long seconds) {
        long cur = yaml.getLong(path(monthKey, uuid), 0L);
        yaml.set(path(monthKey, uuid), cur + seconds);
    }

    public long getSeconds(String monthKey, UUID uuid) {
        return yaml.getLong(path(monthKey, uuid), 0L);
    }

    public Map<UUID, Long> getTop(String monthKey, int limit) {
        Map<String, Object> sec = yaml.getConfigurationSection("months." + monthKey) == null
                ? Collections.emptyMap()
                : yaml.getConfigurationSection("months." + monthKey).getValues(false);

        List<Map.Entry<UUID, Long>> list = new ArrayList<>();
        for (Map.Entry<String, Object> e : sec.entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                long v = (e.getValue() instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(e.getValue()));
                list.add(new AbstractMap.SimpleEntry<>(id, v));
            } catch (Exception ignored) {}
        }

        list.sort((a,b) -> Long.compare(b.getValue(), a.getValue()));
        Map<UUID, Long> out = new LinkedHashMap<>();
        for (int i=0; i<Math.min(limit, list.size()); i++) {
            out.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return out;
    }

    public static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public String resolveName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) return op.getName();
        return uuid.toString().substring(0, 8);
    }

    public void resetMonth(String monthKey) {
        yaml.set("months." + monthKey, null);
    }
}
