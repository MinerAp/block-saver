package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.SupplementaryTypes;

import java.util.HashMap;

import org.bukkit.Chunk;
import org.bukkit.Location;

import com.amshulman.mbapi.storage.StorageManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;

public final class WorldContainer {
    private static StorageManager storageManager;

    private final TypeSafeStorageMap<HashMap<Chunk, HashMap<Location, Reinforcement>>> reinforcements;
    //private final Set<Chunk> loadedChunks = new HashSet<>();

    public WorldContainer(final String worldName) {
        reinforcements = storageManager.getStorageMap(worldName, SupplementaryTypes.HASH_MAP);
        reinforcements.loadAll();
    }

    public static void initialize(final StorageManager storageManager) {
        WorldContainer.storageManager = storageManager;
    }

    public void removeReinforcement(final Location location) {
        final Chunk chunk = location.getChunk();

        //getReinforcementMap(location.getChunk()).remove(location);
        //reinforcements.remove(toString(location));

        // TODO: This is a performance bottleneck. It is meant to ensure that chunks are removed from the loadedChunks list after a Reinforcement is removed.
        final String regionName = getRegionFromChunk(chunk);
        HashMap<Chunk, HashMap<Location, Reinforcement>> chunkMap = reinforcements.get(regionName);
        HashMap<Location, Reinforcement> reinforcementMap = chunkMap.get(chunk);

        reinforcementMap.remove(location);

        if (reinforcementMap.isEmpty()) {
            chunkMap.remove(chunk);

            if (chunkMap.isEmpty()) {
                reinforcements.remove(regionName);
            }
        }

        /*
        for (Reinforcement reinforcement : reinforcements.values()) {
            if (reinforcement.getLocation().getChunk().equals(chunk)) {
                return;
            }
        } */

        //loadedChunks.remove(chunk);
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
            HashMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location.getChunk());
            reinforcementMap.put(location, new Reinforcement(location, playerName, value));
        }
    }

    public Reinforcement getReinforcement(final Location location) {
        HashMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location.getChunk());

        if (reinforcementMap == null) {
            return null;
        }

        return reinforcementMap.get(location);
    }

    public boolean isReinforced(final Location location) {
        HashMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location.getChunk());

        if (reinforcementMap == null) {
            return false;
        }

        return reinforcementMap.containsKey(location);
        //reinforcements.containsKey(toString(location));
    }

    private String getRegionFromChunk(final Chunk chunk) {
        return (chunk.getX() >> 5) + "." + (chunk.getX() >> 5);
    }

    private HashMap<Location, Reinforcement> getReinforcementMap(Chunk chunk) {
        HashMap<Chunk, HashMap<Location, Reinforcement>> chunkMap = reinforcements.get(getRegionFromChunk(chunk));

        if (chunkMap == null) {
            return null;
        }

        return chunkMap.get(chunk);
    }

    private void ensureMapExists(Location location) {
        HashMap<Chunk, HashMap<Location, Reinforcement>> chunkMap = reinforcements.get(getRegionFromChunk(location.getChunk()));

        if (chunkMap == null) {
            chunkMap = new HashMap<>();
            reinforcements.put(getRegionFromChunk(location.getChunk()), chunkMap);
        }

        HashMap<Location, Reinforcement> reinforcementMap = chunkMap.get(location);

        if (reinforcementMap == null) {
            reinforcementMap = new HashMap<>();
            chunkMap.put(location.getChunk(), reinforcementMap);
        }
    }
}
