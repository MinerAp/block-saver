package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.BlockSaverPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockSaverListener implements Listener {
    private final BlockSaverConfigurationContext configurationContext;

    private enum Feedback {
        REINFORCE_SUCCESS,
        REINFORCE_FAIL,
        DAMAGE_SUCCESS,
        DAMAGE_FAIL,
        HIT_FAIL,
    }

    public BlockSaverListener(BlockSaverConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        // If a block is being placed somewhere where there is already a reinforcement value, the reinforcement value is removed.
        // This is to prevent "reinforcement transfers" to blocks which could not normally obtain reinforcements.
        if (!configurationContext.isReinforced(event.getBlock().getLocation()))
            return;

        configurationContext.infoManager.removeReinforcement(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        // If the block is not reinforced, this plugin does not stop the block break event.
        if (!configurationContext.isReinforced(event.getBlock().getLocation()))
            return;

        removeReinforcementIfInvalid(event.getBlock());

        floorReinforcement(event.getBlock());

        // Cancel the event before the diamond pickaxe check because reinforced blocks should not be breakable without one.
        event.setCancelled(true);

        if (!configurationContext.canToolBreakBlock(event.getBlock().getType(), event.getPlayer().getItemInHand().getType())) {
            reinforcementFeedback(event.getBlock().getLocation(), Feedback.DAMAGE_FAIL);
            return;
        }

        configurationContext.infoManager.damageBlock(event.getBlock().getLocation(), event.getPlayer().getName());

        if (configurationContext.useParticleEffects) {
            List<Player> players = new ArrayList<Player>();
            players.add(event.getPlayer());
            ((BlockSaverPlugin) configurationContext.plugin).sendParticleEffect(players, event.getBlock().getLocation());
        } else {
            reinforcementFeedback(event.getBlock().getLocation(), Feedback.DAMAGE_SUCCESS);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(final PlayerInteractEvent event) {
        // If the player is left-clicking, the event is tested to see if it is an attempt to damage a reinforced block.
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;

        // If the block the player is holding cannot be used for reinforcement, the event is of no relevance.
        if (!configurationContext.isReinforcingMaterial(event.getPlayer().getItemInHand().getType())){
            if (event.getClickedBlock() == null)
                return;

            if (!configurationContext.isReinforced(event.getClickedBlock().getLocation()))
                return;

            if (!configurationContext.canToolBreakBlock(event.getClickedBlock().getType(), event.getPlayer().getItemInHand().getType())) {
                reinforcementFeedback(event.getClickedBlock().getLocation(), Feedback.HIT_FAIL);
                event.setCancelled(true);
                return;
            }
        }

        // An attempt is made to reinforce the block the player clicks, which, if not successful, exits the event.
        if (!configurationContext.attemptReinforcement(event.getClickedBlock(), event.getPlayer().getItemInHand().getType(), event.getPlayer().getName())) {
            reinforcementFeedback(event.getClickedBlock().getLocation(), Feedback.REINFORCE_FAIL);
            return;
        }

        // The amount of the reinforcement material in the player's hand is decreased.
        if (event.getPlayer().getItemInHand().getAmount() > 1) {
            event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
        } else {
            event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
        }

        // A "reinforcement successful" sound is played as a reinforceable block was reinforced with a reinforcement material.
        if (configurationContext.useParticleEffects) {
            List<Player> players = new ArrayList<Player>();
            players.add(event.getPlayer());
            ((BlockSaverPlugin) configurationContext.plugin).sendParticleEffect(players, event.getClickedBlock().getLocation());
        } else {
            reinforcementFeedback(event.getClickedBlock().getLocation(), Feedback.REINFORCE_SUCCESS);
        }

        // The event is then cancelled.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        // If the block is not reinforced, it is allowed to burn normally.
        if (!configurationContext.isReinforced(event.getBlock().getLocation()))
            return;

        if (removeReinforcementIfInvalid(event.getBlock()))
            return;

        floorReinforcement(event.getBlock());

        // If the block is reinforced, the burn event is cancelled for the block.
        event.setCancelled(true);

        // If fire is not allowed to damage blocks, the block damage fail effect is played and the event is exited.
        // Otherwise, the block successfully damaged event is played.
        if (!configurationContext.fireDamagesReinforcedBlocks) {
            reinforcementFeedback(event.getBlock().getLocation(), Feedback.DAMAGE_FAIL);
            return;
        } else {
            reinforcementFeedback(event.getBlock().getLocation(), Feedback.DAMAGE_SUCCESS);
        }

        // The block reinforcement is then damaged.
        configurationContext.infoManager.damageBlock(event.getBlock().getLocation(), null);

        if (!configurationContext.extinguishReinforcementFire)
            return;

        if (Math.random() > configurationContext.extinguishChance)
            return;

        for (BlockFace face : BlockFace.values())
        {
            Block relative = event.getBlock().getRelative(face);
            if (relative.getType() == Material.FIRE)
            {
                relative.setType(Material.AIR);
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;

        for (Iterator<Block> iter = event.blockList().iterator(); iter.hasNext();) {
            Block block = iter.next();

            if (!configurationContext.isReinforced(block.getLocation()))
                continue;

            if (removeReinforcementIfInvalid(block))
                continue;

            floorReinforcement(block);

            // If TNT damage is enabled for reinforced blocks, then the block is damaged and the successful damage effect is played.
            // Otherwise, the damage failed is played. In both cases, the block is not destroyed by the blast.
            if (configurationContext.tntDamagesReinforcedBlocks) {
                reinforcementFeedback(block.getLocation(), Feedback.DAMAGE_SUCCESS);
                configurationContext.infoManager.damageBlock(block.getLocation(), null);
            } else {
                reinforcementFeedback(block.getLocation(), Feedback.DAMAGE_FAIL);
            }

            iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(final BlockPhysicsEvent event) {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        ListIterator<Block> iter = event.getBlocks().listIterator(event.getBlocks().size());

        while (iter.hasPrevious()) {
            Block block = iter.previous();

            // If the next block is reinforced and piston reinforced block movement is disabled, the event is cancelled.
            if (configurationContext.isReinforced(block.getRelative(event.getDirection()).getLocation())) {
                // Deletes the reinforcement from the block ahead if it is invalid.
                removeReinforcementIfInvalid(block.getRelative(event.getDirection()));

                if (!configurationContext.pistonsMoveReinforcedBlocks) {
                    event.setCancelled(true);
                    return;
                }
            }

            // If the block is not reinforced, we move on to the next block.
            if (!configurationContext.isReinforced(block.getLocation()))
                continue;

            // We attempt to remove any invalid reinforcements from the block. If any are invalid, we move to the next block.
            if (removeReinforcementIfInvalid(block))
                continue;

            if (!configurationContext.pistonsMoveReinforcedBlocks) {
                event.setCancelled(true);
                return;
            }

            moveReinforcement(block, event.getDirection());
        }

        // Handle the reinforcement on the piston itself.
        if (configurationContext.isReinforced(event.getBlock().getLocation()))
            moveReinforcement(event.getBlock(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky())
            return;

        Block block = event.getBlock().getRelative(event.getDirection(), 2);

        if (!configurationContext.isReinforced(block.getLocation()))
            return;

        if (removeReinforcementIfInvalid(block))
            return;

        if (!configurationContext.pistonsMoveReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        moveReinforcement(block, event.getDirection().getOppositeFace());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFade(final BlockFadeEvent event) {
        if (!event.getBlock().getType().equals(Material.SNOW) && !event.getBlock().getType().equals(Material.ICE))
            return;

        if (!configurationContext.isReinforced(event.getBlock().getLocation()))
            return;

        if (!configurationContext.allowBlockFading) {
            event.setCancelled(true);
            return;
        }

        configurationContext.infoManager.removeReinforcement(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterPassThrough(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG)
            return;

        if (!configurationContext.isReinforced(event.getToBlock().getLocation()))
            return;

        if (removeReinforcementIfInvalid(event.getToBlock()))
            return;

        if (!configurationContext.liquidsDestroyReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        configurationContext.infoManager.removeReinforcement(event.getToBlock().getLocation());
    }

    private void moveReinforcement(Block block, BlockFace direction) {
        Reinforcement previousReinforcement = configurationContext.infoManager.getReinforcement(block.getLocation());
        configurationContext.infoManager.setReinforcement(block.getRelative(direction).getLocation(), configurationContext.infoManager.removeReinforcement(block.getLocation()), previousReinforcement.getCreatorName());
    }

    private boolean removeReinforcementIfInvalid(Block block) {
        // Removes the reinforcement from the un-reinforceable block.
        if (!configurationContext.isMaterialReinforceable(block.getType())) {
            configurationContext.infoManager.removeReinforcement(block.getLocation());
            return true;
        }

        return false;
    }

    private void floorReinforcement(Block block) {
        // If blocks are allowed to accumulate RV, then there is no need to floor the RV.
        if (configurationContext.accumulateReinforcementValues)
            return;

        // Checks to see if the maximum RV is less than the actual RV. If so, floors the RV.
        int maximumReinforcement = configurationContext.getMaterialReinforcementCoefficient(block.getType());

        Reinforcement reinforcement = configurationContext.infoManager.getReinforcement(block.getLocation());

        if (reinforcement == null)
            return;

        if (reinforcement.getReinforcementValue() > maximumReinforcement)
            configurationContext.infoManager.setReinforcement(block.getLocation(), maximumReinforcement, reinforcement.getCreatorName());
    }

    private void reinforcementFeedback(Location location, Feedback feedback) {
        switch (feedback) {
            case REINFORCE_SUCCESS:
                location.getWorld().playSound(location, configurationContext.reinforceSuccessSound, 1.0f, 50f);
                break;
            case REINFORCE_FAIL:
                location.getWorld().playSound(location, configurationContext.reinforceFailSound, 1.0f, 50f);
                break;
            case DAMAGE_SUCCESS:
                location.getWorld().playEffect(location, configurationContext.reinforcementDamageSuccessEffect, 0);
                break;
            case DAMAGE_FAIL:
                location.getWorld().playEffect(location, configurationContext.reinforcementDamageFailEffect, 0);
                break;
            case HIT_FAIL:
                location.getWorld().playSound(location, configurationContext.hitFailSound, 1.0f, 0f);
                break;
            default:
                break;
        }
    }
}
