package in.nikitapek.blocksaver.util;

import com.amshulman.insight.action.BlockAction;
import com.amshulman.mbapi.MbapiPlugin;

public final class InsightBridge {
    public static final String ENFORCE_EVENT_NAME = "bs-block-enforce";
    public static final String DAMAGE_EVENT_NAME = "bs-block-damage";
    public static final BlockAction ENFORCE_EVENT = new BlockAction() {
        @Override
        public String name() {
            return ENFORCE_EVENT_NAME;
        }
    };
    public static final BlockAction DAMAGE_EVENT = new BlockAction() {
        @Override
        public String name() {
            return DAMAGE_EVENT_NAME;
        }
    };

    public InsightBridge(MbapiPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Marble") == null) {
            return;
        }
    }
}
