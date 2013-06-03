package in.nikitapek.blocksaver.management;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeStorageMap;
import com.amshulman.mbapi.storage.TypeSafeStorageSet;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.mbapi.util.ConstructorFactory;
import com.google.gson.reflect.TypeToken;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import in.nikitapek.blocksaver.util.PlayerInfoConstructorFactory;
import in.nikitapek.blocksaver.util.SupplimentaryTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import java.util.*;
import java.util.Map.Entry;

public final class BlockSaverInfoManager extends InfoManager {
    private static final ConstructorFactory<PlayerInfo> FACTORY = new PlayerInfoConstructorFactory();

    private final TypeSafeStorageMap<PlayerInfo> playerInfo;
    private final TypeSafeStorageSet<Reinforcement> reinforcementSet;
    private final Map<Chunk, List<Location>> reinforcements;
    private final BlockSaverConfigurationContext configurationContext;

    public BlockSaverInfoManager(final ConfigurationContext configurationContext) {
        super(configurationContext);
        this.configurationContext = (BlockSaverConfigurationContext) configurationContext;

        playerInfo = storageManager.getStorageMap("playerInfo", new TypeToken<PlayerInfo>() {}.getType());
        registerPlayerInfoLoader(playerInfo, FACTORY);

        reinforcementSet = storageManager.getStorageSet("reinforcements", SupplimentaryTypes.REINFORCEMENT);
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
        // Saves all of the reinforcements to the reinforcementSet.
        reinforcementSet.clear();
        for (final Entry<Chunk, List<Location>> entry : reinforcements.entrySet()) {
            for (final Location location : entry.getValue()) {
                final Reinforcement reinforcement = getReinforcement(location);
                if (reinforcement != null) {
                    reinforcementSet.add(getReinforcement(location));
                }
            }
        }

        playerInfo.saveAll();
        reinforcementSet.saveAll();
    }

    @Override
    public void unloadAll() {
        // Saves all of the reinforcements to the reinforcementSet.
        reinforcementSet.clear();
        for (final Entry<Chunk, List<Location>> entry : reinforcements.entrySet()) {
            for (final Location location : entry.getValue()) {
                final Reinforcement reinforcement = getReinforcement(location);
                if (reinforcement != null) {
                    reinforcementSet.add(getReinforcement(location));
                }
            }
        }

        playerInfo.unloadAll();
        reinforcementSet.unloadAll();
    }

    public boolean containsReinforcement(Location location) {
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

        // This is commented out to allow Reinforcements to be cloned/moved via WorldEdit.
        /*
         * if (!reinforcements.containsKey(location.getChunk())) {
         * return false;
         * }
         * if (!reinforcements.get(location.getChunk()).contains(location)) {
         * return false;
         * }
         */

        final Block block = location.getBlock();
        if (!block.hasMetadata("RV") || !block.hasMetadata("RTS") || !block.hasMetadata("RJC") || !block.hasMetadata("RCN") || !block.hasMetadata("RLMV")) {
            Reinforcement.removeFromMetadata(block);
            return false;
        }

        // This is only in case WE has moved a reinforcement.
        ensureKeyExists(location);

        return true;
    }

    public Reinforcement getReinforcement(final Location location) {
        if (!containsReinforcement(location)) {
            return null;
        }

        return new Reinforcement(location);
    }

    private void writeReinforcementToMetadata(final Reinforcement reinforcement) {
        if (reinforcement == null) {
            return;
        }

        // Makes sure the Chunk key for the reinforcement already exists, before adding the location.
        ensureKeyExists(reinforcement.getLocation());

        reinforcement.writeToMetadata();
    }

    private void ensureKeyExists(final Location location) {
        // Makes sure the Chunk key for the reinforcement already exists, before adding the location.
        // The if statement is commented out to prevent a SOE because of the ensureKeyExists() check in containsReinforcement().
        // if (!containsReinforcement(location)) {
        if (!reinforcements.containsKey(location.getChunk())) {
            reinforcements.put(location.getChunk(), new ArrayList<Location>());
        }
        reinforcements.get(location.getChunk()).add(location);
        // }
    }

    public void setReinforcement(final Location location, final float value, final String playerName) {
        final Reinforcement reinforcement;

        if (containsReinforcement(location)) {
            reinforcement = getReinforcement(location);
        } else {
            reinforcement = new Reinforcement(location, value, playerName);
        }

        ensureKeyExists(location);
        writeReinforcementToMetadata(reinforcement);
    }

    public void damageBlock(final Location location, final String playerName) {
        final Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null) {
            return;
        }

        // Heals the block if the plugin is configured to do so and the required amount of time has elapsed.
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
        writeReinforcementToMetadata(reinforcement);
    }

    public float removeReinforcement(final Location location) {
        final Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null) {
            return -1;
        }

        if (reinforcements.containsKey(location.getChunk())) {
            reinforcements.get(location.getChunk()).remove(location);
            if (reinforcements.get(location.getChunk()).isEmpty()) {
                reinforcements.remove(location.getChunk());
            }
        }

        final float reinforcementValue = reinforcement.getReinforcementValue();
        Reinforcement.removeFromMetadata(location.getBlock());
        return reinforcementValue;
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
