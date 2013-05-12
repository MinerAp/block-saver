package in.nikitapek.blocksaver.management;

import org.bukkit.Location;

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
        if (getReinforcementValue(location) > 1) {
            // TODO: Ask Andy if you can just modify the get() value after retrieval or is this put() necessary?
            setReinforcement(location, (getReinforcementValue(location) - 1));
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
