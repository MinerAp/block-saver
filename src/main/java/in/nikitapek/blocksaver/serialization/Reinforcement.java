package in.nikitapek.blocksaver.serialization;

import com.amshulman.mbapi.MbapiPlugin;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;

public final class Reinforcement implements Comparable<Reinforcement> {
    private final Location location;
    private float value;
    private long timeStamp;
    private String creatorName;
    private float lastMaximumValue;
    private long timeCreated;

    private static MbapiPlugin plugin;
    private static int gracePeriodTime;

    public Reinforcement(final Location location) {
        this.location = location;
        setReinforcementValue(getReinforcementValue());
        setCreatorName(getCreatorName());
        setLastMaximumValue(getLastMaximumValue());
        setCreationTime(getCreationTime());
    }

    public Reinforcement(final Location location, final float value, final String creatorName) {
        this.location = location;
        setReinforcementValue(value);
        setCreatorName(creatorName);
        setCreationTime(System.currentTimeMillis());
    }

    public static void initialize(BlockSaverConfigurationContext configurationContext) {
        plugin = configurationContext.plugin;
        gracePeriodTime = configurationContext.gracePeriodTime;
    }

    public void updateTimeStamp() {
        setTimeStamp(System.currentTimeMillis());
    }

    public void writeToMetadata() {
        getBlock().setMetadata("RV", new FixedMetadataValue(plugin, value));
        getBlock().setMetadata("RTS", new FixedMetadataValue(plugin, timeStamp));
        getBlock().setMetadata("RCN", new FixedMetadataValue(plugin, creatorName));
        getBlock().setMetadata("RLMV", new FixedMetadataValue(plugin, lastMaximumValue));
        getBlock().setMetadata("RTC", new FixedMetadataValue(plugin, timeCreated));
    }

    public static void removeFromMetadata(final Block block) {
        block.removeMetadata("RV", plugin);
        block.removeMetadata("RTS", plugin);
        block.removeMetadata("RCN", plugin);
        block.removeMetadata("RLMV", plugin);
        block.removeMetadata("RTC", plugin);
    }

    public Block getBlock() {
        return location.getBlock();
    }

    public Location getLocation() {
        return location;
    }

    public float getReinforcementValue() {
        if (!getBlock().hasMetadata("RV")) {
            throw new IllegalArgumentException("An RV for a non-reinforced block was attempted to be retrieved");
        }

        value = getBlock().getMetadata("RV").get(0).asInt();
        return value;
    }

    public long getTimeStamp() {
        if (!getBlock().hasMetadata("RTS")) {
            setTimeStamp(System.currentTimeMillis());
            // throw new IllegalArgumentException("An RTS for a non-reinforced block was attempted to be retrieved");
        }

        timeStamp = getBlock().getMetadata("RTS").get(0).asLong();
        return timeStamp;
    }

    public boolean isJustCreated() {
        return (System.currentTimeMillis() - getCreationTime()) >= (gracePeriodTime * BlockSaverUtil.MILLISECONDS_PER_SECOND);
    }

    public long getCreationTime() {
        if (!getBlock().hasMetadata("RCT")) {
            throw new IllegalArgumentException("An RCT for a non-reinforced block was attempted to be retrieved");
        }

        return getBlock().getMetadata("RCT").get(0).asLong();
    }

    public String getCreatorName() {
        if (!getBlock().hasMetadata("RTS")) {
            throw new IllegalArgumentException("An RCN for a non-reinforced block was attempted to be retrieved");
        }

        creatorName = getBlock().getMetadata("RCN").get(0).asString();
        return creatorName;
    }

    public float getLastMaximumValue() {
        if (!getBlock().hasMetadata("RLMV")) {
            setLastMaximumValue(getReinforcementValue());
            // throw new IllegalArgumentException("An RLMV for a non-reinforced block was attempted to be retrieved");
        }

        lastMaximumValue = getBlock().getMetadata("RLMV").get(0).asInt();
        return lastMaximumValue;
    }

    public void setReinforcementValue(final float value) {
        this.value = value;

        getBlock().setMetadata("RV", new FixedMetadataValue(plugin, value));

        setLastMaximumValue(Math.max(value, getLastMaximumValue()));
        updateTimeStamp();
    }

    private void setTimeStamp(final long timeStamp) {
        this.timeStamp = timeStamp;
        getBlock().setMetadata("RTS", new FixedMetadataValue(plugin, timeStamp));
    }

    private void setCreatorName(final String creatorName) {
        this.creatorName = creatorName;
        getBlock().setMetadata("RCN", new FixedMetadataValue(plugin, creatorName));
    }

    private void setLastMaximumValue(final float lastMaximumValue) {
        this.lastMaximumValue = lastMaximumValue;
        getBlock().setMetadata("RLMV", new FixedMetadataValue(plugin, lastMaximumValue));
    }

    public void setCreationTime(final long creationTime) {
        this.timeCreated = creationTime;
        getBlock().setMetadata("RCT", new FixedMetadataValue(plugin, creationTime));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Reinforcement that = (Reinforcement) obj;

        if (timeCreated != that.timeCreated) return false;
        if (Float.compare(that.lastMaximumValue, lastMaximumValue) != 0) return false;
        if (timeStamp != that.timeStamp) return false;
        if (Float.compare(that.value, value) != 0) return false;
        if (!creatorName.equals(that.creatorName)) return false;
        if (!location.equals(that.location)) return false;

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
