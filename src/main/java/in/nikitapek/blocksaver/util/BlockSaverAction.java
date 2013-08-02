package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.GenericAction;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;

import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class BlockSaverAction extends GenericAction {
    private static ReinforcementManager reinforcementManager;

    public class ReinforcementActionData {
        public String owner;
        public long creationTime;
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
        actionData.creationTime = reinforcement.getCreationTime();
    }

    public void setData(String data) {
        this.data = data;
        setReinforcementFromData();
    }

    private void setReinforcementFromData() {
        if (reinforcement != null || data == null) {
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

    @Override
    public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
        return placeReinforcements(parameters.getProcessType(), is_preview);
    }

    @Override
    public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
        return placeReinforcements(parameters.getProcessType(), is_preview);
    }

    private ChangeResult placeReinforcements(PrismProcessType processType, boolean is_preview) {
        // The result of the action. By default nothing occurs, and so it is null until an action is taken.
        ChangeResultType result = null;

        // If the action is simply a preview, nothing happens.
        if (is_preview) {
            return new ChangeResult(ChangeResultType.PLANNED, null);
        }

        // If the event is not a BlockSaver ENFORCE or DAMAGE event being applied, then it is of no consequence.
        if (!BlockSaverPrismBridge.ENFORCE_EVENT_NAME.equals(getType().getName()) && !BlockSaverPrismBridge.DAMAGE_EVENT_NAME.equals(getType().getName())) {
            return new ChangeResult(null, null);
        }

        // If the process is not a ROLLBACK or a RESTORE (e.g. a DRAIN), then it is of no consequence.
        if (!PrismProcessType.ROLLBACK.equals(processType) && !PrismProcessType.RESTORE.equals(processType)) {
            return new ChangeResult(null, null);
        }

        // For each location in the parameters list we ROLLBACK or RESTORE the actions. This is probably unnecessary as each process should be called for each location.
        /*for (Location location : parameters.getSpecificBlockLocations()) {
            if (PrismProcessType.ROLLBACK.equals(pt)) {
                result = rollback(location, parameters, result);
            } else if (PrismProcessType.RESTORE.equals(pt)) {
                result = restore(location, parameters, result);
            }
        }*/

        // If the reinforcement or damage event being rolled back or restored is older than the current existing reinforcement, then it does not need to occur.
        if (reinforcementManager.isReinforced(getLoc())) {
            if (reinforcementManager.getReinforcement(getLoc()).getCreationTime() > actionData.creationTime) {
                return new ChangeResult(ChangeResultType.SKIPPED, null);
            }
        }

        if (PrismProcessType.ROLLBACK.equals(processType)) {
            result = rollback();
        } else if (PrismProcessType.RESTORE.equals(processType)) {
            result = restore();
        }

        return new ChangeResult(result, null);
    }

    private ChangeResultType rollback() {
        // Perform a ROLLBACK for a BlockSaver ENFORCE event (de-enforces the block).
        if (BlockSaverPrismBridge.ENFORCE_EVENT_NAME.equals(getType().getName())) {
            if (!reinforcementManager.isReinforced(getLoc())) {
                // If the block is not reinforced, then the rollback can be assumed to be completed.
                // In a practice, this will only occur when something has gone wrong (e.g. an out-of-order rollback) or when the same rollback is being executed twice.
                // TODO: Confirm whether or not this should be considered APPLIED or SKIPPED.
                return ChangeResultType.APPLIED;
            }

            // If the block is reinforced, then we must confirm that this is the reinforcement to be removed before we continue.
            // If the current reinforcement belongs to the person whose enforcement is being rolled back, then it is removed.
            // Otherwise it remains because it must not be the reinforcement intended to be removed.
            if (getPlayerName().equals(reinforcementManager.getReinforcement(getLoc()).getCreatorName())) {
                reinforcementManager.removeReinforcement(getLoc());
                return ChangeResultType.APPLIED;
            }
        } else {
            // We restore the reinforcement prior to the damage if one does not exist at that location currently.
            if (!reinforcementManager.isReinforced(getLoc())) {
                reinforcementManager.reinforce(getLoc(), actionData.owner, 1);
                // The restored block must have the same creation time as the destroyed one.
                reinforcementManager.getReinforcement(getLoc()).setCreationTime(actionData.creationTime);
                return ChangeResultType.APPLIED;
            }

            // If the same person owns the reinforcement now as the one who did when it was broken, then the damage event probably occurred on this block, and so must be rolled back.
            if (actionData.owner.equals(reinforcementManager.getReinforcement(getLoc()).getCreatorName())) {
                reinforcementManager.reinforce(getLoc(), actionData.owner, 1);
                return ChangeResultType.APPLIED;
            }
        }

        return ChangeResultType.SKIPPED;
    }

    private ChangeResultType restore() {
        if (BlockSaverPrismBridge.ENFORCE_EVENT_NAME.equals(getType().getName())) {
            // If there is no existing reinforcement, then the restoration can proceed without problems.
            if (!reinforcementManager.isReinforced(getLoc())) {
                reinforcementManager.reinforce(getLoc(), actionData.owner);
                return ChangeResultType.APPLIED;
            }

            // The already existing reinforcement is removed and replaced by the one being restored.
            reinforcementManager.removeReinforcement(getLoc());
            reinforcementManager.reinforce(getLoc(), actionData.owner);
            return ChangeResultType.APPLIED;
        } else {
            // If there is no existing reinforcement, then the restoration cannot proceed.
            // In fact, this should never occur.
            if (!reinforcementManager.isReinforced(getLoc())) {
                // If the block is not reinforced, then the restoration of damage can be assumed to be completed.
                // In a practice, this will only occur when something has gone wrong (e.g. an out-of-order restore) or when the same restore is being executed twice.
                // TODO: Confirm whether or not this should be considered APPLIED or SKIPPED.
                return ChangeResultType.APPLIED;
            }

            reinforcementManager.reinforce(getLoc(), getPlayerName(), -1);
            return ChangeResultType.APPLIED;
        }
    }
}
