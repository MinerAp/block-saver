package in.nikitapek.blocksaver.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import in.nikitapek.blocksaver.management.FeedbackManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.GenericAction;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Map.Entry;

public final class BlockSaverAction extends GenericAction {
    private static ReinforcementManager reinforcementManager;

    public class ReinforcementActionData {
        public String owner;
    }

    private Reinforcement reinforcement;

    private ReinforcementActionData actionData;

    private Gson gson1 = new GsonBuilder().disableHtmlEscaping().create();

    public static void initialize(ReinforcementManager reinforcementManager) {
        BlockSaverAction.reinforcementManager = reinforcementManager;
    }

    public void setReinforcement(Reinforcement reinforcement) {
        actionData = new ReinforcementActionData();

        if (reinforcement == null) {
            this.setCanceled(true);
            return;
        }

        this.reinforcement = reinforcement;

        this.block_id = reinforcement.getBlock().getTypeId();
        this.setLoc(reinforcement.getLocation());
        actionData.owner = reinforcement.getCreatorName();
    }

    public void setData(String data) {
        this.data = data;
        setReinforcementFromData();
    }

    private void setReinforcementFromData() {
        if(reinforcement != null || data == null){
            return;
        }

        actionData = gson1.fromJson(data, ReinforcementActionData.class);
    }

    public void save() {
        data = gson1.toJson(actionData);
    }

    public ReinforcementActionData getActionData() {
        return this.actionData;
    }

    public Reinforcement getReinforcement() {
        return reinforcement;
    }

    @Override
    public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
        return placeReinforcements(parameters, is_preview);
    }

    @Override
    public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
        return placeReinforcements(parameters, is_preview);
    }

    private ChangeResult placeReinforcements(QueryParameters parameters, boolean is_preview) {
        ChangeResultType result = null;

        if (is_preview) {
            return new ChangeResult(ChangeResultType.PLANNED, null);
        }

        if (!plugin.getConfig().getBoolean("prism.appliers.allow_rollback_items_removed_from_container")) {
            return new ChangeResult(null, null);
        }

        if (!FeedbackManager.ENFORCE_EVENT_NAME.equals(getType().getName()) && !FeedbackManager.DAMAGE_EVENT_NAME.equals(getType().getName())) {
            return new ChangeResult(null, null);
        }

        PrismProcessType pt = parameters.getProcessType();

        if (!PrismProcessType.ROLLBACK.equals(pt) && !PrismProcessType.RESTORE.equals(pt)) {
            return new ChangeResult(null, null);
        }

        for (Location location : parameters.getSpecificBlockLocations()) {
            if (PrismProcessType.ROLLBACK.equals(pt)) {
                result = rollback(location, parameters, result);
            } else if (PrismProcessType.RESTORE.equals(pt)) {
                result = restore(location, parameters, result);
            }
        }

        return new ChangeResult(result, null);
    }

    private ChangeResultType rollback(Location location, QueryParameters parameters, ChangeResultType result) {
        if (FeedbackManager.ENFORCE_EVENT_NAME.equals(getType().getName())) {
            if (!reinforcementManager.isReinforced(location)) {
                return ChangeResultType.APPLIED;
            }

            Reinforcement reinforcement = new Reinforcement(location);

            Map<String, MatchRule> playerNames = parameters.getPlayerNames();
            if (playerNames.size() == 0) {
                reinforcementManager.removeReinforcement(location);
                return ChangeResultType.APPLIED;
            }

            for (Entry<String, MatchRule> entry : playerNames.entrySet()) {
                String name = entry.getKey();
                MatchRule rule = entry.getValue();
                String creator = reinforcement.getCreatorName();

                if ((name.equals(creator) && MatchRule.INCLUDE.equals(rule)) || (!name.equals(creator) && MatchRule.EXCLUDE.equals(rule))) {
                    reinforcementManager.removeReinforcement(location);
                    return ChangeResultType.APPLIED;
                }
            }
        } else {
            Map<String, MatchRule> playerNames = parameters.getPlayerNames();

            if (playerNames.size() == 0) {
                reinforcementManager.reinforce(location, 1, actionData.owner);
                return ChangeResultType.APPLIED;
            }

            for (Entry<String, MatchRule> entry : playerNames.entrySet()) {
                String name = entry.getKey();
                MatchRule rule = entry.getValue();
                String damager = getPlayerName();

                if ((name.equals(damager) && MatchRule.INCLUDE.equals(rule)) || (!name.equals(damager) && MatchRule.EXCLUDE.equals(rule))) {
                    reinforcementManager.reinforce(location, 1, actionData.owner);
                    return ChangeResultType.APPLIED;
                }
            }
        }

        return result;
    }

    private ChangeResultType restore(Location location, QueryParameters parameters, ChangeResultType result) {
        if (FeedbackManager.ENFORCE_EVENT_NAME.equals(getType().getName())) {
            Map<String, MatchRule> playerNames = parameters.getPlayerNames();
            if (playerNames.size() == 0) {
                reinforcementManager.reinforce(location, actionData.owner);
                return ChangeResultType.APPLIED;
            }

            for (Entry<String, MatchRule> entry : playerNames.entrySet()) {
                String name = entry.getKey();
                MatchRule rule = entry.getValue();
                String creator = reinforcement.getCreatorName();

                if ((name.equals(creator) && MatchRule.INCLUDE.equals(rule)) || (!name.equals(creator) && MatchRule.EXCLUDE.equals(rule))) {
                    reinforcementManager.reinforce(location, actionData.owner);
                    return ChangeResultType.APPLIED;
                }
            }
        } else {
            Map<String, MatchRule> playerNames = parameters.getPlayerNames();

            if (playerNames.size() == 0) {
                reinforcementManager.reinforce(location, -1, getPlayerName());
                return ChangeResultType.APPLIED;
            }

            for (Entry<String, MatchRule> entry : playerNames.entrySet()) {
                String name = entry.getKey();
                MatchRule rule = entry.getValue();
                String damager = getPlayerName();

                if ((name.equals(damager) && MatchRule.INCLUDE.equals(rule)) || (!name.equals(damager) && MatchRule.EXCLUDE.equals(rule))) {
                    reinforcementManager.reinforce(location, -1, damager);
                    return ChangeResultType.APPLIED;
                }
            }
        }

        return result;
    }
}
