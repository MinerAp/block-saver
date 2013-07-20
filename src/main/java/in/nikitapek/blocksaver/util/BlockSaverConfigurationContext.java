package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.gson.TypeSafeSetTypeAdapter;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.FeedbackManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.serialization.ReinforcementTypeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;

public final class BlockSaverConfigurationContext extends ConfigurationContext {
    private final static double EXTINGUISH_CHANCE = 0.9;

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
    public final boolean allowBlockFading;
    public final boolean allowReinforcementGracePeriod;
    public final boolean allowReinforcementHealing;
    public final boolean leaveBlockAfterDeinforce;
    public final boolean mobsInteractWithReinforcedBlocks;
    public final boolean enderdragonInteractWithReinforcedBlocks;
    public final boolean enableLogging;
    public final double extinguishChance;
    public final int gracePeriodTime;
    public final int reinforcementHealingTime;
    public final String primaryFeedback;

    public final BlockSaverInfoManager infoManager;
    public final FeedbackManager feedbackManager;

    public final TypeSafeMap<Material, Integer> reinforceableBlocks;
    public final TypeSafeMap<Material, Integer> reinforcementBlocks;
    public final TypeSafeMap<Material, List<Integer>> toolRequirements;

    private final ReinforcementManager reinforcementManager;

    public BlockSaverConfigurationContext(final MbapiPlugin plugin) {
        super(plugin, new TypeSafeSetTypeAdapter<Reinforcement>(SupplementaryTypes.TREESET, SupplementaryTypes.REINFORCEMENT), new ReinforcementTypeAdapter());

        reinforceableBlocks = new TypeSafeMapImpl<Material, Integer>(new EnumMap<Material, Integer>(Material.class), SupplementaryTypes.MATERIAL, SupplementaryTypes.INTEGER);
        reinforcementBlocks = new TypeSafeMapImpl<Material, Integer>(new EnumMap<Material, Integer>(Material.class), SupplementaryTypes.MATERIAL, SupplementaryTypes.INTEGER);
        toolRequirements = new TypeSafeMapImpl<Material, List<Integer>>(new EnumMap<Material, List<Integer>>(Material.class), SupplementaryTypes.MATERIAL, SupplementaryTypes.LIST);

        plugin.saveDefaultConfig();

        try {
            // Note: setting the default values here might be unnecessary because if the config values fail to load and it defaults to these, they will never result in an exception.
            reinforcementDamageFailEffect = Effect.valueOf(plugin.getConfig().getString("reinforcementDamageFailEffect", Effect.EXTINGUISH.toString()));
            reinforcementDamageSuccessEffect = Effect.valueOf(plugin.getConfig().getString("reinforcementDamageSuccessEffect", Effect.POTION_BREAK.toString()));
            reinforceSuccessSound = Sound.valueOf(plugin.getConfig().getString("reinforceSuccessSound", Sound.ANVIL_USE.toString()));
            reinforceFailSound = Sound.valueOf(plugin.getConfig().getString("reinforceFailSound", Sound.BLAZE_HIT.toString()));
            hitFailSound = Sound.valueOf(plugin.getConfig().getString("hitFailSound", Sound.CREEPER_DEATH.toString()));
        }
        catch (final IllegalArgumentException ex) {
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
        allowBlockFading = plugin.getConfig().getBoolean("allowBlockFading", false);
        allowReinforcementGracePeriod = plugin.getConfig().getBoolean("allowReinforcementGracePeriod", true);
        allowReinforcementHealing = plugin.getConfig().getBoolean("allowReinforcementHealing", true);
        leaveBlockAfterDeinforce = plugin.getConfig().getBoolean("leaveBlockAfterDeinforce", false);
        mobsInteractWithReinforcedBlocks = plugin.getConfig().getBoolean("mobsInteractWithReinforcedBlocks", false);
        enderdragonInteractWithReinforcedBlocks = plugin.getConfig().getBoolean("enderdragonInteractWithReinforcedBlocks", false);
        enableLogging = plugin.getConfig().getBoolean("enableLogging", true);

        // Loads the primary feedback form, ensuring that the provided type of feedback is valid.
        String primaryFeedback = plugin.getConfig().getString("primaryFeedback", "visual");
        if ("auditory".equals(primaryFeedback) || "visual".equals(primaryFeedback) || "off".equals(primaryFeedback)) {
            this.primaryFeedback = primaryFeedback;
        } else {
            this.primaryFeedback = "visual";
        }

        // Validates that the extinguish chance is a value from 0.0 to 1.0.
        double extinguishChance = plugin.getConfig().getDouble("extinguishChance", EXTINGUISH_CHANCE);
        this.extinguishChance = (extinguishChance < 0 || extinguishChance > 1) ? EXTINGUISH_CHANCE : extinguishChance;

        // Validates that the grace period for reinforcement removal is not less than zero.
        int gracePeriodTime = plugin.getConfig().getInt("gracePeriodTime", 3);
        this.gracePeriodTime = (gracePeriodTime < 0) ? 3 : gracePeriodTime;

        // Validates that the reinforcement healing time is not less than zero.
        int reinforcementHealingTime = plugin.getConfig().getInt("reinforcementHealingTime", 5);
        this.reinforcementHealingTime = (reinforcementHealingTime < 0) ? 5 : reinforcementHealingTime;

        ConfigurationSection configSection;

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        configSection = plugin.getConfig().getConfigurationSection("reinforceableBlocks");
        if (configSection == null) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforceable blocks list.");
        } else {
            for (final String materialName : configSection.getKeys(false)) {
                final Material material = Material.getMaterial(materialName);
                final int value = configSection.getInt(materialName);

                if (material != null && value > 0) {
                    reinforceableBlocks.put(material, value);
                } else {
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid reinforcement value.");
                }
            }
        }

        configSection = plugin.getConfig().getConfigurationSection("reinforcementBlocks");
        if (configSection == null) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforcement blocks list.");
        } else {
            for (final String materialName : configSection.getKeys(false)) {
                final int value = configSection.getInt(materialName);

                if (value > 0 || value == BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
                    reinforcementBlocks.put(Material.getMaterial(materialName), value);
                } else {
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid reinforcement value.");
                }
            }
        }

        configSection = plugin.getConfig().getConfigurationSection("toolRequirements");
        if (configSection == null) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load tool requirements list.");
        } else {
            for (final String materialName : configSection.getKeys(false)) {
                final Material blockMaterial = Material.getMaterial(materialName);
                final List<Integer> tools = new ArrayList<Integer>();

                for (final String split : configSection.getString(materialName).split(",")) {
                    // If HANDS is a supplied tool we add -1 to the tool list to represent it.
                    if ("HANDS".equals(split)) {
                        tools.add(BlockSaverUtil.HANDS_TOOL_CODE);
                    }
                    // If ALL is provided as a valid tool, we clear the list of other tools, add -2 to the tool list to represent it, and skip the rest of the tools.
                    else if ("ALL".equals(split)) {
                        tools.clear();
                        tools.add(BlockSaverUtil.ALL_TOOL_CODE);
                        toolRequirements.remove(blockMaterial);
                        break;
                    } else {
                        Material material = Material.getMaterial(split);
                        if (material != null) {
                            tools.add(material.getId());
                        }
                    }
                }

                // Copies over the previously added tools for the block.
                if (toolRequirements.containsKey(blockMaterial)) {
                    tools.addAll(toolRequirements.get(blockMaterial));
                    toolRequirements.remove(blockMaterial);
                    Bukkit.getLogger().log(Level.WARNING, "Tool requirements for the block " + blockMaterial.name() + " have been loaded more than once.");
                }

                toolRequirements.put(Material.getMaterial(materialName), tools);
            }
        }

        // Load the Reinforcement class. This is to ensure Reinforcement has access to MbapiPlugin and related configuration values.
        Reinforcement.initialize(this);
        // Load the Managers and initialize BlockSaverAction. This must occur in this order.
        infoManager = new BlockSaverInfoManager(this);
        feedbackManager = new FeedbackManager(this);
        reinforcementManager = new ReinforcementManager(this);
        if (feedbackManager.isPrismBridged()) {
            BlockSaverAction.initialize(reinforcementManager);
        }
    }

    public ReinforcementManager getReinforcementManager() {
        return reinforcementManager;
    }
}
