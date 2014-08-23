package in.nikitapek.blocksaver.logging.insight;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.row.BlockRowEntry;

import in.nikitapek.blocksaver.events.BlockDeinforceEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public final class InsightBlockDeinforceListener extends BaseEventHandler<BlockDeinforceEvent> {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(BlockDeinforceEvent event) {
        if (event.isLogged()) {
            add(new BlockRowEntry(event.getTime(), event.getPlayerName(), InsightBridge.DAMAGE_EVENT, event.getBlock()));
        }
    }
}
