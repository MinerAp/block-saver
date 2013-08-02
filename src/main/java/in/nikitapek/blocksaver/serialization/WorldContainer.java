package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.SupplementaryTypes;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;

import com.amshulman.mbapi.storage.StorageManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;

public final class WorldContainer {
    private static StorageManager storageManager;

    private final TypeSafeStorageMap<Reinforcement> reinforcements;
    private final Set<Chunk> loadedChunks = new HashSet<>();
    //private final World world;

    public WorldContainer(final String worldName) {
        //world = Bukkit.getWorld(worldName);
        reinforcements = storageManager.getStorageMap(worldName, SupplementaryTypes.REINFORCEMENT);
        // TODO: This may be necessary.
        //reinforcementsForWorld.load();
    }

    public static void initialize(final StorageManager storageManager) {
        WorldContainer.storageManager = storageManager;
    }

    public void removeReinforcement(final Location location) {
        final Chunk chunk = location.getChunk();

        reinforcements.remove(toString(location));

        // TODO: This is a performance bottleneck. It is meant to ensure that chunks are removed from the loadedChunks list after a Reinforcement is removed.
        for (Reinforcement reinforcement : reinforcements.values()) {
            if (reinforcement.getLocation().getChunk().equals(chunk)) {
                return;
            }
        }

        loadedChunks.remove(chunk);
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
            reinforcements.put(toString(location), new Reinforcement(location, playerName, value));
        }
    }

    public Reinforcement getReinforcement(final Location location) {
        return reinforcements.get(toString(location));
    }

    public boolean isReinforced(final Location location) {
        return reinforcements.containsKey(toString(location));
    }

    private String toString(final Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
