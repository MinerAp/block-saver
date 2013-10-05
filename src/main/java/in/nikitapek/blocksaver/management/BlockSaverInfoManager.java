package in.nikitapek.blocksaver.management;

import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.serialization.WorldContainer;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplementaryTypes;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeDistributedStorageMap;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.TypeSafeSet;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public final class BlockSaverInfoManager extends InfoManager {
    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    private final TypeSafeSet<String> worlds;

    private final TypeSafeDistributedStorageMap<PlayerInfo> playerInfo;

    private final TypeSafeMap<String, WorldContainer> worldContainers;

    private ReinforcementManager reinforcementManager;

    public BlockSaverInfoManager(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext);

        worlds = configurationContext.worlds;

        playerInfo = storageManager.getDistributedStorageMap("playerInfo", SupplementaryTypes.PLAYER_INFO);
        registerPlayerInfoLoader(playerInfo, FACTORY);

        WorldContainer.initialize(storageManager);
        worldContainers = new TypeSafeMapImpl<>(new HashMap<String, WorldContainer>(), SupplementaryTypes.STRING, SupplementaryTypes.WORLD_CONTAINER);

        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            worldContainers.put(worldName, new WorldContainer(worldName));
        }
    }

    @Override
    public void saveAll() {
        playerInfo.saveAll();

        for (WorldContainer worldContainer : worldContainers.values()) {
            worldContainer.saveAll();
        }
    }

    @Override
    public void unloadAll() {
        playerInfo.unloadAll();

        for (WorldContainer worldContainer : worldContainers.values()) {
            worldContainer.unloadAll();
        }
    }

    public Reinforcement getReinforcement(final Location location) {
        String worldName = location.getWorld().getName();
        if (!isWorldLoaded(worldName)) {
            return null;
        }

        final Reinforcement reinforcement = worldContainers.get(worldName).getReinforcement(location);
        // TODO: Remove this possibly unecessary double-check.
        reinforcementManager.floorReinforcement(reinforcement, location);
        return reinforcement;
    }

    public void setReinforcement(final Location location, final String playerName, final float value) {
        String worldName = location.getWorld().getName();
        if (!isWorldLoaded(worldName)) {
            return;
        }

        worldContainers.get(worldName).setReinforcement(location, playerName, value, reinforcementManager.getMaterialReinforcementCoefficient(location.getBlock().getType()));
    }

    public void reinforce(final Location location, final String playerName, float value) {
        String worldName = location.getWorld().getName();
        if (!isWorldLoaded(worldName)) {
            return;
        }

        if (reinforcementManager.isReinforced(location)) {
            value += getReinforcement(location).getReinforcementValue();
        }

        worldContainers.get(worldName).setReinforcement(location, playerName, value, reinforcementManager.getMaterialReinforcementCoefficient(location.getBlock().getType()));
    }

    public PlayerInfo getPlayerInfo(final String playerName) {
        PlayerInfo info = playerInfo.get(playerName);
        if (info == null) {
            playerInfo.load(playerName, FACTORY);
            info = playerInfo.get(playerName);
        }

        return info;
    }

    private TypeSafeSet<String> getLoadedWorlds() {
        return worldContainers.keySet();
    }

    public boolean isWorldLoaded(String worldName) {
        return getLoadedWorlds().contains(worldName);
    }

    public void removeReinforcement(final Location location) {
        setReinforcement(location, "", 0);
    }

    public boolean isReinforced(final Location location) {
        final String worldName = location.getWorld().getName();

        // Confirm that the reinforcement list is already tracking the chunk and location.
        if (!isWorldLoaded(worldName)) {
            return false;
        }
        if (!worldContainers.get(worldName).isReinforced(location)) {
            return false;
        }

        return true;
    }

    // package private
    void setReinforcementManager(ReinforcementManager reinforcementManager) {
        this.reinforcementManager = reinforcementManager;
    }

    public ReinforcementManager getReinforcementManager() {
        return reinforcementManager;
    }
}
