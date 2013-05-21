package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.serialization.ReinforcementTypeAdapter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Location;
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
    // TODO: Find a better way to access configuration values from the Reinforcement class to remove this atrocity.
    public static BlockSaverConfigurationContext configurationContext;

    private final TypeSafeMap<Material, Integer> reinforceableBlocks;
    private final TypeSafeMap<Material, Integer> reinforcementBlocks;
    public final TypeSafeMap<Material, List<Material>> toolRequirements;

    public Effect reinforcementDamageFailEffect;
    public Effect reinforcementDamageSuccessEffect;
    public Sound reinforceSuccessSound;
    public Sound reinforceFailSound;
    public Sound hitFailSound;

    public final boolean accumulateReinforcementValues;
    public final boolean tntDamagesReinforcedBlocks;
    public final boolean tntStripReinforcementEntirely;
    public final boolean liquidsDestroyReinforcedBlocks;
    public final boolean allowReinforcedBlockPhysics;
    public final boolean fireDamagesReinforcedBlocks;
    public final boolean extinguishReinforcementFire;
    public final boolean pistonsMoveReinforcedBlocks;
    public final boolean useParticleEffects;
    public final boolean allowBlockFading;
    public final boolean allowReinforcementGracePeriod;
    public final boolean allowReinforcementHealing;

    public final double extinguishChance;

    public final int gracePeriodTime;
    public final int reinforcementHealingTime;

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin, new TypeSafeSetTypeAdapter<Reinforcement>(SupplimentaryTypes.TREESET, SupplimentaryTypes.REINFORCEMENT), new ReinforcementTypeAdapter());
        configurationContext = this;

        infoManager = new BlockSaverInfoManager(this);

        reinforceableBlocks = new TypeSafeMapImpl<Material, Integer>(new EnumMap<Material, Integer>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.INTEGER);
        reinforcementBlocks = new TypeSafeMapImpl<Material, Integer>(new EnumMap<Material, Integer>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.INTEGER);
        toolRequirements = new TypeSafeMapImpl<Material, List<Material>>(new EnumMap<Material, List<Material>>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.LIST);

        plugin.saveDefaultConfig();

        try {
            // Note: setting the default values here might be unnecessary because if the config values fail to load and it defaults to these, they will never result in an exception.
            reinforcementDamageFailEffect = Effect.valueOf(plugin.getConfig().getString("reinforcementDamageFailEffect", Effect.EXTINGUISH.toString()));
            reinforcementDamageSuccessEffect = Effect.valueOf(plugin.getConfig().getString("reinforcementDamageSuccessEffect", Effect.POTION_BREAK.toString()));
            reinforceSuccessSound = Sound.valueOf(plugin.getConfig().getString("reinforceSuccessSound", Sound.ANVIL_USE.toString()));
            reinforceFailSound = Sound.valueOf(plugin.getConfig().getString("reinforceFailSound", Sound.BLAZE_HIT.toString()));
            hitFailSound = Sound.valueOf(plugin.getConfig().getString("hitFailSound", Sound.CREEPER_DEATH.toString()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load one or more Effect values. Reverting to defaults.");

            reinforcementDamageFailEffect = Effect.EXTINGUISH;
            reinforcementDamageSuccessEffect = Effect.POTION_BREAK;
            reinforceSuccessSound = Sound.ANVIL_USE;
            reinforceFailSound = Sound.BLAZE_HIT;
            hitFailSound = Sound.CREEPER_DEATH;
        }

        accumulateReinforcementValues = plugin.getConfig().getBoolean("accumulateReinforcementValues", true);
        tntDamagesReinforcedBlocks = plugin.getConfig().getBoolean("tntDamagesReinforcedBlocks", true);
        tntStripReinforcementEntirely = plugin.getConfig().getBoolean("tntStripReinforcementEntirely", true);
        liquidsDestroyReinforcedBlocks = plugin.getConfig().getBoolean("liquidsDestroyReinforcedBlocks", true);
        allowReinforcedBlockPhysics = plugin.getConfig().getBoolean("allowReinforcedBlockPhysics", true);
        fireDamagesReinforcedBlocks = plugin.getConfig().getBoolean("fireDamagesReinforcedBlocks", true);
        extinguishReinforcementFire = plugin.getConfig().getBoolean("extinguishReinforcementFire", true);
        pistonsMoveReinforcedBlocks = plugin.getConfig().getBoolean("pistonsMoveReinforcedBlocks", true);
        useParticleEffects = plugin.getConfig().getBoolean("useParticleEffects", true);
        allowBlockFading = plugin.getConfig().getBoolean("allowBlockFading", true);
        allowReinforcementGracePeriod = plugin.getConfig().getBoolean("allowReinforcementGracePeriod", true);
        allowReinforcementHealing = plugin.getConfig().getBoolean("allowReinforcementHealing", true);

        // Validates that the extinguish chance is a value from 0.0 to 1.0.
        extinguishChance = (plugin.getConfig().getDouble("extinguishChance", 0.8) < 0 || plugin.getConfig().getDouble("extinguishChance", 0.8) > 1) ? 0.8 : plugin.getConfig().getDouble("extinguishChance", 0.8);

        // Validates that the grace period for reinforcement removal is not less than zero.
        gracePeriodTime = (plugin.getConfig().getInt("gracePeriodTime", 3) < 0) ? 3 : plugin.getConfig().getInt("gracePeriodTime", 3);

        // Validates that the reinforcement healing time is not less than zero.
        reinforcementHealingTime = (plugin.getConfig().getInt("reinforcementHealingTime", 5) < 0) ? 5 : plugin.getConfig().getInt("reinforcementHealingTime", 5);

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

        try {
            configSection = plugin.getConfig().getConfigurationSection("toolRequirements");
            for (String materialName : configSection.getKeys(false)) {
                String value = configSection.getString(materialName);
                for (String split : value.split(",")) {
                    List<Material> tools = new ArrayList<Material>();
                    tools.add(Material.getMaterial(split));

                    if (toolRequirements.containsKey(Material.getMaterial(materialName))) {
                        tools.addAll(toolRequirements.get(Material.getMaterial(materialName)));
                        toolRequirements.remove(Material.getMaterial(materialName));
                    }

                    toolRequirements.put(Material.getMaterial(materialName), tools);
                }
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

    public boolean attemptReinforcement(Block block, Material reinforcement, String playerName) {
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
            infoManager.setReinforcement(block.getLocation(), additionalReinforcementValue, playerName);
        else {
            infoManager.setReinforcement(block.getLocation(), Math.min(additionalReinforcementValue, coefficient), playerName);
        }

        return true;
    }

    public boolean isReinforced(Location location) {
        return infoManager.getReinforcementValue(location) == -1 ? false : true;
    }

    public boolean canToolBreakBlock(Material block, Material tool) {
        if (!toolRequirements.containsKey(block))
            return false;

        for (Entry<Material, List<Material>> material : toolRequirements.entrySet()) {
            if (!material.getKey().equals(block))
                continue;

            if (!material.getValue().contains(tool)) {
                return false;
            }
        }

        return true;
    }
}
