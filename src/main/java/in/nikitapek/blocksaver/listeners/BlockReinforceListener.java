package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.BlockReinforceEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class BlockReinforceListener implements Listener {
    private final ReinforcementManager reinforcementManager;
    private final BlockSaverInfoManager infoManager;

    public BlockReinforceListener(BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(BlockReinforceEvent event) {
        Block block = event.getBlock();

        infoManager.reinforce(block.getLocation(), event.getPlayerName(), true);
    }
}
