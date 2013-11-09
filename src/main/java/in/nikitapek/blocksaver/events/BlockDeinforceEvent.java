package in.nikitapek.blocksaver.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BlockDeinforceEvent extends Event {
    protected long time;
    protected Block reinforcedBlock;
    protected String playerName;
    protected boolean isLogged;

    public BlockDeinforceEvent(Block reinforcedBlock) {
        this(reinforcedBlock, null, false);
    }

    public BlockDeinforceEvent(Block reinforcedBlock, String playerName, boolean isLogged) {
        this(System.currentTimeMillis(), reinforcedBlock, playerName, isLogged);
    }

    public BlockDeinforceEvent(Long time, Block reinforcedBlock, String playerName, boolean isLogged) {
        this.time = time;
        this.reinforcedBlock = reinforcedBlock;
        this.playerName = playerName;
        this.isLogged = isLogged;
    }

    public Long getTime() {
        return time;
    }

    public Block getBlock() {
        return reinforcedBlock;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isLogged() {
        return isLogged;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }
}
