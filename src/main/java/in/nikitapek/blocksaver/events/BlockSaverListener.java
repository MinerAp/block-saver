package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.SupplimentaryTypes;

import java.util.HashMap;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public class BlockSaverListener implements Listener {

    private final BlockSaverConfigurationContext configurationContext;
    private TypeSafeMap<Block, Byte> reinforcedBlocks;
    private final byte breakCount = 3;
    //private TypeSafeSet<Block> reinforcedBlocks;

    public BlockSaverListener(BlockSaverConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
        
        reinforcedBlocks = new TypeSafeMapImpl<Block, Byte>(new HashMap<Block, Byte>(), SupplimentaryTypes.BLOCK, SupplimentaryTypes.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!configurationContext.reinforceableBlocks.contains(event.getBlock().getType()))
            return;

        if (!reinforcedBlocks.containsKey(event.getBlock()))
            return;

        // Cancel the event before the diamond pickaxe check because reinforced blocks should not be breakable without one.
        event.setCancelled(true);

        // Plays a sound effect to whether or not the players attempt to de-enforce the block was successful.
        if (!event.getPlayer().getItemInHand().getType().equals(Material.DIAMOND_PICKAXE)) {
            event.getPlayer().playEffect(event.getBlock().getLocation(), Effect.EXTINGUISH, 0);
            return;
        } else {
            event.getPlayer().playEffect(event.getBlock().getLocation(), Effect.POTION_BREAK, 0);
        }

        if (reinforcedBlocks.get(event.getBlock()) > 1) {
            // Can you just modify the get() value after retrieval or is this put() necessary?
            reinforcedBlocks.put(event.getBlock(), (byte) (reinforcedBlocks.get(event.getBlock()) - 1));
            return;
        }

        reinforcedBlocks.remove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(final PlayerInteractEvent event) {
        if (!event.getPlayer().getItemInHand().getType().equals(Material.OBSIDIAN))
            return;

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;

        if (!configurationContext.reinforceableBlocks.contains(event.getClickedBlock().getType()))
            return;
        
        //if (reinforcedBlocks.containsKey(event.getClickedBlock()))
        //    return;

        event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.BURP, 1.0f, 0);

        if (event.getPlayer().getItemInHand().getAmount() > 1) {
            event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
        } else {
            event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
        }

        byte breakValue = breakCount;
        breakValue += reinforcedBlocks.containsKey(event.getClickedBlock()) ? reinforcedBlocks.get(event.getClickedBlock()) : 0;
        
        reinforcedBlocks.put(event.getClickedBlock(), breakValue);
        //reinforcedBlocks.add(event.getClickedBlock());

        event.setCancelled(true);
    }
}
