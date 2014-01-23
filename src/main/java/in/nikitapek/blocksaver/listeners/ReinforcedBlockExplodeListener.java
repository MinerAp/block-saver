package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.ReinforcedBlockExplodeEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ReinforcedBlockExplodeListener implements Listener {
    private final ReinforcementManager reinforcementManager;
    private final BlockSaverInfoManager infoManager;

    public ReinforcedBlockExplodeListener(BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler
    public void listen(ReinforcedBlockExplodeEvent event) {
        Block block = event.getBlock();
        float value = -((float) Math.pow(reinforcementManager.getMaterialReinforcementCoefficient(block.getType()), 2) / 100);

        infoManager.reinforce(block.getLocation(), event.getPlayerName(), value);
    }
}
