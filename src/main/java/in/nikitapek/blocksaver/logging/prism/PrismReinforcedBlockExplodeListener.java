package in.nikitapek.blocksaver.logging.prism;

import in.nikitapek.blocksaver.events.ReinforcedBlockExplodeEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import me.botsko.prism.Prism;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PrismReinforcedBlockExplodeListener implements Listener {
    private final BlockSaverInfoManager infoManager;

    public PrismReinforcedBlockExplodeListener(BlockSaverInfoManager infoManager) {
        this.infoManager = infoManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void listen(ReinforcedBlockExplodeEvent event) {
        Location location = event.getBlock().getLocation();

        if (event.isLogged()) {
            Prism.actionsRecorder.addToQueue(new BlockSaverAction(location, event.getPlayerName(), PrismBridge.DAMAGE_EVENT, infoManager.getReinforcement(location)));
        }
    }
}
