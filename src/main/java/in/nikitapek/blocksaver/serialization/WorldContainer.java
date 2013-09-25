package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.SupplementaryTypes;

import org.bukkit.Chunk;
import org.bukkit.Location;

import com.amshulman.mbapi.storage.StorageManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;

public final class WorldContainer {
    private static StorageManager storageManager;

    private final TypeSafeStorageMap<TypeSafeStorageMap<TypeSafeStorageMap<Reinforcement>>> reinforcements;

    public WorldContainer(final String worldName) {
        reinforcements = storageManager.getStorageMap(worldName, SupplementaryTypes.HASH_MAP);
        reinforcements.loadAll();
    }

    public static void initialize(final StorageManager storageManager) {
        WorldContainer.storageManager = storageManager;
    }

    public void removeReinforcement(final Location location) {
        final Chunk chunk = location.getChunk();
        final String regionName = getRegionNameFromChunk(chunk);
        final String chunkName = getChunkName(chunk);
        final String locationName = getLocationName(location);

        TypeSafeStorageMap<TypeSafeStorageMap<Reinforcement>> chunkMap = reinforcements.get(regionName);
        TypeSafeStorageMap<Reinforcement> reinforcementMap = chunkMap.get(chunkName);

        reinforcementMap.remove(locationName);

        // TODO: This is a performance bottleneck. It is garbage collection meant to ensure that stray nodes get removed when a reinforcement is removed.
        if (reinforcementMap.isEmpty()) {
            chunkMap.remove(chunkName);

            if (chunkMap.isEmpty()) {
                reinforcements.remove(regionName);
            }
        }
    }

    public void saveAll() {
        reinforcements.saveAll();
    }

    public void unloadAll() {
        reinforcements.unloadAll();
    }

    public void setReinforcement(final Location location, final String playerName, final float value) {
        // If the reinforcement is being set a value of 0, then it is just deleted.
        if (value <= 0) {
            removeReinforcement(location);
            return;
        }

        if (isReinforced(location)) {
            getReinforcement(location).setReinforcementValue(value);
        } else {
            ensureMapExists(location);
            TypeSafeStorageMap<Reinforcement> reinforcementMap = getReinforcementMap(location.getChunk());
            reinforcementMap.put(getLocationName(location), new Reinforcement(location, playerName, value));
        }
    }

    public Reinforcement getReinforcement(final Location location) {
        TypeSafeStorageMap<Reinforcement> reinforcementMap = getReinforcementMap(location.getChunk());

        if (reinforcementMap == null) {
            return null;
        }

        return reinforcementMap.get(getLocationName(location));
    }

    public boolean isReinforced(final Location location) {
        TypeSafeStorageMap<Reinforcement> reinforcementMap = getReinforcementMap(location.getChunk());

        if (reinforcementMap == null) {
            return false;
        }

        return reinforcementMap.containsKey(getLocationName(location));
    }

    private String getRegionNameFromChunk(final Chunk chunk) {
        return (chunk.getX() >> 5) + "." + (chunk.getZ() >> 5);
    }

    private String getChunkName(final Chunk chunk) {
        return chunk.getX() + "." + chunk.getZ();
    }

    private String getLocationName(final Location location) {
        return location.getX() + "." + location.getY() + "." + location.getZ();
    }

    private TypeSafeStorageMap<Reinforcement> getReinforcementMap(Chunk chunk) {
        TypeSafeStorageMap<TypeSafeStorageMap<Reinforcement>> chunkMap = reinforcements.get(getRegionNameFromChunk(chunk));

        if (chunkMap == null) {
            return null;
        }

        return chunkMap.get(getChunkName(chunk));
    }

    private void ensureMapExists(Location location) {
        final Chunk chunk = location.getChunk();
        final String regionName = getRegionNameFromChunk(chunk);
        final String chunkName = getChunkName(chunk);
        final String locationName = getLocationName(location);

        TypeSafeStorageMap<TypeSafeStorageMap<Reinforcement>> chunkMap = reinforcements.get(regionName);

        if (chunkMap == null) {
            chunkMap = new TypeSafeStorageMap<>();
            reinforcements.put(chunkName, chunkMap);
        }

        TypeSafeStorageMap<Reinforcement> reinforcementMap = chunkMap.get(locationName);

        if (reinforcementMap == null) {
            reinforcementMap = new TypeSafeStorageMap<>();
            chunkMap.put(locationName, reinforcementMap);
        }
    }
}
