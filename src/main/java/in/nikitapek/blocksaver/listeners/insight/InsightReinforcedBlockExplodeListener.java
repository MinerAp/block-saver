package in.nikitapek.blocksaver.listeners.insight;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.rows.BlockRowEntry;
import in.nikitapek.blocksaver.events.ReinforcedBlockExplodeEvent;
import in.nikitapek.blocksaver.util.InsightBridge;
import org.bukkit.event.EventHandler;

public final class InsightReinforcedBlockExplodeListener extends BaseEventHandler<ReinforcedBlockExplodeEvent> {
    @EventHandler
    public void listen(ReinforcedBlockExplodeEvent event) {
        if (event.isLogged()) {
            add(new BlockRowEntry(event.getTime(), event.getPlayerName(), InsightBridge.ENFORCE_EVENT, event.getBlock()));
        }
    }
}
