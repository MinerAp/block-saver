package in.nikitapek.blocksaver.management;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.SupplimentaryTypes;

import com.amshulman.mbapi.management.InfoManager;
import com.amshulman.mbapi.storage.TypeSafeStorageSet;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeSet;

public class BlockSaverInfoManager extends InfoManager {
    private final TypeSafeStorageSet<Reinforcement> reinforcements;

    public BlockSaverInfoManager(ConfigurationContext configurationContext) {
        super(configurationContext);

        reinforcements = storageManager.getStorageSet("reinforcements", SupplimentaryTypes.REINFORCEMENT);

        reinforcements.load();
    }

    @Override
    public void saveAll() {
        reinforcements.saveAll();
    }

    @Override
    public void unloadAll() {
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

    public void setReinforcement(Location location, int value) {
        removeReinforcement(location);

        reinforcements.add(new Reinforcement(location, value));
    }

    public void damageBlock(Location location) {
        Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null)
            return;

        if (reinforcement.getReinforcementValue() > 1) {
            // TODO: Ask Andy if you can just modify the get() value after retrieval or is this put() necessary?
            setReinforcement(reinforcement.getLocation(), (reinforcement.getReinforcementValue() - 1));
            return;
        }

        removeReinforcement(location);
    }

    public int removeReinforcement(Location location) {
        Reinforcement reinforcement = getReinforcement(location);

        if (reinforcement == null)
            return -1;

        reinforcements.remove(reinforcement);

        return reinforcement.getReinforcementValue();
    }
}
