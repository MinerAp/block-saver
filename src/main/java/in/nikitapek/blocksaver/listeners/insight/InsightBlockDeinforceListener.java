package in.nikitapek.blocksaver.listeners.insight;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.rows.BlockRowEntry;
import in.nikitapek.blocksaver.events.BlockDeinforceEvent;
import in.nikitapek.blocksaver.util.InsightBridge;
import org.bukkit.event.EventHandler;

public final class InsightBlockDeinforceListener extends BaseEventHandler<BlockDeinforceEvent> {
    @EventHandler
    public void listen(BlockDeinforceEvent event) {
        if (event.isLogged()) {
            add(new BlockRowEntry(event.getTime(), event.getPlayerName(), InsightBridge.DAMAGE_EVENT, event.getBlock()));
        }
    }
}
