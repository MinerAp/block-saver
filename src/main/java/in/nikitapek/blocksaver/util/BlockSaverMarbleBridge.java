package in.nikitapek.blocksaver.util;

import com.amshulman.marble.action.BlockAction;
import com.amshulman.marble.event.EventBaseHandler;
import com.amshulman.marble.rows.BlockRowEntry;
import com.amshulman.mbapi.MbapiPlugin;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import org.bukkit.Location;

public final class BlockSaverMarbleBridge {
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

    public BlockSaverMarbleBridge(MbapiPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Marble") == null) {
            return;
        }
    }
}
