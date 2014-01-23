package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.MbapiPlugin;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.exceptions.InvalidActionException;
import org.bukkit.Location;

public final class PrismBridge {
    public static final String ENFORCE_EVENT_NAME = "bs-block-enforce";
    public static final String DAMAGE_EVENT_NAME = "bs-block-damage";
    public static final ActionType ENFORCE_EVENT = new ActionType(ENFORCE_EVENT_NAME, false, true, true, "BlockSaverAction", "reinforced");
    public static final ActionType DAMAGE_EVENT = new ActionType(DAMAGE_EVENT_NAME, false, true, true, "BlockSaverAction", "damaged");

    public PrismBridge(MbapiPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Prism") == null) {
            return;
        }

        // Register the custom events.
        try {
            Prism.getActionRegistry().registerCustomAction(plugin, ENFORCE_EVENT);
            Prism.getActionRegistry().registerCustomAction(plugin, DAMAGE_EVENT);
            Prism.getHandlerRegistry().registerCustomHandler(plugin, BlockSaverAction.class);
        } catch (InvalidActionException e) {
            e.printStackTrace();
        }
    }

    public static void logReinforcementEvent(final Reinforcement reinforcement, final Location location, final String playerName, float value) {
        ActionType actionType;

        if (value < 0) {
            actionType = PrismBridge.DAMAGE_EVENT;
        } else {
            actionType = PrismBridge.ENFORCE_EVENT;
        }

        logReinforcementEvent(reinforcement, location, playerName, actionType);
    }

    public static void logReinforcementEvent(final Reinforcement reinforcement, final Location location, final String playerName, ActionType actionType) {
        BlockSaverAction action = new BlockSaverAction();

        action.setType(actionType);
        action.setLoc(location);
        action.setPlayerName(playerName);

        // Required for the ItemStackAction
        action.setReinforcement(location, reinforcement);

        // Add the recorder queue
        Prism.actionsRecorder.addToQueue(action);
    }
}
