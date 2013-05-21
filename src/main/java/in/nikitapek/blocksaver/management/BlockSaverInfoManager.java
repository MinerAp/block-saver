package in.nikitapek.blocksaver.management;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplimentaryTypes;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;
import com.amshulman.mbapi.storage.TypeSafeStorageSet;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.amshulman.typesafety.TypeSafeSet;
import com.google.gson.reflect.TypeToken;

public class BlockSaverInfoManager extends InfoManager {
    private final TypeSafeStorageMap<PlayerInfo> playerInfo;
    private final TypeSafeStorageSet<Reinforcement> reinforcements;
    private final BlockSaverConfigurationContext configurationContext;

    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    public BlockSaverInfoManager(ConfigurationContext configurationContext) {
        super(configurationContext);
        this.configurationContext = (BlockSaverConfigurationContext) configurationContext;

        playerInfo = storageManager.getStorageMap("playerInfo", new TypeToken<PlayerInfo>() {}.getType());
        registerPlayerInfoLoader(playerInfo, FACTORY);

        reinforcements = storageManager.getStorageSet("reinforcements", SupplimentaryTypes.REINFORCEMENT);
        reinforcements.load();
    }

    @Override
    public void saveAll() {
        playerInfo.saveAll();
        reinforcements.saveAll();
    }

    @Override
    public void unloadAll() {
        playerInfo.unloadAll();
        reinforcements.unloadAll();
    }

    public TypeSafeSet<Reinforcement> getReinforcements() {
        return reinforcements;
    }

    public int getReinforcementValue(Location location) {
        Reinforcement reinforcement = getReinforcement(location);
        return (reinforcement == null) ? -1 : reinforcement.getReinforcementValue();
    }

    public Reinforcement getReinforcement(Location location) {
        // If a part of the piston was damaged, the rest should be damaged too.
        if (location.getBlock().getType().equals(Material.PISTON_EXTENSION)) {
            MaterialData data = location.getBlock().getState().getData();
            BlockFace direction = null;

            // Check the block it pushed directly
            if (data instanceof PistonExtensionMaterial) {
                direction = ((PistonExtensionMaterial) data).getFacing();
            }

            if (direction != null)
                location = location.getBlock().getRelative(direction.getOppositeFace()).getLocation();
        }

        for (Reinforcement reinforcement : reinforcements) {
            if (reinforcement.getLocation().equals(location))
                return reinforcement;
        }

        return null;
    }

    public void setReinforcement(Location location, int value, String playerName) {
        Reinforcement reinforcement = getReinforcement(location);
        
        if (reinforcement == null) {
            reinforcements.add(new Reinforcement(location, value, playerName));
            return;
        }

        reinforcement.setReinforcementValue(value);
        reinforcement.updateTimeStamp();
    }

    public void damageBlock(Location location, String playerName) {
        Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null)
            return;

        // Heals the block is the plugin is configured to do so and the required amount of time elapsed.
        if (configurationContext.allowReinforcementHealing)
            if ((System.currentTimeMillis() - reinforcement.getTimeStamp()) >= (configurationContext.reinforcementHealingTime * 1000))
                reinforcement.setReinforcementValue(reinforcement.getLastMaximumValue());

        if (reinforcement.getReinforcementValue() <= 1 || !isFortified(reinforcement, playerName)) {
            removeReinforcement(location);
            return;
        }

        // TODO: Ask Andy if you can just modify the get() value after retrieval or is this put() necessary?
        setReinforcement(reinforcement.getLocation(), (reinforcement.getReinforcementValue() - 1), reinforcement.getCreatorName());
        return;
    }

    public int removeReinforcement(Location location) {
        Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null)
            return -1;

        reinforcements.remove(reinforcement);

        return reinforcement.getReinforcementValue();
    }

    public boolean isFortified(Reinforcement reinforcement, String playerName) {
        if (!configurationContext.allowReinforcementGracePeriod)
            return true;
        
        if (reinforcement == null || playerName == null)
            return true;

        if (!reinforcement.isJustCreated())
            return true;

        if (!reinforcement.getCreatorName().equals(playerName))
            return true;

        return (System.currentTimeMillis() - reinforcement.getTimeStamp() > (configurationContext.gracePeriodTime * 1000));    
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        PlayerInfo info = playerInfo.get(playerName);
        if (info == null) {
            playerInfo.load(playerName, FACTORY);
            info = playerInfo.get(playerName);
        }

        return info;
    }
}
