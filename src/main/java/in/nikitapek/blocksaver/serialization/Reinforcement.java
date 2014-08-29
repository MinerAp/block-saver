package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;

public final class Reinforcement {
    private static int gracePeriodTime;

    private transient final String creatorName;
    private transient long timeCreated;

    // Only gson will call this.
    @SuppressWarnings("unused")
	private Reinforcement() {
        this(null);
    }

    public Reinforcement(final String creatorName) {
        this.creatorName = creatorName;
        this.timeCreated = System.currentTimeMillis();
    }

    public static void initialize(BlockSaverConfigurationContext configurationContext) {
        gracePeriodTime = configurationContext.gracePeriodTime;
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

    // TODO: this is only public for rollback functionality. A better system should be put in place to prevent alteration of creation time.
    public void setCreationTime(final long timeCreated) {
        this.timeCreated = timeCreated;
    }
}
