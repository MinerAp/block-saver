package in.nikitapek.blocksaver.serialization;

import com.amshulman.mbapi.storage.StorageManager;
import com.amshulman.mbapi.storage.TypeSafeDistributedStorageMap;
import com.amshulman.mbapi.storage.TypeSafeUnifiedStorageMap;
import in.nikitapek.blocksaver.util.SupplementaryTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;

public final class WorldContainer {
    private static StorageManager storageManager;

    private final TypeSafeDistributedStorageMap<TypeSafeUnifiedStorageMap<Location, Reinforcement>> reinforcements;

    public WorldContainer(final String worldName) {
        reinforcements = storageManager.getDistributedStorageMap(worldName, SupplementaryTypes.REINFORCEMENT_STORAGE);
        LocationTypeAdapter.currentWorld = worldName;
        reinforcements.loadAll();
    }

    public static void initialize(final StorageManager storageManager) {
        WorldContainer.storageManager = storageManager;
    }

    public void removeReinforcement(final Location location) {
        final Chunk chunk = location.getChunk();
        final String regionName = getRegionNameFromChunk(chunk);

        TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = reinforcements.get(regionName);
        reinforcementMap.remove(location);
    }

    public void saveAll() {
        reinforcements.saveAll();
    }

    public void unloadAll() {
        reinforcements.unloadAll();
    }

    public void setReinforcement(final Location location, final String playerName, final float value, final float coefficient) {
        // If the reinforcement is being set a value of 0, then it is just deleted.
        if (value <= 0) {
            removeReinforcement(location);
            return;
        }

        if (isReinforced(location)) {
            getReinforcement(location).setReinforcementValue(value, coefficient);
        } else {
            ensureMapExists(location);
            TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location);
            reinforcementMap.put(location, new Reinforcement(playerName, value));
        }
    }

    public Reinforcement getReinforcement(final Location location) {
        TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location);

        if (reinforcementMap == null) {
            return null;
        }

        return reinforcementMap.get(location);
    }

    /**
     * Moves a reinforcement from one location to another location, without modifying the reinforcement object.
     * @param fromLocation the initial location of the reinforcement.
     * @param toLocation the target location of the reinforcement.
     */
    public void moveReinforcement(Location fromLocation, Location toLocation) {
        ensureMapExists(toLocation);
        TypeSafeUnifiedStorageMap<Location, Reinforcement> fromReinforcementMap = getReinforcementMap(fromLocation);
        TypeSafeUnifiedStorageMap<Location, Reinforcement> toReinforcementMap = getReinforcementMap(toLocation);
        toReinforcementMap.put(toLocation, fromReinforcementMap.remove(fromLocation));
    }

    public boolean isReinforced(final Location location) {
        TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location);

        if (reinforcementMap == null) {
            return false;
        }

        return reinforcementMap.containsKey(location);
    }

    private static String getRegionNameFromChunk(final Chunk chunk) {
        return (chunk.getX() >> 5) + "." + (chunk.getZ() >> 5);
    }

    private TypeSafeUnifiedStorageMap<Location, Reinforcement> getReinforcementMap(Location location) {
        return reinforcements.get(getRegionNameFromChunk(location.getChunk()));
    }

    private void ensureMapExists(Location location) {
        if (getReinforcementMap(location) == null) {
            reinforcements.putTypeSafeUnifiedStorageMap(getRegionNameFromChunk(location.getChunk()), SupplementaryTypes.LOCATION, SupplementaryTypes.REINFORCEMENT);
        }
    }
}
