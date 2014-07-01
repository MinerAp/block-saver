package in.nikitapek.blocksaver.management;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeDistributedStorageMap;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.TypeSafeSet;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.serialization.WorldContainer;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplementaryTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;

public final class BlockSaverInfoManager extends InfoManager {
    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    private final TypeSafeDistributedStorageMap<PlayerInfo> playerInfo;

    private final TypeSafeMap<String, WorldContainer> worldContainers;

    private ReinforcementManager reinforcementManager;

    public BlockSaverInfoManager(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext);

        playerInfo = storageManager.getDistributedStorageMap("playerInfo", SupplementaryTypes.PLAYER_INFO);
        registerPlayerInfoLoader(playerInfo, FACTORY);

        WorldContainer.initialize(storageManager);
        worldContainers = new TypeSafeMapImpl<>(new HashMap<String, WorldContainer>(), SupplementaryTypes.STRING, SupplementaryTypes.WORLD_CONTAINER);

        for (String worldName : configurationContext.worlds) {
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

        return reinforcement;
    }

    public void reinforce(final Location location, final String playerName, float value) {
        String worldName = location.getWorld().getName();
        if (!isWorldLoaded(worldName)) {
            return;
        }

        float coefficient = reinforcementManager.getMaterialReinforcementCoefficient(location.getBlock().getType());
        if (reinforcementManager.isReinforced(location)) {
            value += getReinforcement(location).getReinforcementValue(coefficient);
        }

        worldContainers.get(worldName).setReinforcement(location, playerName, value, coefficient);
    }

    public PlayerInfo getPlayerInfo(final Player player) {
        String playerUUID = player.getUniqueId().toString();
        PlayerInfo info = playerInfo.get(playerUUID);
        if (info == null) {
            playerInfo.load(playerUUID, FACTORY);
            info = playerInfo.get(playerUUID);
        }

        return info;
    }

    private TypeSafeSet<String> getLoadedWorlds() {
        return worldContainers.keySet();
    }

    public boolean isWorldLoaded(String worldName) {
        return getLoadedWorlds().contains(worldName);
    }

    /**
     * Moves a reinforcement from one location to another location, without modifying the reinforcement object.
     *
     * @param fromLocation the initial location of the reinforcement.
     * @param toLocation   the target location of the reinforcement.
     */
    public void moveReinforcement(Location fromLocation, Location toLocation) {
        worldContainers.get(fromLocation.getWorld().getName()).moveReinforcement(fromLocation, toLocation);
    }

    public void removeReinforcement(final Location location) {
        String worldName = location.getWorld().getName();
        if (!isWorldLoaded(worldName)) {
            return;
        }

        worldContainers.get(worldName).removeReinforcement(location);
    }

    public boolean isReinforced(final Location location) {
        final String worldName = location.getWorld().getName();

        // Confirm that the reinforcement list is already tracking the chunk and location.
        if (!isWorldLoaded(worldName)) {
            return false;
        }

        return worldContainers.get(worldName).isReinforced(location);
    }

    // package private
    void setReinforcementManager(ReinforcementManager reinforcementManager) {
        this.reinforcementManager = reinforcementManager;
    }

    public ReinforcementManager getReinforcementManager() {
        return reinforcementManager;
    }
}
