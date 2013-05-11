package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.serialization.ReinforcementTypeAdapter;

import java.util.EnumMap;
import java.util.logging.Level;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.gson.TypeSafeSetTypeAdapter;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public class BlockSaverConfigurationContext extends ConfigurationContext {
    public final BlockSaverInfoManager infoManager;

    private final TypeSafeMap<Material, Integer> reinforceableBlocks;
    private final TypeSafeMap<Material, Integer> reinforcementBlocks;

    public Effect blockBreakFailEffect;
    public Effect blockReinforcementDamageEffect;
    public Sound blockReinforceSound;

    public final boolean accumulateReinforcementValues;
    public final boolean tntDamagesReinforcedBlocks;
    public final boolean fireDamagesReinforcedBlocks;
    public final boolean pistonsMoveReinforcedBlocks;

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin, new TypeSafeSetTypeAdapter<Reinforcement>(SupplimentaryTypes.TREESET, SupplimentaryTypes.REINFORCEMENT), new ReinforcementTypeAdapter());

        infoManager = new BlockSaverInfoManager(this);

        reinforceableBlocks = new TypeSafeMapImpl<Material, Integer>(new EnumMap<Material, Integer>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.INTEGER);
        reinforcementBlocks = new TypeSafeMapImpl<Material, Integer>(new EnumMap<Material, Integer>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.INTEGER);

        plugin.saveDefaultConfig();

        try {
            // Note: setting the default values here might be unnecessary because if the config values fail to load and it defaults to these, they will never result in an exception.
            blockBreakFailEffect = Effect.valueOf(plugin.getConfig().getString("blockBreakFailEffect", Effect.EXTINGUISH.toString()));
            blockReinforcementDamageEffect = Effect.valueOf(plugin.getConfig().getString("blockReinforcementDamageEffect", Effect.POTION_BREAK.toString()));
            blockReinforceSound = Sound.valueOf(plugin.getConfig().getString("blockReinforceSound", Sound.ANVIL_USE.toString()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load one or more Effect values. Reverting to defaults.");

            blockBreakFailEffect = Effect.EXTINGUISH;
            blockReinforcementDamageEffect = Effect.POTION_BREAK;
            blockReinforceSound = Sound.ANVIL_USE;
        }

        accumulateReinforcementValues = plugin.getConfig().getBoolean("accumulateReinforcementValues", true);
        tntDamagesReinforcedBlocks = plugin.getConfig().getBoolean("tntDamagesReinforcedBlocks", true);
        fireDamagesReinforcedBlocks = plugin.getConfig().getBoolean("fireDamagesReinforcedBlocks", true);
        pistonsMoveReinforcedBlocks = plugin.getConfig().getBoolean("pistonsMoveReinforcedBlocks", true);

        ConfigurationSection configSection;

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        try {
            configSection = plugin.getConfig().getConfigurationSection("reinforceableBlocks");
            for (String materialName : configSection.getKeys(false)) {
                int value = configSection.getInt(materialName);

                if (value > 0)
                    reinforceableBlocks.put(Material.getMaterial(materialName), value);
                else
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid reinforcement value.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforceable blocks list.");
            e.printStackTrace();
        }

        try {
            configSection = plugin.getConfig().getConfigurationSection("reinforcementBlocks");
            for (String materialName : configSection.getKeys(false)) {
                int value = configSection.getInt(materialName);

                if (value > 0 || value == -2)
                    reinforcementBlocks.put(Material.getMaterial(materialName), value);
                else
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid reinforcement value.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforcement blocks list.");
            e.printStackTrace();
        }
    }

    public boolean isMaterialReinforceable(Material material) {
        return reinforceableBlocks.containsKey(material);
    }

    public boolean isReinforceable(Block block) {
        int coefficient = getMaterialReinforcementCoefficient(block.getType());

        return coefficient != -1 ? infoManager.getReinforcementValue(block.getLocation()) < coefficient : false;
    }

    public boolean isReinforcingMaterial(Material material) {
        return reinforcementBlocks.containsKey(material);
    }

    public int getMaterialReinforcementCoefficient(Material material) {
        return isMaterialReinforceable(material) ? reinforceableBlocks.get(material) : -1;
    }

    public boolean attemptReinforcement(Block block, Material reinforcement) {
        // Retrieves the maximum reinforcement value the block being reinforced can have.
        int coefficient = getMaterialReinforcementCoefficient(block.getType());

        // If the block cannot be reinforced, the reinforcement fails.
        if (coefficient == -1)
            return false;

        // Retrieves the current reinforcement value of the block (if it is reinforced).
        int currentReinforcementValue = infoManager.getReinforcementValue(block.getLocation());

        // If reinforcement values are being capped, and the block is already at maximum reinforcement, the reinforcement fails.
        if (!accumulateReinforcementValues && currentReinforcementValue >= coefficient)
            return false;

        // If the material cannot be used for reinforcement, the reinforcement fails.
        if (!reinforcementBlocks.containsKey(reinforcement))
            return false;

        // Retrieves the amount the material will reinforce the block by.
        int additionalReinforcementValue = reinforcementBlocks.get(reinforcement);

        // If the material being used to reinforce has a reinforcement value of -2, then we want to set the block to its maximum possible enforcement.
        if (additionalReinforcementValue == -2) {
            additionalReinforcementValue = coefficient;

            // If there is no reinforcement value cap, then we cannot set the block to its maximum reinforcement, therefore the reinforcement fails.
            if (accumulateReinforcementValues)
                return false;
        }

        // If the block is currently reinforced, we add the current reinforcement value to the value to reinforce the block by.
        if (currentReinforcementValue != -1)
            additionalReinforcementValue += currentReinforcementValue;

        // If we are accumulating reinforcement values, the block's reinforcement is increased by the additionalReinforcementValue which is simply the additional protection of the material being used added to the current reinforcement value of the block.
        // Otherwise, we simply attempt to increase the block's reinforcement by the amount provided by the material.
        if (accumulateReinforcementValues)
            infoManager.setReinforcement(block.getLocation(), additionalReinforcementValue);
        else {
            infoManager.setReinforcement(block.getLocation(), Math.min(additionalReinforcementValue, coefficient));
        }

        return true;
    }
}
