package in.nikitapek.blocksaver.logging.insight;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.row.BlockRowEntry;

import in.nikitapek.blocksaver.events.ReinforcedBlockDamageEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public final class InsightReinforcedBlockDamageListener extends BaseEventHandler<ReinforcedBlockDamageEvent> {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(ReinforcedBlockDamageEvent event) {
        if (event.isLogged()) {
            add(new BlockRowEntry(event.getTime(), event.getPlayerName(), InsightBridge.DAMAGE_EVENT, event.getBlock()));
        }
    }
}
