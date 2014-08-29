package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.mbapi.util.CoreTypes;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.TypeSafeSet;
import com.amshulman.typesafety.gson.TypeSafeMapTypeAdapter;
import com.amshulman.typesafety.gson.TypeSafeSetTypeAdapter;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;
import com.amshulman.typesafety.impl.TypeSafeSetImpl;

import in.nikitapek.blocksaver.logging.insight.InsightBridge;
import in.nikitapek.blocksaver.logging.prism.PrismBridge;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.FeedbackManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.serialization.LocationTypeAdapter;
import in.nikitapek.blocksaver.serialization.Reinforcement;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

public final class BlockSaverConfigurationContext extends ConfigurationContext {
    private final static double EXTINGUISH_CHANCE = 0.9;

    public Effect reinforcementDamageFailEffect;
    public Sound reinforceSuccessSound;
    public Sound reinforceFailSound;
    public Sound hitFailSound;

    public final boolean tntDamagesReinforcedBlocks;
    public final boolean tntStripReinforcementEntirely;
    public final boolean liquidsDestroyReinforcedBlocks;
    public final boolean allowReinforcedBlockPhysics;
    public final boolean fireDamagesReinforcedBlocks;
    public final boolean extinguishReinforcementFire;
    public final boolean pistonsMoveReinforcedBlocks;
    public final boolean allowBlockFading;
    public final boolean allowReinforcementGracePeriod;
    public final boolean leaveBlockAfterDeinforce;
    public final boolean mobsInteractWithReinforcedBlocks;
    public final boolean enderdragonInteractWithReinforcedBlocks;
    public final boolean prismLogging;
    public final boolean insightLogging;
    public final boolean integrateWorldEdit;
    public final double extinguishChance;
    public final int gracePeriodTime;

    public final BlockSaverInfoManager infoManager;
    public final FeedbackManager feedbackManager;

    public final TypeSafeSet<String> worlds;
    public final TypeSafeMap<Material, Integer> reinforceableBlocks;
    public final TypeSafeMap<Material, Integer> reinforcementBlocks;
    public final TypeSafeMap<Material, List<Integer>> toolRequirements;

    private final ReinforcementManager reinforcementManager;

    public BlockSaverConfigurationContext(final MbapiPlugin plugin) {
        super(plugin,
                new TypeSafeMapTypeAdapter<>(SupplementaryTypes.HASHMAP, SupplementaryTypes.MATERIAL, CoreTypes.INTEGER),
                new TypeSafeMapTypeAdapter<>(SupplementaryTypes.HASHMAP, SupplementaryTypes.LOCATION, SupplementaryTypes.REINFORCEMENT),
                new TypeSafeSetTypeAdapter<Reinforcement>(SupplementaryTypes.HASHSET, SupplementaryTypes.REINFORCEMENT),
                new LocationTypeAdapter());

        worlds = new TypeSafeSetImpl<>(new HashSet<String>(), SupplementaryTypes.STRING);
        reinforceableBlocks = new TypeSafeMapImpl<>(new EnumMap<Material, Integer>(Material.class), SupplementaryTypes.MATERIAL, CoreTypes.INTEGER);
        reinforcementBlocks = new TypeSafeMapImpl<>(new HashMap<Material, Integer>(), SupplementaryTypes.MATERIAL, CoreTypes.INTEGER);
        toolRequirements = new TypeSafeMapImpl<>(new EnumMap<Material, List<Integer>>(Material.class), SupplementaryTypes.MATERIAL, SupplementaryTypes.LIST);

        plugin.saveDefaultConfig();

        try {
            // Note: setting the default values here might be unnecessary because if the config values fail to load and it defaults to these, they will never result in an exception.
            reinforcementDamageFailEffect = Effect.valueOf(plugin.getConfig().getString("reinforcementDamageFailEffect", Effect.EXTINGUISH.toString()));
            reinforceSuccessSound = Sound.valueOf(plugin.getConfig().getString("reinforceSuccessSound", Sound.ANVIL_USE.toString()));
            reinforceFailSound = Sound.valueOf(plugin.getConfig().getString("reinforceFailSound", Sound.BLAZE_HIT.toString()));
            hitFailSound = Sound.valueOf(plugin.getConfig().getString("hitFailSound", Sound.CREEPER_DEATH.toString()));
        } catch (final IllegalArgumentException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load one or more Effect values. Reverting to defaults.");

            reinforcementDamageFailEffect = Effect.EXTINGUISH;
            reinforceSuccessSound = Sound.ANVIL_USE;
            reinforceFailSound = Sound.BLAZE_HIT;
            hitFailSound = Sound.CREEPER_DEATH;
        }

        tntDamagesReinforcedBlocks = plugin.getConfig().getBoolean("tntDamagesReinforcedBlocks", true);
        tntStripReinforcementEntirely = plugin.getConfig().getBoolean("tntStripReinforcementEntirely", false);
        liquidsDestroyReinforcedBlocks = plugin.getConfig().getBoolean("liquidsDestroyReinforcedBlocks", true);
        allowReinforcedBlockPhysics = plugin.getConfig().getBoolean("allowReinforcedBlockPhysics", true);
        fireDamagesReinforcedBlocks = plugin.getConfig().getBoolean("fireDamagesReinforcedBlocks", true);
        extinguishReinforcementFire = plugin.getConfig().getBoolean("extinguishReinforcementFire", true);
        pistonsMoveReinforcedBlocks = plugin.getConfig().getBoolean("pistonsMoveReinforcedBlocks", true);
        allowBlockFading = plugin.getConfig().getBoolean("allowBlockFading", false);
        allowReinforcementGracePeriod = plugin.getConfig().getBoolean("allowReinforcementGracePeriod", true);
        leaveBlockAfterDeinforce = plugin.getConfig().getBoolean("leaveBlockAfterDeinforce", false);
        mobsInteractWithReinforcedBlocks = plugin.getConfig().getBoolean("mobsInteractWithReinforcedBlocks", false);
        enderdragonInteractWithReinforcedBlocks = plugin.getConfig().getBoolean("enderdragonInteractWithReinforcedBlocks", false);
        prismLogging = plugin.getConfig().getBoolean("prismLogging", true);
        insightLogging = plugin.getConfig().getBoolean("insightLogging", true);
        integrateWorldEdit = plugin.getConfig().getBoolean("integrateWorldEdit", true);

        // Validates that the extinguish chance is a value from 0.0 to 1.0.
        double extinguishChance = plugin.getConfig().getDouble("extinguishChance", EXTINGUISH_CHANCE);
        this.extinguishChance = (extinguishChance < 0 || extinguishChance > 1) ? EXTINGUISH_CHANCE : extinguishChance;

        // Validates that the grace period for reinforcement removal is not less than zero.
        int gracePeriodTime = plugin.getConfig().getInt("gracePeriodTime", 3);
        this.gracePeriodTime = (gracePeriodTime < 0) ? 3 : gracePeriodTime;

        ConfigurationSection configSection;

        // Attempts to read the configurationSection containing the worlds protected by the plugin.
        @SuppressWarnings("unchecked")
        List<String> worldList = (List<String>) plugin.getConfig().getList("worlds");
        if (worldList == null) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load protected worlds list.");
        } else {
            for (final String world : worldList) {
                worlds.add(world);
            }
        }

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        configSection = plugin.getConfig().getConfigurationSection("reinforceableBlocks");
        if (configSection == null) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reinforceable blocks list.");
        } else {
            for (final String materialName : configSection.getKeys(false)) {
                final Material material = loadMaterial(materialName);
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
            for (String materialName : configSection.getKeys(false)) {
                final Material material = loadMaterial(materialName);
                final int value = configSection.getInt(materialName);

                if (material != null && value > 0) {
                    reinforcementBlocks.put(material, value);
                } else {
                    plugin.getLogger().log(Level.WARNING, materialName + "has an invalid usage count.");
                }
            }
        }

        configSection = plugin.getConfig().getConfigurationSection("toolRequirements");
        if (configSection == null) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load tool requirements list.");
        } else {
            for (final String materialName : configSection.getKeys(false)) {
                final Material blockMaterial = loadMaterial(materialName);
                final List<Integer> tools = new ArrayList<>();

                tools:
                for (final String split : configSection.getString(materialName).split(",")) {
                    switch (split) {
                        // If HANDS is a supplied tool we add -1 to the tool list to represent it.
                        case "HANDS":
                            tools.add(BlockSaverUtil.HANDS_TOOL_CODE);
                            break;
                        // If ALL is provided as a valid tool, we clear the list of other tools, add -2 to the tool list to represent it, and skip the rest of the tools.
                        case "ALL":
                            tools.clear();
                            tools.add(BlockSaverUtil.ALL_TOOL_CODE);
                            toolRequirements.remove(blockMaterial);
                            break tools;
                        default:
                            Material material = loadMaterial(split);
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

                toolRequirements.put(loadMaterial(materialName), tools);
            }
        }

        // Load the Reinforcement class. This is to ensure Reinforcement has access to MbapiPlugin and related configuration values.
        Reinforcement.initialize(this);
        // Load the Managers and initialize BlockSaverAction. This must occur in this order.
        infoManager = new BlockSaverInfoManager(this);
        feedbackManager = new FeedbackManager(this);
        reinforcementManager = new ReinforcementManager(this);


        if (prismLogging) {
            try {
                new PrismBridge(this);
            } catch (final NoClassDefFoundError ex) {
                plugin.getLogger().log(Level.WARNING, "\"prismLogging\" true but Prism not found or not enabled. Prism logging will not be enabled.");
            }
        }

        if (insightLogging) {
            try {
                new InsightBridge(this);
            } catch (final NoClassDefFoundError ex) {
                plugin.getLogger().log(Level.WARNING, "\"insightLogging\" true but Insight not found or not enabled. Insight logging will not be enabled.");
            }
        }
    }

    private static Material loadMaterial(String materialName) {
        int materialId;

        try {
            materialId = Integer.parseInt(materialName);
        } catch (NumberFormatException e) {
            return Material.getMaterial(materialName);
        }

        return Material.getMaterial(materialId);
    }

    public ReinforcementManager getReinforcementManager() {
        return reinforcementManager;
    }
}
