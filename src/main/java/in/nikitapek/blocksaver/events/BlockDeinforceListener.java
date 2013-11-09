package in.nikitapek.blocksaver.events;

import com.amshulman.marble.event.EventBaseHandler;
import com.amshulman.marble.rows.BlockRowEntry;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverMarbleBridge;
import in.nikitapek.blocksaver.util.BlockSaverPrismBridge;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;

public final class BlockDeinforceListener extends EventBaseHandler<BlockReinforceEvent> {
    private final ReinforcementManager reinforcementManager;
    private final BlockSaverInfoManager infoManager;

    public BlockDeinforceListener(final BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler
    public void listen(BlockReinforceEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        String playerName = event.getPlayerName();

        infoManager.removeReinforcement(location);

        if (event.isLogged) {
            if (reinforcementManager.isPrismBridged()) {
                BlockSaverPrismBridge.logReinforcementEvent(reinforcementManager.getReinforcement(location), location, playerName, BlockSaverPrismBridge.DAMAGE_EVENT);
            }
            if (reinforcementManager.isMarbleBridged()) {
                add(new BlockRowEntry(event.getTime(), playerName, BlockSaverMarbleBridge.DAMAGE_EVENT, event.getBlock()));
            }
        }
    }
}
