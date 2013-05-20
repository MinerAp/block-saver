package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class Reinforcement implements Comparable<Reinforcement> {
    private Location location;
    private int value;
    private long timeStamp;
    private boolean justCreated;
    private String creatorName;
    private int lastMaximumValue;

    public Reinforcement(final Location location, final int value, final String creatorName) {
        this.location = location;
        this.value = value;
        updateTimeStamp();
        this.creatorName = creatorName;
        justCreated = true;
        lastMaximumValue = value;
    }

    public Block getBlock() {
        return location.getBlock();
    }

    public Location getLocation() {
        return location;
    }

    public int getReinforcementValue() {
        return value;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public boolean isJustCreated() {
        return justCreated;
    }
    
    public int getLastMaximumValue() {
        return lastMaximumValue;
    }

    public void updateTimeStamp() {
        if ((System.currentTimeMillis() - timeStamp) >= (BlockSaverConfigurationContext.configurationContext.gracePeriodTime * 1000))
            justCreated = false;

        timeStamp = System.currentTimeMillis();
    }

    public void updateLastMaximumValue() {
        lastMaximumValue = value;
    }

    public void setReinforcementValue(int value) {
        this.value = value;
        lastMaximumValue = Math.max(value, lastMaximumValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value; // TODO: Ask Andy if this line is right.
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Reinforcement other = (Reinforcement) obj;
        // TODO: Ask Andy if these lines are right.
        if (value != other.value) {
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
    public int compareTo(Reinforcement o) {
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
