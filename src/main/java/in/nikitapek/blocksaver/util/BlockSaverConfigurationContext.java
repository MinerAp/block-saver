package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;

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

    public final Effect blockBreakFailEffect = Effect.EXTINGUISH;
    public final Effect reinforcedBlockDamageEffect = Effect.POTION_BREAK;
    public final Sound blockReinforceSound = Sound.BURP;

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin, new TypeSafeSetTypeAdapter<Reinforcement>(SupplimentaryTypes.TREESET, SupplimentaryTypes.REINFORCEMENT));

        infoManager = new BlockSaverInfoManager(this);

        reinforceableBlocks = new TypeSafeMapImpl<Material, Byte>(new EnumMap<Material, Byte>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.BYTE);

        plugin.saveDefaultConfig();

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        // If it is successful in reading them, it then stores them to the reinforceableBlocks map for use throughout the plugin.
        try {
            ConfigurationSection blockCoefficientMap = plugin.getConfig().getConfigurationSection("reinforceableBlocks");
            for (String materialName : blockCoefficientMap.getKeys(false)) {
                reinforceableBlocks.put(Material.getMaterial(materialName), (byte) blockCoefficientMap.getInt(materialName));
            }
        }
        catch (Exception e) {
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
