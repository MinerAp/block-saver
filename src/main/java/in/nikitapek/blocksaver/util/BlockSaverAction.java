package in.nikitapek.blocksaver.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.BlockAction;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BlockSaverAction extends BlockAction {
    private static BlockSaverInfoManager infoManager;
    private static ReinforcementManager reinforcementManager;

    public class ReinforcementActionData extends BlockActionData {
        public String owner;
        public long creationTime;
    }

    private Reinforcement reinforcement;

    private ReinforcementActionData actionData;

    private Gson gson1 = new GsonBuilder().disableHtmlEscaping().create();

    public static void initialize(ReinforcementManager reinforcementManager, BlockSaverInfoManager infoManager) {
        BlockSaverAction.infoManager = infoManager;
        BlockSaverAction.reinforcementManager = reinforcementManager;
    }

    public void setReinforcement(Location location, Reinforcement reinforcement) {
        actionData = new ReinforcementActionData();

        if (reinforcement == null) {
            this.setCanceled(true);
            return;
        }

        this.reinforcement = reinforcement;

        this.block_id = location.getBlock().getTypeId();
        this.setLoc(location);
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
            if (getPlayerName().equals(infoManager.getReinforcement(getLoc()).getCreatorName())) {
                infoManager.removeReinforcement(getLoc());
                return ChangeResultType.APPLIED;
            }
        } else {
            // We restore the reinforcement prior to the damage if one does not exist at that location currently.
            if (!reinforcementManager.isReinforced(getLoc())) {
                infoManager.reinforce(getLoc(), actionData.owner, reinforcementManager.getMaterialReinforcementCoefficient(getLoc().getBlock().getType()));
                // The restored block must have the same creation time as the destroyed one.
                infoManager.getReinforcement(getLoc()).setCreationTime(actionData.creationTime);
                return ChangeResultType.APPLIED;
            }

            // If the same person owns the reinforcement now as the one who did when it was broken, then the damage event probably occurred on this block, and so must be rolled back.
            if (actionData.owner.equals(infoManager.getReinforcement(getLoc()).getCreatorName())) {
                infoManager.reinforce(getLoc(), actionData.owner, reinforcementManager.getMaterialReinforcementCoefficient(getLoc().getBlock().getType()));
                return ChangeResultType.APPLIED;
            }
        }

        return ChangeResultType.SKIPPED;
    }

    private ChangeResultType restore() {
        if (BlockSaverPrismBridge.ENFORCE_EVENT_NAME.equals(getType().getName())) {
            infoManager.reinforce(getLoc(), actionData.owner, reinforcementManager.getMaterialReinforcementCoefficient(getLoc().getBlock().getType()));
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

            infoManager.removeReinforcement(getLoc());
            return ChangeResultType.APPLIED;
        }
    }
}
