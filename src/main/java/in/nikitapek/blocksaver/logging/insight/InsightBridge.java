package in.nikitapek.blocksaver.logging.insight;

import com.amshulman.insight.action.BlockAction;
import com.amshulman.mbapi.MbapiPlugin;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import org.bukkit.plugin.Plugin;

public final class InsightBridge {
    public static final String ENFORCE_EVENT_NAME = "bs-block-enforce";
    public static final String DAMAGE_EVENT_NAME = "bs-block-damage";
    public static final BlockAction ENFORCE_EVENT = new BlockAction() {
        @Override
        public String getName() {
            return ENFORCE_EVENT_NAME;
        }

        @Override
        public String getFriendlyDescription() {
            return "reinforced";
        }
    };
    public static final BlockAction DAMAGE_EVENT = new BlockAction() {
        @Override
        public String getName() {
            return DAMAGE_EVENT_NAME;
        }

        @Override
        public String getFriendlyDescription() {
            return "damaged";
        }
    };

    public InsightBridge(BlockSaverConfigurationContext configurationContext) {
        MbapiPlugin plugin = configurationContext.plugin;

        Plugin insightPlugin = plugin.getServer().getPluginManager().getPlugin("Insight");

        if (insightPlugin == null || !insightPlugin.isEnabled()) {
            return;
        }

        plugin.registerEventHandler(new InsightBlockDeinforceListener());
        plugin.registerEventHandler(new InsightBlockReinforceListener());
        plugin.registerEventHandler(new InsightReinforcedBlockExplodeListener());
    }
}
