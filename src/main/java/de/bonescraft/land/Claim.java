package de.bonescraft.land;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Claim {
    public UUID owner;
    public Set<UUID> trusted = new HashSet<>();

    public Claim(UUID owner) {
        this.owner = owner;
    }

    public boolean isAllowed(UUID uuid) {
        return owner.equals(uuid) || trusted.contains(uuid);
    }
}
