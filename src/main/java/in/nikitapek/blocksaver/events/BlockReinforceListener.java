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

public final class BlockReinforceListener extends EventBaseHandler<BlockReinforceEvent> {
    private final ReinforcementManager reinforcementManager;
    private final BlockSaverInfoManager infoManager;

    public BlockReinforceListener(final BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();
        this.infoManager = configurationContext.infoManager;
    }

    @EventHandler
    public void listen(BlockReinforceEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        String playerName = event.getPlayerName();
        int value = reinforcementManager.getMaterialReinforcementCoefficient(block.getType());

        infoManager.reinforce(location, playerName, value);

        if (event.isLogged()) {
            if (reinforcementManager.isPrismBridged()) {
                BlockSaverPrismBridge.logReinforcementEvent(reinforcementManager.getReinforcement(location), location, playerName, value);
            }
            if (reinforcementManager.isMarbleBridged()) {
                add(new BlockRowEntry(event.getTime(), playerName, BlockSaverMarbleBridge.ENFORCE_EVENT, event.getBlock()));
            }
        }
    }
}
