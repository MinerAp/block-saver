package in.nikitapek.blocksaver.listeners;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.rows.BlockRowEntry;
import in.nikitapek.blocksaver.events.BlockReinforceEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.InsightBridge;
import in.nikitapek.blocksaver.util.PrismBridge;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;

public final class BlockReinforceListener extends BaseEventHandler<BlockReinforceEvent> {
    private final ReinforcementManager reinforcementManager;
    private final BlockSaverInfoManager infoManager;

    public BlockReinforceListener(final BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler
    public void listen(BlockReinforceEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        String playerName = event.getPlayerName();
        int value = reinforcementManager.getMaterialReinforcementCoefficient(block.getType());

        infoManager.reinforce(location, playerName, value);

        if (event.isLogged()) {
            if (reinforcementManager.isPrismBridged()) {
                PrismBridge.logReinforcementEvent(reinforcementManager.getReinforcement(location), location, playerName, value);
            }
            if (reinforcementManager.isInsightBridged()) {
                add(new BlockRowEntry(event.getTime(), playerName, InsightBridge.ENFORCE_EVENT, event.getBlock()));
            }
        }
    }
}
