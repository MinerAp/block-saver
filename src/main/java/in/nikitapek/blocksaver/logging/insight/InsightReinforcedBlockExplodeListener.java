package in.nikitapek.blocksaver.logging.insight;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.rows.BlockRowEntry;

import in.nikitapek.blocksaver.events.ReinforcedBlockExplodeEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public final class InsightReinforcedBlockExplodeListener extends BaseEventHandler<ReinforcedBlockExplodeEvent> {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(ReinforcedBlockExplodeEvent event) {
        if (event.isLogged()) {
            add(new BlockRowEntry(event.getTime(), event.getPlayerName(), InsightBridge.ENFORCE_EVENT, event.getBlock()));
        }
    }
}
