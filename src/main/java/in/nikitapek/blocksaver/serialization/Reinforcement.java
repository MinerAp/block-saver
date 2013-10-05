package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.BlockSaverPlugin;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;

public final class Reinforcement {
    private static BlockSaverPlugin plugin;
    private static int gracePeriodTime;
    private static boolean accumulateReinforcementValues;

    private transient final String creatorName;
    private transient long timeCreated;
    private transient float value;
    private transient long timeStamp;

    public Reinforcement(final String creatorName, final float value) {
        this.creatorName = creatorName;
        this.timeCreated = System.currentTimeMillis();
        setReinforcementValue(value, Float.MAX_VALUE);
    }

    public static void initialize(BlockSaverConfigurationContext configurationContext) {
        plugin = (BlockSaverPlugin) configurationContext.plugin;
        gracePeriodTime = configurationContext.gracePeriodTime;
        accumulateReinforcementValues = configurationContext.accumulateReinforcementValues;
    }

    private void updateTimeStamp() {
        this.timeStamp = System.currentTimeMillis();
    }

    public boolean isJustCreated() {
        return (System.currentTimeMillis() - getCreationTime()) < (gracePeriodTime * BlockSaverUtil.MILLISECONDS_PER_SECOND);
    }

    public String getCreatorName() {
        return creatorName;
    }

    public long getCreationTime() {
        return timeCreated;
    }

    public float getReinforcementValue() {
        return value;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setReinforcementValue(final float value, final float coefficient) {
        if (!accumulateReinforcementValues && value > coefficient) {
            this.value = coefficient;
        } else {
            this.value = value;
        }

        updateTimeStamp();
    }

    // TODO: this is only public for rollback functionality. A better system should be put in place to prevent alteration of creation time.
    public void setCreationTime(final long timeCreated) {
        this.timeCreated = timeCreated;
    }
}
