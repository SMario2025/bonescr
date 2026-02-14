package de.bonescraft.land;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ClaimManager {

    private final BonescraftLand plugin;
    private final Map<String, Claim> claims = new HashMap<>();
    private final File file;
    private YamlConfiguration yaml;

    public ClaimManager(BonescraftLand plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "claims.yml");
    }

    public static String key(World w, int cx, int cz) {
        return w.getUID() + ":" + cx + ":" + cz;
    }

    public Claim get(Chunk chunk) {
        return claims.get(key(chunk.getWorld(), chunk.getX(), chunk.getZ()));
    }

    public boolean isClaimed(Chunk chunk) {
        return get(chunk) != null;
    }

    public void set(Chunk chunk, Claim claim) {
        claims.put(key(chunk.getWorld(), chunk.getX(), chunk.getZ()), claim);
    }

    public void remove(Chunk chunk) {
        claims.remove(key(chunk.getWorld(), chunk.getX(), chunk.getZ()));
    }

    public int countClaims(UUID owner) {
        int c = 0;
        for (Claim claim : claims.values()) {
            if (claim.owner.equals(owner)) c++;
        }
        return c;
    }

    public void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            yaml = YamlConfiguration.loadConfiguration(file);

            claims.clear();
            for (String k : yaml.getKeys(false)) {
                String ownerStr = yaml.getString(k + ".owner");
                if (ownerStr == null) continue;

                Claim c = new Claim(UUID.fromString(ownerStr));
                List<String> trusted = yaml.getStringList(k + ".trusted");
                for (String t : trusted) c.trusted.add(UUID.fromString(t));
                claims.put(k, c);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load claims.yml: " + e.getMessage());
        }
    }

    public void save() {
        try {
            if (yaml == null) yaml = new YamlConfiguration();
            for (String k : new HashSet<>(yaml.getKeys(false))) yaml.set(k, null);

            for (Map.Entry<String, Claim> e : claims.entrySet()) {
                yaml.set(e.getKey() + ".owner", e.getValue().owner.toString());
                List<String> tr = new ArrayList<>();
                for (UUID id : e.getValue().trusted) tr.add(id.toString());
                yaml.set(e.getKey() + ".trusted", tr);
            }
            yaml.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save claims.yml: " + e.getMessage());
        }
    }
}
