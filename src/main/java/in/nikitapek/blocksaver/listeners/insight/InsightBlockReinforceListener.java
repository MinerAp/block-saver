package in.nikitapek.blocksaver.listeners.insight;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.rows.BlockRowEntry;
import in.nikitapek.blocksaver.events.BlockReinforceEvent;
import in.nikitapek.blocksaver.util.InsightBridge;
import org.bukkit.event.EventHandler;

public final class InsightBlockReinforceListener extends BaseEventHandler<BlockReinforceEvent> {
    @EventHandler
    public void listen(BlockReinforceEvent event) {
        if (event.isLogged()) {
            add(new BlockRowEntry(event.getTime(), event.getPlayerName(), InsightBridge.ENFORCE_EVENT, event.getBlock()));
        }
    }
}
