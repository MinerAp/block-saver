package in.nikitapek.blocksaver.logging.prism;

import com.amshulman.mbapi.MbapiPlugin;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.exceptions.InvalidActionException;

import org.bukkit.plugin.Plugin;

public final class PrismBridge {
    public static final String ENFORCE_EVENT_NAME = "bs-block-enforce";
    public static final String DAMAGE_EVENT_NAME = "bs-block-damage";
    public static final ActionType ENFORCE_EVENT = new ActionType(ENFORCE_EVENT_NAME, false, true, true, "BlockSaverAction", "reinforced");
    public static final ActionType DAMAGE_EVENT = new ActionType(DAMAGE_EVENT_NAME, false, true, true, "BlockSaverAction", "damaged");

    public PrismBridge(BlockSaverConfigurationContext configurationContext) {
        MbapiPlugin plugin = configurationContext.plugin;

        Plugin prismPlugin = plugin.getServer().getPluginManager().getPlugin("Prism");

        if (prismPlugin == null || !prismPlugin.isEnabled()) {
            return;
        }

        // Register the custom events.
        try {
            Prism.getActionRegistry().registerCustomAction(plugin, ENFORCE_EVENT);
            Prism.getActionRegistry().registerCustomAction(plugin, DAMAGE_EVENT);
            Prism.getHandlerRegistry().registerCustomHandler(plugin, BlockSaverAction.class);
        } catch (InvalidActionException e) {
            e.printStackTrace();
        }

        plugin.registerEventHandler(new PrismBlockDeinforceListener(configurationContext.infoManager));
        plugin.registerEventHandler(new PrismBlockReinforceListener(configurationContext.infoManager));
        plugin.registerEventHandler(new PrismReinforcedBlockExplodeListener(configurationContext.infoManager));

        BlockSaverAction.initialize(configurationContext.getReinforcementManager(), configurationContext.infoManager);
    }
}
