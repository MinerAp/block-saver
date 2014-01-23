package in.nikitapek.blocksaver.events;

import com.amshulman.insight.event.BaseEventHandler;
import com.amshulman.insight.rows.BlockRowEntry;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverMarbleBridge;
import in.nikitapek.blocksaver.util.BlockSaverPrismBridge;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;

public final class ReinforcedBlockExplodeListener extends BaseEventHandler<ReinforcedBlockExplodeEvent> {
    private final ReinforcementManager reinforcementManager;
    private final BlockSaverInfoManager infoManager;

    public ReinforcedBlockExplodeListener(final BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler
    public void listen(ReinforcedBlockExplodeEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        String playerName = event.getPlayerName();
        float value = -((float) Math.pow(reinforcementManager.getMaterialReinforcementCoefficient(block.getType()), 2) / 100);

        infoManager.reinforce(location, playerName, value);

        if (event.isLogged) {
            if (reinforcementManager.isPrismBridged()) {
                BlockSaverPrismBridge.logReinforcementEvent(reinforcementManager.getReinforcement(location), location, playerName, BlockSaverPrismBridge.DAMAGE_EVENT);
            }
            if (reinforcementManager.isMarbleBridged()) {
                add(new BlockRowEntry(event.getTime(), playerName, BlockSaverMarbleBridge.ENFORCE_EVENT, event.getBlock()));
            }
        }
    }
}
