package in.nikitapek.blocksaver.management;

import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplimentaryTypes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;
import com.amshulman.mbapi.storage.TypeSafeStorageSet;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.amshulman.typesafety.TypeSafeSet;
import com.google.gson.reflect.TypeToken;

public final class BlockSaverInfoManager extends InfoManager {
    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    private final TypeSafeStorageMap<PlayerInfo> playerInfo;
    private final TypeSafeStorageSet<Reinforcement> reinforcements;
    private final BlockSaverConfigurationContext configurationContext;

    public BlockSaverInfoManager(final ConfigurationContext configurationContext) {
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

    public int getReinforcementValue(final Location location) {
        final Reinforcement reinforcement = getReinforcement(location);
        return (reinforcement == null) ? -1 : reinforcement.getReinforcementValue();
    }

    public Reinforcement getReinforcement(Location location) {
        // If a part of the piston was damaged, the rest should be damaged too.
        if (location.getBlock().getType().equals(Material.PISTON_EXTENSION)) {
            final MaterialData data = location.getBlock().getState().getData();
            BlockFace direction = null;

            // Check the block it pushed directly
            if (data instanceof PistonExtensionMaterial) {
                direction = ((PistonExtensionMaterial) data).getFacing();
            }

            if (direction != null) {
                location = location.getBlock().getRelative(direction.getOppositeFace()).getLocation();
            }
        }

        for (final Reinforcement reinforcement : reinforcements) {
            if (reinforcement.getLocation().equals(location)) {
                return reinforcement;
            }
        }

        return null;
    }

    public void setReinforcement(final Location location, final int value, final String playerName) {
        final Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null) {
            reinforcements.add(new Reinforcement(location, value, playerName));
            return;
        }

        reinforcement.setReinforcementValue(value);
    }

    public void damageBlock(final Location location, final String playerName) {
        final Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null) {
            return;
        }

        // Heals the block is the plugin is configured to do so and the required amount of time elapsed.
        if (configurationContext.allowReinforcementHealing) {
            if ((System.currentTimeMillis() - reinforcement.getTimeStamp()) >= (configurationContext.reinforcementHealingTime * BlockSaverUtil.MILLISECONDS_PER_SECOND)) {
                reinforcement.setReinforcementValue(reinforcement.getLastMaximumValue());
            }
        }

        if (reinforcement.getReinforcementValue() <= 1 || !isFortified(reinforcement, playerName)) {
            removeReinforcement(location);
            return;
        }

        reinforcement.setReinforcementValue(reinforcement.getReinforcementValue() - 1);
        return;
    }

    public int removeReinforcement(final Location location) {
        final Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null) {
            return -1;
        }

        reinforcements.remove(reinforcement);

        return reinforcement.getReinforcementValue();
    }

    public boolean isFortified(final Reinforcement reinforcement, final String playerName) {
        if (!configurationContext.allowReinforcementGracePeriod) {
            return true;
        }

        if (reinforcement == null || playerName == null) {
            return true;
        }

        if (!reinforcement.isJustCreated()) {
            return true;
        }

        if (!reinforcement.getCreatorName().equals(playerName)) {
            return true;
        }

        return (System.currentTimeMillis() - reinforcement.getTimeStamp() > (configurationContext.gracePeriodTime * BlockSaverUtil.MILLISECONDS_PER_SECOND));
    }

    public PlayerInfo getPlayerInfo(final String playerName) {
        PlayerInfo info = playerInfo.get(playerName);
        if (info == null) {
            playerInfo.load(playerName, FACTORY);
            info = playerInfo.get(playerName);
        }

        return info;
    }
}
