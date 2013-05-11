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

    private final TypeSafeMap<Material, Byte> reinforceableBlocks;

    public Effect blockBreakFailEffect;
    public Effect blockReinforcementDamageEffect;
    public Sound blockReinforceSound;

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin, new TypeSafeSetTypeAdapter<Reinforcement>(SupplimentaryTypes.TREESET, SupplimentaryTypes.REINFORCEMENT), new ReinforcementTypeAdapter());

        infoManager = new BlockSaverInfoManager(this);

        reinforceableBlocks = new TypeSafeMapImpl<Material, Byte>(new EnumMap<Material, Byte>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.BYTE);

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

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        // If it is successful in reading them, it then stores them to the reinforceableBlocks map for use throughout the plugin.
        try {
            ConfigurationSection blockCoefficientMap = plugin.getConfig().getConfigurationSection("reinforceableBlocks");
            for (String materialName : blockCoefficientMap.getKeys(false)) {
                reinforceableBlocks.put(Material.getMaterial(materialName), (byte) blockCoefficientMap.getInt(materialName));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration.");
            e.printStackTrace();
        }
    }

    public boolean isMaterialReinforceable(Material material) {
        return reinforceableBlocks.containsKey(material);
    }

    public boolean isReinforceable(Block block) {
        byte coefficient = getMaterialReinforcementCoefficient(block.getType());

        return coefficient != -1 ? infoManager.getReinforcementValue(block.getLocation()) < coefficient : false;
    }
    
    public byte getMaterialReinforcementCoefficient(Material material) {
        return isMaterialReinforceable(material) ? reinforceableBlocks.get(material) : -1;
    }
    
    public boolean attemptReinforcement(Block block) {
        byte coefficient = getMaterialReinforcementCoefficient(block.getType());

        if (coefficient == -1)
            return false;

        if (infoManager.getReinforcementValue(block.getLocation()) >= coefficient)
            return false;

        infoManager.setReinforcement(block.getLocation(), coefficient);
        return true;
    }
}
