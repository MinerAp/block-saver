package in.nikitapek.blocksaver.management;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeDistributedStorageMap;
import com.amshulman.mbapi.storage.TypeSafeUnifiedStorageMap;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.TypeSafeSet;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

import in.nikitapek.blocksaver.serialization.LocationTypeAdapter;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplementaryTypes;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;

public final class BlockSaverInfoManager extends InfoManager {
    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    private final TypeSafeDistributedStorageMap<PlayerInfo> playerInfo;

    private final TypeSafeMap<String, TypeSafeDistributedStorageMap<TypeSafeUnifiedStorageMap<Location, Reinforcement>>> worlds;

    private ReinforcementManager reinforcementManager;

    public BlockSaverInfoManager(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext);

        playerInfo = storageManager.getDistributedStorageMap("playerInfo", SupplementaryTypes.PLAYER_INFO);
        registerPlayerInfoLoader(playerInfo, FACTORY);

        worlds = new TypeSafeMapImpl<>(new HashMap<String, TypeSafeDistributedStorageMap<TypeSafeUnifiedStorageMap<Location, Reinforcement>>>(), SupplementaryTypes.STRING, SupplementaryTypes.TYPE_SAFE_DISTRIBUTED_STORAGE_MAP);

        for (String worldName : configurationContext.worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            TypeSafeDistributedStorageMap<TypeSafeUnifiedStorageMap<Location, Reinforcement>> reinforcements = storageManager.getDistributedStorageMap(worldName, SupplementaryTypes.REINFORCEMENT_STORAGE);
            LocationTypeAdapter.currentWorld = worldName;
            reinforcements.loadAll();

            worlds.put(worldName, reinforcements);
        }
    }

    @Override
    public void saveAll() {
        playerInfo.saveAll();

        for (TypeSafeDistributedStorageMap<TypeSafeUnifiedStorageMap<Location, Reinforcement>> world : worlds.values()) {
            world.saveAll();
        }
    }

    @Override
    public void unloadAll() {
        playerInfo.unloadAll();

        for (TypeSafeDistributedStorageMap<TypeSafeUnifiedStorageMap<Location, Reinforcement>> world : worlds.values()) {
            world.unloadAll();
        }
    }

    public Reinforcement getReinforcement(final Location location) {
        if (!isWorldLoaded(location.getWorld().getName())) {
            return null;
        }

        TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location);

        if (reinforcementMap == null) {
            return null;
        }

        return reinforcementMap.get(location);
    }

    public void reinforce(final Location location, final String playerName, boolean removing) {
        if (!isWorldLoaded(location.getWorld().getName())) {
            return;
        }

        if (removing) {
            removeReinforcement(location);
        } else {
	        if (!isReinforced(location)) {
	            ensureMapExists(location);
	            getReinforcementMap(location).put(location, new Reinforcement(playerName));
	        }
        }
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
        return worlds.keySet();
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
        ensureMapExists(toLocation);
        TypeSafeUnifiedStorageMap<Location, Reinforcement> fromReinforcementMap = getReinforcementMap(fromLocation);
        TypeSafeUnifiedStorageMap<Location, Reinforcement> toReinforcementMap = getReinforcementMap(toLocation);
        toReinforcementMap.put(toLocation, fromReinforcementMap.remove(fromLocation));
    }

    public void removeReinforcement(final Location location) {
        String worldName = location.getWorld().getName();
        if (!isWorldLoaded(worldName)) {
            return;
        }

        String regionName = getRegionNameFromChunk(location.getChunk());

        TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = worlds.get(worldName).get(regionName);
        reinforcementMap.remove(location);
    }

    boolean isReinforced(final Location location) {
        // Confirm that the reinforcement list is already tracking the chunk and location.
        if (!isWorldLoaded(location.getWorld().getName())) {
            return false;
        }

        TypeSafeUnifiedStorageMap<Location, Reinforcement> reinforcementMap = getReinforcementMap(location);

        if (reinforcementMap == null) {
            return false;
        }

        return reinforcementMap.containsKey(location);
    }

    void setReinforcementManager(ReinforcementManager reinforcementManager) {
        this.reinforcementManager = reinforcementManager;
    }

    public ReinforcementManager getReinforcementManager() {
        return reinforcementManager;
    }

    private static String getRegionNameFromChunk(final Chunk chunk) {
        return (chunk.getX() >> 5) + "." + (chunk.getZ() >> 5);
    }

    private TypeSafeUnifiedStorageMap<Location, Reinforcement> getReinforcementMap(Location location) {
        return worlds.get(location.getWorld().getName()).get(getRegionNameFromChunk(location.getChunk()));
    }

    private void ensureMapExists(Location location) {
        if (getReinforcementMap(location) == null) {
            worlds.get(location.getWorld().getName()).putTypeSafeUnifiedStorageMap(getRegionNameFromChunk(location.getChunk()), SupplementaryTypes.LOCATION, SupplementaryTypes.REINFORCEMENT);
        }
    }
}
