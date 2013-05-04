package in.nikitapek.blocksaver.util;

import java.util.EnumSet;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;
import com.amshulman.typesafety.TypeSafeSet;
import com.amshulman.typesafety.impl.TypeSafeSetImpl;

public class BlockSaverConfigurationContext extends ConfigurationContext {
    
    public final TypeSafeSet<Material> reinforceableBlocks = new TypeSafeSetImpl<Material>(EnumSet.noneOf(Material.class), SupplimentaryTypes.MATERIAL);

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin);

        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        try {
            for (Object materialString : config.getList("reinforceableBlocks")) {
                Material material = Material.getMaterial((String) materialString);

                reinforceableBlocks.add((Material) material);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration");
            e.printStackTrace();
        }
    }
}
