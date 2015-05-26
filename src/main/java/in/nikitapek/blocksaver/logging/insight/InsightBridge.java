package in.nikitapek.blocksaver.logging.insight;

import com.amshulman.insight.action.BlockAction;
import com.amshulman.mbapi.MbapiPlugin;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import org.bukkit.plugin.Plugin;

public final class InsightBridge {
    public static final BlockAction ENFORCE_EVENT = new BlockAction() {
        @Override
        public String getName() {
            return "bs-block-enforce";
        }

        @Override
        public String getFriendlyDescription() {
            return "reinforced";
        }
    };
    public static final BlockAction DAMAGE_EVENT = new BlockAction() {
        @Override
        public String getName() {
            return "bs-block-damage";
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
        plugin.registerEventHandler(new InsightReinforcedBlockDamageListener());
        plugin.registerEventHandler(new InsightReinforcedBlockExplodeListener());
    }
}
