package in.nikitapek.blocksaver.events;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class BlockReinforceEvent extends BlockEvent {
    private static final HandlerList handlers = new HandlerList();

    protected long time;
    protected String playerName;
    protected boolean isLogged;

    public BlockReinforceEvent(Block reinforcedBlock, String playerName, boolean isLogged) {
        this(System.currentTimeMillis(), reinforcedBlock, playerName, isLogged);
    }

    public BlockReinforceEvent(Long time, Block reinforcedBlock, String playerName, boolean isLogged) {
        super(reinforcedBlock);
        this.time = time;
        this.playerName = playerName;
        this.isLogged = isLogged;
    }

    public Long getTime() {
        return time;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isLogged() {
        return isLogged;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
