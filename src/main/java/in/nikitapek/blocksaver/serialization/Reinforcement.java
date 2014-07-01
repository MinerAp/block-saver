package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;

public final class Reinforcement {
    private static int gracePeriodTime;

    private transient final String creatorName;
    private transient long timeCreated;
    private transient float value = BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT;
    private transient long timeStamp;

    // Only gson will call this
    private Reinforcement() {
        this(null, BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT);
    }

    public Reinforcement(final String creatorName, float value) {
        this.creatorName = creatorName;
        this.timeCreated = System.currentTimeMillis();
        setReinforcementValue(value, Float.MAX_VALUE);
    }

    public static void initialize(BlockSaverConfigurationContext configurationContext) {
        gracePeriodTime = configurationContext.gracePeriodTime;
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

    public float getReinforcementValue(final float coefficient) {
        // If the block has been recently restored or created, set its RV to RVC.
        if (this.value == BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
            this.value = coefficient;
        }

        return value;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    void setReinforcementValue(final float value, final float coefficient) {
        // If the block has been recently restored or created, set its RV to RVC.
        if (this.value == BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
            this.value = coefficient;
        }

        // Ensure that the RV is not being set above RVC.
        if (value > coefficient) {
            this.value = coefficient;
        } else {
            // Set the RV to the requested value.
            this.value = value;
        }

        updateTimeStamp();
    }

    // TODO: this is only public for rollback functionality. A better system should be put in place to prevent alteration of creation time.
    public void setCreationTime(final long timeCreated) {
        this.timeCreated = timeCreated;
    }
}
