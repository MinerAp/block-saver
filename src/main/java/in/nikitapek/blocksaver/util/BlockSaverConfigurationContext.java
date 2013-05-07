package in.nikitapek.blocksaver.util;

import java.util.EnumMap;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public class BlockSaverConfigurationContext extends ConfigurationContext {

    public final TypeSafeMap<Material, Byte> reinforceableBlocks = new TypeSafeMapImpl<Material, Byte>(new EnumMap<Material, Byte>(Material.class), SupplimentaryTypes.MATERIAL, SupplimentaryTypes.BYTE);

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin);

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
