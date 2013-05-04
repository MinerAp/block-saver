package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.SupplimentaryTypes;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public class BlockSaverListener implements Listener {

    private final BlockSaverConfigurationContext configurationContext;
    private TypeSafeMap<Block, Byte> reinforcedBlocks;

    private final Effect blockBreakFailEffect = Effect.EXTINGUISH;
    private final Effect reinforcedBlockDamageEffect = Effect.POTION_BREAK;
    private final Sound blockReinforceSound = Sound.BURP;

    public BlockSaverListener(BlockSaverConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
        
        reinforcedBlocks = new TypeSafeMapImpl<Block, Byte>(new HashMap<Block, Byte>(), SupplimentaryTypes.BLOCK, SupplimentaryTypes.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!configurationContext.reinforceableBlocks.containsKey(event.getBlock().getType()))
            return;

        if (!reinforcedBlocks.containsKey(event.getBlock()))
            return;

        // Cancel the event before the diamond pickaxe check because reinforced blocks should not be breakable without one.
        event.setCancelled(true);

        // Plays a sound effect to whether or not the players attempt to de-enforce the block was successful.
        if (!event.getPlayer().getItemInHand().getType().equals(Material.DIAMOND_PICKAXE)) {
            event.getPlayer().getWorld().playEffect(event.getBlock().getLocation(), blockBreakFailEffect, 0);
            return;
        } else {
            // TODO: Make the particles appear without the sound (through ProtocolLib).
            event.getPlayer().getWorld().playEffect(event.getBlock().getLocation(), reinforcedBlockDamageEffect, 0);
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

        if (!configurationContext.reinforceableBlocks.containsKey(event.getClickedBlock().getType()))
            return;

        byte materialCoefficient = configurationContext.reinforceableBlocks.get(event.getClickedBlock().getType());

        // If the block is already maximum reinforced, we do not reinforce it further or use up the obsidian.
        if (reinforcedBlocks.containsKey(event.getClickedBlock()) && reinforcedBlocks.get(event.getClickedBlock()) == materialCoefficient)
            return;

        event.getPlayer().getWorld().playSound(event.getClickedBlock().getLocation(), blockReinforceSound, 1.0f, 50f);

        if (event.getPlayer().getItemInHand().getAmount() > 1) {
            event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
        } else {
            event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
        }

        reinforcedBlocks.put(event.getClickedBlock(), materialCoefficient);

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        if (!configurationContext.reinforceableBlocks.containsKey(event.getBlock().getType()))
            return;

        if (!reinforcedBlocks.containsKey(event.getBlock()))
            return;

        event.setCancelled(true);

        event.getBlock().getWorld().playEffect(event.getBlock().getLocation(), reinforcedBlockDamageEffect, 0);

        if (reinforcedBlocks.get(event.getBlock()) > 1) {
            // Can you just modify the get() value after retrieval or is this put() necessary?
            reinforcedBlocks.put(event.getBlock(), (byte) (reinforcedBlocks.get(event.getBlock()) - 1));
            return;
        }

        reinforcedBlocks.remove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;

        for (Iterator<Block> iter = event.blockList().iterator(); iter.hasNext();) {
            Block block = iter.next();

            if (!configurationContext.reinforceableBlocks.containsKey(block.getType()))
                continue;

            if (!reinforcedBlocks.containsKey(block))
                continue;

            block.getWorld().playEffect(block.getLocation(), reinforcedBlockDamageEffect, 0);

            iter.remove();
            //if (reinforcedBlocks.get(block) > 1) {
            //    // Can you just modify the get() value after retrieval or is this put() necessary?
            //    reinforcedBlocks.put(block, (byte) (reinforcedBlocks.get(block) - 1));
            //    continue;
            //}
    
            //reinforcedBlocks.remove(block);
        }
    }
}
