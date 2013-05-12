package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;

public class BlockSaverListener implements Listener {
    private final BlockSaverConfigurationContext configurationContext;

    public BlockSaverListener(BlockSaverConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        // If the block is not reinforced, this plugin does not stop the block break event.
        if (configurationContext.infoManager.getReinforcementValue(event.getBlock().getLocation()) == -1)
            return;

        // Removes the reinforcement from the un-reinforceable block.
        if (!configurationContext.isMaterialReinforceable(event.getBlock().getType())) {
            configurationContext.infoManager.removeReinforcement(event.getBlock().getLocation());
            return;
        }

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
        // If the player is not left-clicking, the event is of no relevance.
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;

        // If the block the player is holding cannot be used for reinforcement, the event is of no relevance.
        if (!configurationContext.isReinforcingMaterial(event.getPlayer().getItemInHand().getType()))
            return;

        // An attempt is made to reinforce the block the player clicks, which, if not successful, exits the event.
        if (!configurationContext.attemptReinforcement(event.getClickedBlock(), event.getPlayer().getItemInHand().getType()))
            return;

        // A "reinforcement successful" (pitch-shifted +50) sound is played as a reinforceable block was reinforced with a reinforcement material.
        event.getPlayer().getWorld().playSound(event.getClickedBlock().getLocation(), configurationContext.blockReinforceSound, 1.0f, 50f);

        // The amount of the reinforcement material in the player's hand is decreased.
        if (event.getPlayer().getItemInHand().getAmount() > 1) {
            event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
        } else {
            event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
        }

        // The event is then cancelled.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        // If the block is not reinforced, it is allowed to burn normally.
        if (configurationContext.infoManager.getReinforcementValue(event.getBlock().getLocation()) == -1)
            return;

        // Removes the reinforcement from the un-reinforceable block.
        if (!configurationContext.isMaterialReinforceable(event.getBlock().getType())) {
            configurationContext.infoManager.removeReinforcement(event.getBlock().getLocation());
            return;
        }

        // If the block is reinforced, the burn event is cancelled for the block.
        event.setCancelled(true);

        // If fire is not allowed to damage blocks, the block damage fail effect is played and the event is exited.
        // Otherwise, the block successfully damaged event is played.
        if (!configurationContext.fireDamagesReinforcedBlocks) {
            event.getBlock().getWorld().playEffect(event.getBlock().getLocation(), configurationContext.blockBreakFailEffect, 0);
            return;
        } else {
            event.getBlock().getWorld().playEffect(event.getBlock().getLocation(), configurationContext.blockReinforcementDamageEffect, 0);
        }

        // The block reinforcement is then damaged.
        configurationContext.infoManager.damageBlock(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;

        for (Iterator<Block> iter = event.blockList().iterator(); iter.hasNext();) {
            Block block = iter.next();

            // If the block is not reinforced, then it is ignored.
            if (configurationContext.infoManager.getReinforcementValue(block.getLocation()) == -1)
                continue;

            // Removes the reinforcement from the un-reinforceable block.
            if (!configurationContext.isMaterialReinforceable(block.getType())) {
                configurationContext.infoManager.removeReinforcement(block.getLocation());
                continue;
            }

            // If TNT damage is enabled for reinforced blocks, then the block is damaged and the successful damage effect is played.
            // Otherwise, the damage failed is played. In both cases, the block is not destroyed by the blast.
            if (configurationContext.tntDamagesReinforcedBlocks) {
                block.getWorld().playEffect(block.getLocation(), configurationContext.blockReinforcementDamageEffect, 0);

                configurationContext.infoManager.damageBlock(block.getLocation());
            } else {
                block.getWorld().playEffect(block.getLocation(), configurationContext.blockBreakFailEffect, 0);
            }

            iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(final BlockPhysicsEvent event) {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (configurationContext.infoManager.getReinforcementValue(block.getLocation()) == -1)
                continue;

            // Removes the reinforcement from the un-reinforceable block.
            if (!configurationContext.isMaterialReinforceable(block.getType())) {
                configurationContext.infoManager.removeReinforcement(block.getLocation());
                continue;
            }

            if (!configurationContext.pistonsMoveReinforcedBlocks) {
                event.setCancelled(true);
                return;
            }

            configurationContext.infoManager.setReinforcement(block.getRelative(event.getDirection()).getLocation(), configurationContext.infoManager.getReinforcementValue(block.getLocation()));
            configurationContext.infoManager.removeReinforcement(block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky())
            return;

        MaterialData data = event.getBlock().getState().getData();
        BlockFace direction = null;

        if (data instanceof PistonBaseMaterial) {
            direction = ((PistonBaseMaterial) data).getFacing();
        }

        if (direction == null)
            return;

        Block block = event.getBlock().getRelative(direction, 2);

        if (configurationContext.infoManager.getReinforcementValue(block.getLocation()) == -1)
            return;

        // Removes the reinforcement from the un-reinforceable block.
        if (!configurationContext.isMaterialReinforceable(block.getType())) {
            configurationContext.infoManager.removeReinforcement(block.getLocation());
            return;
        }

        if (!configurationContext.pistonsMoveReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        configurationContext.infoManager.setReinforcement(event.getBlock().getRelative(direction, 1).getLocation(), configurationContext.infoManager.getReinforcementValue(block.getLocation()));
        configurationContext.infoManager.removeReinforcement(block.getLocation());
    }
}
