package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.ReinforcedBlockDamageEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ReinforcedBlockDamageListener implements Listener {
    private final BlockSaverInfoManager infoManager;

    public ReinforcedBlockDamageListener(BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(ReinforcedBlockDamageEvent event) {
        infoManager.reinforce(event.getBlock().getLocation(), event.getPlayerName(), false);
    }
}
