package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.gson.TypeSafeSetTypeAdapter;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.serialization.ReinforcementTypeAdapter;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

public final class BlockSaverConfigurationContext extends ConfigurationContext {
    // TODO: Find a better way to access configuration values from the Reinforcement class to remove this atrocity.
    public static BlockSaverConfigurationContext configurationContext;
    public static final byte REINFORCEMENT_MAXIMIZING_COEFFICIENT = -2;
    public static final byte NO_REINFORCEMENT_VALUE = -1;

    public Effect reinforcementDamageFailEffect;
    public Effect reinforcementDamageSuccessEffect;
    public Sound reinforceSuccessSound;
    public Sound reinforceFailSound;
    public Sound hitFailSound;

    public final BlockSaverInfoManager infoManager;

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
    public final boolean leaveBlockAfterDeinforce;
    public final boolean mobsInteractWithReinforcedBlocks;
    public final boolean enderdragonInteractWithReinforcedBlocks;
    public final double extinguishChance;
    public final int gracePeriodTime;
    public final int reinforcementHealingTime;

    public final TypeSafeMap<Material, List<Material>> toolRequirements;
    private final TypeSafeMap<Material, Integer> reinforceableBlocks;
    private final TypeSafeMap<Material, Integer> reinforcementBlocks;

    public BlockSaverConfigurationContext(final MbapiPlugin plugin) {
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
        } catch (final IllegalArgumentException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load one or more Effect values. Reverting to defaults.");

            reinforcementDamageFailEffect = Effect.EXTINGUISH;
            reinforcementDamageSuccessEffect = Effect.POTION_BREAK;
            reinforceSuccessSound = Sound.ANVIL_USE;
            reinforceFailSound = Sound.BLAZE_HIT;
            hitFailSound = Sound.CREEPER_DEATH;
        }

        accumulateReinforcementValues = plugin.getConfig().getBoolean("accumulateReinforcementValues", false);
        tntDamagesReinforcedBlocks = plugin.getConfig().getBoolean("tntDamagesReinforcedBlocks", true);
        tntStripReinforcementEntirely = plugin.getConfig().getBoolean("tntStripReinforcementEntirely", false);
        liquidsDestroyReinforcedBlocks = plugin.getConfig().getBoolean("liquidsDestroyReinforcedBlocks", true);
        allowReinforcedBlockPhysics = plugin.getConfig().getBoolean("allowReinforcedBlockPhysics", true);
        fireDamagesReinforcedBlocks = plugin.getConfig().getBoolean("fireDamagesReinforcedBlocks", true);
        extinguishReinforcementFire = plugin.getConfig().getBoolean("extinguishReinforcementFire", true);
        pistonsMoveReinforcedBlocks = plugin.getConfig().getBoolean("pistonsMoveReinforcedBlocks", true);
        useParticleEffects = plugin.getConfig().getBoolean("useParticleEffects", true);
        allowBlockFading = plugin.getConfig().getBoolean("allowBlockFading", false);
        allowReinforcementGracePeriod = plugin.getConfig().getBoolean("allowReinforcementGracePeriod", true);
        allowReinforcementHealing = plugin.getConfig().getBoolean("allowReinforcementHealing", true);
        leaveBlockAfterDeinforce = plugin.getConfig().getBoolean("leaveBlockAfterDeinforce", false);
        mobsInteractWithReinforcedBlocks = plugin.getConfig().getBoolean("mobsInteractWithReinforcedBlocks", false);
        enderdragonInteractWithReinforcedBlocks = plugin.getConfig().getBoolean("enderdragonInteractWithReinforcedBlocks", false);

        // Validates that the extinguish chance is a value from 0.0 to 1.0.
        extinguishChance = (plugin.getConfig().getDouble("extinguishChance", 0.9) < 0 || plugin.getConfig().getDouble("extinguishChance", 0.9) > 1) ? 0.9 : plugin.getConfig().getDouble("extinguishChance", 0.9);

        // Validates that the grace period for reinforcement removal is not less than zero.
        gracePeriodTime = (plugin.getConfig().getInt("gracePeriodTime", 3) < 0) ? 3 : plugin.getConfig().getInt("gracePeriodTime", 3);

        // Validates that the reinforcement healing time is not less than zero.
        reinforcementHealingTime = (plugin.getConfig().getInt("reinforcementHealingTime", 5) < 0) ? 5 : plugin.getConfig().getInt("reinforcementHealingTime", 5);

        ConfigurationSection configSection;

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        try {
            configSection = plugin.getConfig().getConfigurationSection("reinforceableBlocks");

            for (final String materialName : configSection.getKeys(false)) {
                final int value = configSection.getInt(materialName);

                if (value > 0) {
                    reinforceableBlocks.put(Material.getMaterial(materialName), value);
                } else {
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid reinforcement value.");
                }
            }
        } catch (final NullPointerException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforceable blocks list.");
        }

        try {
            configSection = plugin.getConfig().getConfigurationSection("reinforcementBlocks");
            for (final String materialName : configSection.getKeys(false)) {
                final int value = configSection.getInt(materialName);

                if (value > 0 || value == REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
                    reinforcementBlocks.put(Material.getMaterial(materialName), value);
                } else {
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid reinforcement value.");
                }
            }
        } catch (final NullPointerException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforcement blocks list.");
        }

        try {
            configSection = plugin.getConfig().getConfigurationSection("toolRequirements");
            for (final String materialName : configSection.getKeys(false)) {
                final String value = configSection.getString(materialName);
                for (final String split : value.split(",")) {
                    final List<Material> tools = new ArrayList<Material>();
                    tools.add(Material.getMaterial(split));

                    if (toolRequirements.containsKey(Material.getMaterial(materialName))) {
                        tools.addAll(toolRequirements.get(Material.getMaterial(materialName)));
                        toolRequirements.remove(Material.getMaterial(materialName));
                    }

                    toolRequirements.put(Material.getMaterial(materialName), tools);
                }
            }
        } catch (final NullPointerException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforcement blocks list.");
        }
    }

    public boolean isMaterialReinforceable(final Material material) {
        return reinforceableBlocks.containsKey(material);
    }

    public boolean isReinforceable(final Block block) {
        final int coefficient = getMaterialReinforcementCoefficient(block.getType());

        // If the block's material cannot be reinforced, the reinforcement fails.
        if (coefficient == -1) {
            return false;
        }

        // Retrieves the reinforcement on the block, if the reinforcement exists.
        final Reinforcement reinforcement = infoManager.getReinforcement(block.getLocation());

        // If the block is not reinforced, it can be reinforced further.
        if (reinforcement == null) {
            return true;
        }

        final int currentReinforcementValue = reinforcement.getReinforcementValue();

        // If reinforcement values are being accumulated, the RV cannot have reached RVC, and therefore the block is reinforceable.
        if (accumulateReinforcementValues) {
            return true;
        }

        // If reinforcement values are being capped, and the RV is already at RVC, the block cannot be reinforced further.
        return currentReinforcementValue < coefficient;
    }

    public boolean isReinforcingMaterial(final Material material) {
        return reinforcementBlocks.containsKey(material);
    }

    public int getMaterialReinforcementCoefficient(final Material material) {
        return isMaterialReinforceable(material) ? reinforceableBlocks.get(material) : NO_REINFORCEMENT_VALUE;
    }

    public boolean attemptReinforcement(final Block block, final Material reinforcementMaterial, final String playerName) {
        // If the material cannot be used for reinforcement, the reinforcement fails.
        if (!reinforcementBlocks.containsKey(reinforcementMaterial)) {
            return false;
        }

        if (!isReinforceable(block)) {
            return false;
        }

        // Retrieves the reinforcement on the block, if the reinforcement exists.
        final Reinforcement reinforcement = infoManager.getReinforcement(block.getLocation());
        final int currentReinforcementValue;

        // If the block is not reinforced, we must designate the coefficient as such.
        if (reinforcement == null) {
            currentReinforcementValue = -1;
        } else {
            currentReinforcementValue = reinforcement.getReinforcementValue();
        }

        final int coefficient = getMaterialReinforcementCoefficient(block.getType());

        // Retrieves the amount the material will reinforce the block by.
        int additionalReinforcementValue = reinforcementBlocks.get(reinforcementMaterial);

        // If the material being used to reinforce has a reinforcement maximizing coefficient, then we want to set the block to its maximum possible enforcement.
        if (additionalReinforcementValue == REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
            additionalReinforcementValue = coefficient;

            // If there is no reinforcement value cap, then we cannot set the block to its maximum reinforcement, therefore the reinforcement fails.
            if (accumulateReinforcementValues) {
                return false;
            }
        }

        // If the block is currently reinforced, we add the current reinforcement value to the value to reinforce the block by.
        if (currentReinforcementValue != -1) {
            additionalReinforcementValue += currentReinforcementValue;
        }

        // If we are accumulating reinforcement values, the block's reinforcement is increased by the additionalReinforcementValue which is simply the additional protection of the material being used added to the current reinforcement value of the block.
        // Otherwise, we simply attempt to increase the block's reinforcement by the amount provided by the material.
        if (accumulateReinforcementValues) {
            infoManager.setReinforcement(block.getLocation(), additionalReinforcementValue, playerName);
        } else {
            infoManager.setReinforcement(block.getLocation(), Math.min(additionalReinforcementValue, coefficient), playerName);
        }

        return true;
    }

    public boolean canToolBreakBlock(final Material block, final Material tool) {
        if (!toolRequirements.containsKey(block)) {
            return false;
        }

        for (final Entry<Material, List<Material>> material : toolRequirements.entrySet()) {
            if (!material.getKey().equals(block)) {
                continue;
            }

            if (!material.getValue().contains(tool)) {
                return false;
            }
        }

        return true;
    }
}
