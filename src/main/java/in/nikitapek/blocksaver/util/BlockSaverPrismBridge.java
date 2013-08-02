package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.serialization.Reinforcement;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.exceptions.InvalidActionException;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.amshulman.mbapi.MbapiPlugin;

public final class BlockSaverPrismBridge {
    public static final String ENFORCE_EVENT_NAME = "bs-block-enforce";
    public static final String DAMAGE_EVENT_NAME = "bs-block-damage";
    public static final ActionType ENFORCE_EVENT = new ActionType(ENFORCE_EVENT_NAME, false, true, true, "BlockSaverAction", "reinforced");
    public static final ActionType DAMAGE_EVENT = new ActionType(DAMAGE_EVENT_NAME, false, true, true, "BlockSaverAction", "damaged");

    private final MbapiPlugin plugin;
    private final Prism prism;

    public BlockSaverPrismBridge(MbapiPlugin plugin) {
        this.plugin = plugin;

        final Plugin tempPrism = plugin.getServer().getPluginManager().getPlugin("Prism");

        if (tempPrism == null) {
            prism = null;
            return;
        }

        prism = (Prism) tempPrism;

        // Register the custom events.
        try {
            Prism.getActionRegistry().registerCustomAction(plugin, ENFORCE_EVENT);
            Prism.getActionRegistry().registerCustomAction(plugin, DAMAGE_EVENT);
            Prism.getHandlerRegistry().registerCustomHandler(plugin, BlockSaverAction.class);
        }
        catch (InvalidActionException e) {
            e.printStackTrace();
        }
    }

    public void logCustomEvent(final Reinforcement reinforcement, final Player player, final ActionType event) {
        BlockSaverAction action = new BlockSaverAction();

        action.setType(event);
        action.setLoc(reinforcement.getLocation());
        action.setPlayerName(player.getName());

        // Required for the ItemStackAction
        action.setReinforcement(reinforcement);

        // Add the recorder queue
        Prism.actionsRecorder.addToQueue(action);
    }
}
