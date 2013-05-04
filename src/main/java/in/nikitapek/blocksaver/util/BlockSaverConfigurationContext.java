package in.nikitapek.blocksaver.util;

import org.bukkit.configuration.file.FileConfiguration;

import com.amshulman.mbapi.MbapiPlugin;
import com.amshulman.mbapi.util.ConfigurationContext;

public class BlockSaverConfigurationContext extends ConfigurationContext {
    public final int pearlCooldownTime;

    public BlockSaverConfigurationContext(MbapiPlugin plugin) {
        super(plugin);

        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        //pearlCooldownTime = config.getInt("pearlCooldownTime", 0);
        pearlCooldownTime = 0;
    }
}
