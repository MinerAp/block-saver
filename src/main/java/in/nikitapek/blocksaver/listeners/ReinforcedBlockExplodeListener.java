package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.ReinforcedBlockExplodeEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ReinforcedBlockExplodeListener implements Listener {
    private final BlockSaverInfoManager infoManager;

    public ReinforcedBlockExplodeListener(BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(ReinforcedBlockExplodeEvent event) {
        Block block = event.getBlock();
        // TODO: Find a way to replace this functionality.
        //float value = -((float) Math.pow(reinforcementManager.getMaterialReinforcementCoefficient(block.getType()), 2) / 100);

        infoManager.removeReinforcement(block.getLocation());
    }
}
