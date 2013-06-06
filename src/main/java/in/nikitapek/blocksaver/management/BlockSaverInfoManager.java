package in.nikitapek.blocksaver.management;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;
import com.amshulman.mbapi.storage.TypeSafeStorageSet;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.google.gson.reflect.TypeToken;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplementaryTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;
import java.util.Map.Entry;

public final class BlockSaverInfoManager extends InfoManager {
    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    private final TypeSafeStorageMap<PlayerInfo> playerInfo;
    private final TypeSafeStorageSet<Reinforcement> reinforcementSet;
    private final Map<Chunk, List<Location>> reinforcements;

    private final BlockSaverConfigurationContext configurationContext;

    public BlockSaverInfoManager(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext);

        this.configurationContext = configurationContext;

        playerInfo = storageManager.getStorageMap("playerInfo", new TypeToken<PlayerInfo>() {}.getType());
        registerPlayerInfoLoader(playerInfo, FACTORY);

        reinforcementSet = storageManager.getStorageSet("reinforcements", SupplementaryTypes.REINFORCEMENT);
        reinforcementSet.load();

        // Transfer the reinforcements from the reinforcementSet to block Metadata and store all of the locations in reinforcements.
        reinforcements = new HashMap<Chunk, List<Location>>();
        final Iterator<Reinforcement> iter = reinforcementSet.iterator();
        while (iter.hasNext()) {
            writeReinforcementToMetadata(iter.next());
            iter.remove();
        }
    }

    @Override
    public void saveAll() {
        saveReinforcementsToSet();
        playerInfo.saveAll();
        reinforcementSet.saveAll();
    }

    @Override
    public void unloadAll() {
        saveReinforcementsToSet();
        playerInfo.unloadAll();
        reinforcementSet.unloadAll();
    }

    private void saveReinforcementsToSet() {
        // Saves all of the reinforcements to the reinforcementSet for serialization.
        reinforcementSet.clear();
        for (final Entry<Chunk, List<Location>> entry : reinforcements.entrySet()) {
            for (final Location location : entry.getValue()) {
                final Reinforcement reinforcement = getReinforcement(location);
                if (reinforcement != null) {
                    reinforcementSet.add(getReinforcement(location));
                }
            }
        }
    }

    public void writeReinforcementToMetadata(final Reinforcement reinforcement) {
        if (reinforcement == null) {
            return;
        }

        // Makes sure the Chunk key for the reinforcement already exists, before adding the location.
        ensureKeyExists(reinforcement.getLocation());
        reinforcement.writeToMetadata();
    }

    public void ensureKeyExists(final Location location) {
        Chunk chunk = location.getChunk();
        // Makes sure the Chunk key for the reinforcement already exists, before adding the location.
        // The if statement is commented out to prevent a SOE because of the ensureKeyExists() check in isReinforced().
        // if (!isReinforced(location)) {
        if (!reinforcements.containsKey(chunk)) {
            reinforcements.put(chunk, new ArrayList<Location>());
        }
        reinforcements.get(chunk).add(location);
        // }
    }

    public Reinforcement getReinforcement(final Location location) {
        if (!configurationContext.getReinforcementManager().isReinforced(location)) {
            return null;
        }

        return new Reinforcement(location);
    }

    public void setReinforcement(final Location location, final float value, final String playerName) {
        final Reinforcement reinforcement;

        if (configurationContext.getReinforcementManager().isReinforced(location)) {
            reinforcement = getReinforcement(location);
        } else {
            reinforcement = new Reinforcement(location, value, playerName);
        }

        writeReinforcementToMetadata(reinforcement);
    }

    public PlayerInfo getPlayerInfo(final String playerName) {
        PlayerInfo info = playerInfo.get(playerName);
        if (info == null) {
            playerInfo.load(playerName, FACTORY);
            info = playerInfo.get(playerName);
        }

        return info;
    }

    public Map<Chunk, List<Location>> getReinforcements() {
        return reinforcements;
    }
}
