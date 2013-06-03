package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverUtil;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;

public final class Reinforcement implements Comparable<Reinforcement> {
    private final Location location;
    private float value;
    private long timeStamp;
    private boolean justCreated;
    private String creatorName;
    private float lastMaximumValue;

    public Reinforcement(final Location location) {
        this.location = location;
        setReinforcementValue(getReinforcementValue());
        setJustCreated(isJustCreated());
        setCreatorName(getCreatorName());
        setLastMaximumValue(getLastMaximumValue());
    }

    public Reinforcement(final Location location, final float value, final String creatorName) {
        this.location = location;
        setReinforcementValue(value);
        setJustCreated(true);
        setCreatorName(creatorName);
    }

    public void updateTimeStamp() {
        if ((System.currentTimeMillis() - getTimeStamp()) >= (BlockSaverConfigurationContext.configurationContext.gracePeriodTime * BlockSaverUtil.MILLISECONDS_PER_SECOND)) {
            setJustCreated(false);
        }

        setTimeStamp(System.currentTimeMillis());
    }

    public void writeToMetadata() {
        getBlock().setMetadata("RV", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, value));
        getBlock().setMetadata("RTS", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, timeStamp));
        getBlock().setMetadata("RJC", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, justCreated));
        getBlock().setMetadata("RCN", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, creatorName));
        getBlock().setMetadata("RLMV", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, lastMaximumValue));
    }

    public static void removeFromMetadata(final Block block) {
        block.removeMetadata("RV", BlockSaverConfigurationContext.configurationContext.plugin);
        block.removeMetadata("RTS", BlockSaverConfigurationContext.configurationContext.plugin);
        block.removeMetadata("RJC", BlockSaverConfigurationContext.configurationContext.plugin);
        block.removeMetadata("RCN", BlockSaverConfigurationContext.configurationContext.plugin);
        block.removeMetadata("RLMV", BlockSaverConfigurationContext.configurationContext.plugin);
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
        if (!getBlock().hasMetadata("RJC")) {
            throw new IllegalArgumentException("An RJC for a non-reinforced block was attempted to be retrieved");
        }

        justCreated = getBlock().getMetadata("RJC").get(0).asBoolean();
        return justCreated;
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
        getBlock().setMetadata("RV", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, value));

        setLastMaximumValue(Math.max(value, getLastMaximumValue()));
        updateTimeStamp();
    }

    private void setTimeStamp(final long timeStamp) {
        this.timeStamp = timeStamp;
        getBlock().setMetadata("RTS", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, timeStamp));
    }

    private void setJustCreated(final boolean justCreated) {
        this.justCreated = justCreated;
        getBlock().setMetadata("RJC", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, justCreated));
    }

    private void setCreatorName(final String creatorName) {
        this.creatorName = creatorName;
        getBlock().setMetadata("RCN", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, creatorName));
    }

    private void setLastMaximumValue(final float lastMaximumValue) {
        this.lastMaximumValue = lastMaximumValue;
        getBlock().setMetadata("RLMV", new FixedMetadataValue(BlockSaverConfigurationContext.configurationContext.plugin, lastMaximumValue));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (int) (prime * result + getReinforcementValue()); // TODO: Ask Andy if this line is right.
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Reinforcement other = (Reinforcement) obj;
        // TODO: Ask Andy if these lines are right.
        if (getReinforcementValue() != other.getReinforcementValue()) {
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(final Reinforcement o) {
        int c = Integer.compare(location.getBlockX(), o.location.getBlockX());
        if (c != 0) {
            return c;
        }

        c = Integer.compare(location.getBlockY(), o.location.getBlockY());
        if (c != 0) {
            return c;
        }

        return Integer.compare(location.getBlockZ(), o.location.getBlockZ());
    }
}
