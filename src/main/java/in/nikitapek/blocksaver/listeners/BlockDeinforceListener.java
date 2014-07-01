package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.BlockDeinforceEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class BlockDeinforceListener implements Listener {
    private final BlockSaverInfoManager infoManager;

    public BlockDeinforceListener(BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(BlockDeinforceEvent event) {
        infoManager.removeReinforcement(event.getBlock().getLocation());
    }
}
