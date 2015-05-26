package in.nikitapek.blocksaver.logging.insight;

import com.amshulman.insight.action.BlockAction;
import com.amshulman.insight.util.InsightAPI;
import com.amshulman.mbapi.MbapiPlugin;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import org.bukkit.Bukkit;

public final class InsightBridge {
    static final BlockAction ENFORCE_EVENT = new BlockAction() {
        @Override
        public String getName() {
            return "bs-block-enforce";
        }

        @Override
        public String getFriendlyDescription() {
            return "reinforced";
        }

        @Override
        public BlockRollbackAction getRollbackAction() {
            // TODO Auto-generated method stub
            return null;
        }
    };
    static final BlockAction DAMAGE_EVENT = new BlockAction() {
        @Override
        public String getName() {
            return "bs-block-damage";
        }

        @Override
        public String getFriendlyDescription() {
            return "damaged";
        }

        @Override
        public BlockRollbackAction getRollbackAction() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    public InsightBridge(BlockSaverConfigurationContext configurationContext) {
        InsightAPI insightPlugin = (InsightAPI) Bukkit.getPluginManager().getPlugin("Insight");
        if (insightPlugin == null || !insightPlugin.isEnabled()) {
            return;
        }

        insightPlugin.registerAction(DAMAGE_EVENT);
        insightPlugin.registerAction(ENFORCE_EVENT);

        MbapiPlugin plugin = configurationContext.plugin;
        plugin.registerEventHandler(new InsightBlockDeinforceListener());
        plugin.registerEventHandler(new InsightBlockReinforceListener());
        plugin.registerEventHandler(new InsightReinforcedBlockDamageListener());
        plugin.registerEventHandler(new InsightReinforcedBlockExplodeListener());
    }
}
