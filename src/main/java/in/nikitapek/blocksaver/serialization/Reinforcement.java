package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.BlockSaverPlugin;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;

import org.bukkit.Location;
import org.bukkit.block.Block;

public final class Reinforcement implements Comparable<Reinforcement> {
    private static BlockSaverPlugin plugin;
    private static int gracePeriodTime;
    private static boolean accumulateReinforcementValues;

    private final Location location;
    private final String creatorName;
    private long timeCreated;
    private float value;
    private float lastMaximumValue;
    private long timeStamp;

    public Reinforcement(final Location location, final String creatorName, final float value) {
        this.location = location;
        this.creatorName = creatorName;
        this.timeCreated = System.currentTimeMillis();
        setReinforcementValue(value);
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

    public Block getBlock() {
        return location.getBlock();
    }

    public int getReinforcementValueCoefficient() {
        return plugin.reinforcementManager.getMaterialReinforcementCoefficient(getBlock().getType());
    }

    public Location getLocation() {
        return location;
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

    public float getLastMaximumValue() {
        return  lastMaximumValue;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setReinforcementValue(final float value) {
        final int coefficient = getReinforcementValueCoefficient();

        if (!accumulateReinforcementValues && value > coefficient) {
            this.value = coefficient;
        } else {
            this.value = value;
        }

        lastMaximumValue = Math.max(value, lastMaximumValue);
        updateTimeStamp();
    }

    // TODO: this is only public for rollback functionality. A better system should be put in place to prevent alteration of creation time.
    public void setCreationTime(final long timeCreated) {
        this.timeCreated = timeCreated;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Reinforcement that = (Reinforcement) obj;

        if (timeCreated != that.timeCreated)
            return false;
        if (Float.compare(that.lastMaximumValue, lastMaximumValue) != 0)
            return false;
        if (timeStamp != that.timeStamp)
            return false;
        if (Float.compare(that.value, value) != 0)
            return false;
        if (!creatorName.equals(that.creatorName))
            return false;
        if (!location.equals(that.location))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + (value != +0.0f ? Float.floatToIntBits(value) : 0);
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        result = 31 * result + (int) (timeCreated ^ (timeCreated >>> 32));
        result = 31 * result + creatorName.hashCode();
        result = 31 * result + (lastMaximumValue != +0.0f ? Float.floatToIntBits(lastMaximumValue) : 0);
        return result;
    }

    @Override
    public int compareTo(final Reinforcement obj) {
        int c = Integer.compare(location.getBlockX(), obj.location.getBlockX());
        if (c != 0) {
            return c;
        }

        c = Integer.compare(location.getBlockY(), obj.location.getBlockY());
        if (c != 0) {
            return c;
        }

        return Integer.compare(location.getBlockZ(), obj.location.getBlockZ());
    }
}
