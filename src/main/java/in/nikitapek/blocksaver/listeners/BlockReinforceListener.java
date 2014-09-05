package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.BlockReinforceEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class BlockReinforceListener implements Listener {
    private final BlockSaverInfoManager infoManager;

    public BlockReinforceListener(BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(BlockReinforceEvent event) {
        infoManager.reinforce(event.getBlock().getLocation(), event.getPlayerName());
    }
}
