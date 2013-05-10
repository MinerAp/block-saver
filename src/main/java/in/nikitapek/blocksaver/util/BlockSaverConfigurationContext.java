package in.nikitapek.blocksaver.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public class BlockSaverConfigurationContext extends ConfigurationContext {
    public final TypeSafeMap<Material, Byte> reinforceableBlocks;
    public final TypeSafeMap<Block, Byte> reinforcedBlocks;

    public final Effect blockBreakFailEffect = Effect.EXTINGUISH;
    public final Effect reinforcedBlockDamageEffect = Effect.POTION_BREAK;
    public final Sound blockReinforceSound = Sound.BURP;

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin);

        reinforceableBlocks = new TypeSafeMapImpl<Material, Byte>(new EnumMap<Material, Byte>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.BYTE);
        reinforcedBlocks = new TypeSafeMapImpl<Block, Byte>(new HashMap<Block, Byte>(), SupplimentaryTypes.BLOCK, SupplimentaryTypes.BYTE);

        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        // Attempts to read the configurationSection containing the keys and values storing the block reinforcement coefficients.
        // If it is successful in reading them, it then stores them to the reinforceableBlocks map for use throughout the plugin.
        try {
            ConfigurationSection blockCoefficientMap = config.getConfigurationSection("reinforceableBlocks");
            for (String materialName : blockCoefficientMap.getKeys(false)) {
                reinforceableBlocks.put(Material.getMaterial(materialName), (byte) blockCoefficientMap.getInt(materialName));
            }
        }
        catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration.");
            e.printStackTrace();
        }
    }
}
