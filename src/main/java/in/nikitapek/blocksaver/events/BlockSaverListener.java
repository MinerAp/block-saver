package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockSaverListener implements Listener {
    private final BlockSaverConfigurationContext configurationContext;

    public BlockSaverListener(BlockSaverConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!configurationContext.isMaterialReinforceable(event.getBlock().getType()))
            return;

        if (configurationContext.infoManager.getReinforcementValue(event.getBlock().getLocation()) == -1)
            return;

        // Cancel the event before the diamond pickaxe check because reinforced blocks should not be breakable without one.
        event.setCancelled(true);

        // Plays a sound effect to whether or not the players attempt to de-enforce the block was successful.
        if (!event.getPlayer().getItemInHand().getType().equals(Material.DIAMOND_PICKAXE)) {
            event.getPlayer().getWorld().playEffect(event.getBlock().getLocation(), configurationContext.blockBreakFailEffect, 0);
            return;
        } else {
            // TODO: Make the particles appear without the sound (through ProtocolLib).
            event.getPlayer().getWorld().playEffect(event.getBlock().getLocation(), configurationContext.blockReinforcementDamageEffect, 0);
        }

        configurationContext.infoManager.damageBlock(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(final PlayerInteractEvent event) {
        if (!event.getPlayer().getItemInHand().getType().equals(Material.OBSIDIAN))
            return;

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;

        if (!configurationContext.attemptReinforcement(event.getClickedBlock()))
            return;

        event.getPlayer().getWorld().playSound(event.getClickedBlock().getLocation(), configurationContext.blockReinforceSound, 1.0f, 50f);

        if (event.getPlayer().getItemInHand().getAmount() > 1) {
            event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
        } else {
            event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        if (!configurationContext.isMaterialReinforceable(event.getBlock().getType()))
            return;

        if (configurationContext.infoManager.getReinforcementValue(event.getBlock().getLocation()) == -1)
            return;

        event.setCancelled(true);

        event.getBlock().getWorld().playEffect(event.getBlock().getLocation(), configurationContext.blockReinforcementDamageEffect, 0);

        configurationContext.infoManager.damageBlock(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;

        for (Iterator<Block> iter = event.blockList().iterator(); iter.hasNext();) {
            Block block = iter.next();

            if (!configurationContext.isMaterialReinforceable(block.getType()))
                continue;

            if (configurationContext.infoManager.getReinforcementValue(block.getLocation()) == -1)
                continue;

            block.getWorld().playEffect(block.getLocation(), configurationContext.blockReinforcementDamageEffect, 0);

            iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(final BlockPhysicsEvent event) {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (!configurationContext.isMaterialReinforceable(block.getType()))
                continue;

            if (configurationContext.infoManager.getReinforcementValue(block.getLocation()) == -1)
                continue;

            configurationContext.infoManager.setReinforcement(block.getRelative(event.getDirection()).getLocation(), configurationContext.infoManager.getReinforcementValue(block.getLocation()));
            configurationContext.infoManager.removeReinforcement(block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky())
            return;

        if (event.getRetractLocation().getBlock() == null || !configurationContext.isMaterialReinforceable(event.getRetractLocation().getBlock().getType()) || configurationContext.infoManager.getReinforcementValue(event.getRetractLocation().getBlock().getLocation()) == -1)
            return;

        configurationContext.infoManager.setReinforcement(event.getRetractLocation().getBlock().getLocation(), configurationContext.infoManager.getReinforcementValue(event.getRetractLocation().getBlock().getLocation()));
        configurationContext.infoManager.removeReinforcement(event.getRetractLocation().getBlock().getLocation());
    }
}
