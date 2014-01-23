package in.nikitapek.blocksaver.listeners.prism;

import in.nikitapek.blocksaver.events.BlockDeinforceEvent;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverAction;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.PrismBridge;
import me.botsko.prism.Prism;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PrismBlockDeinforceListener implements Listener {
    private final ReinforcementManager reinforcementManager;

    public PrismBlockDeinforceListener(BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
    }

    @EventHandler
    public void listen(BlockDeinforceEvent event) {
        Location location = event.getBlock().getLocation();

        if (event.isLogged()) {
            Prism.actionsRecorder.addToQueue(new BlockSaverAction(location, event.getPlayerName(), PrismBridge.DAMAGE_EVENT, reinforcementManager.getReinforcement(location)));
        }
    }
}
