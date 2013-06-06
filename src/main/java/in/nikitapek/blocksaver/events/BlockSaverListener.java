package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverDamageCause;
import in.nikitapek.blocksaver.util.BlockSaverFeedback;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;
import java.util.ListIterator;

public final class BlockSaverListener implements Listener {
    private final ReinforcementManager reinforcementManager;

    private final boolean tntDamagesReinforcedBlocks;
    private final boolean tntStripReinforcementEntirely;
    private final boolean liquidsDestroyReinforcedBlocks;
    private final boolean allowReinforcedBlockPhysics;
    private final boolean fireDamagesReinforcedBlocks;
    private final boolean extinguishReinforcementFire;
    private final boolean pistonsMoveReinforcedBlocks;
    private final boolean allowBlockFading;
    private final boolean leaveBlockAfterDeinforce;
    private final boolean mobsInteractWithReinforcedBlocks;
    private final boolean enderdragonInteractWithReinforcedBlocks;
    private final double extinguishChance;

    public BlockSaverListener(final BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();

        this.tntDamagesReinforcedBlocks = configurationContext.tntDamagesReinforcedBlocks;
        this.tntStripReinforcementEntirely = configurationContext.tntStripReinforcementEntirely;
        this.liquidsDestroyReinforcedBlocks = configurationContext.liquidsDestroyReinforcedBlocks;
        this.allowReinforcedBlockPhysics = configurationContext.allowReinforcedBlockPhysics;
        this.fireDamagesReinforcedBlocks = configurationContext.fireDamagesReinforcedBlocks;
        this.extinguishReinforcementFire = configurationContext.extinguishReinforcementFire;
        this.pistonsMoveReinforcedBlocks = configurationContext.pistonsMoveReinforcedBlocks;
        this.allowBlockFading = configurationContext.allowBlockFading;
        this.leaveBlockAfterDeinforce = configurationContext.leaveBlockAfterDeinforce;
        this.mobsInteractWithReinforcedBlocks = configurationContext.mobsInteractWithReinforcedBlocks;
        this.enderdragonInteractWithReinforcedBlocks = configurationContext.enderdragonInteractWithReinforcedBlocks;
        this.extinguishChance = configurationContext.extinguishChance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        // If a block is being placed somewhere where there is already a reinforcement value, the reinforcement value is removed.
        // This is to prevent "reinforcement transfers" to blocks which could not normally obtain reinforcements.
        if (!reinforcementManager.isReinforced(event.getBlock().getLocation())) {
            return;
        }

        reinforcementManager.removeReinforcement(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final Player player = event.getPlayer();

        // If the block is not reinforced, this plugin does not stop the block break event.
        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // Cancel the event before the diamond pickaxe check because reinforced blocks should not be breakable without one.
        event.setCancelled(true);

        if (!reinforcementManager.canToolBreakBlock(block.getType(), player.getItemInHand())) {
            reinforcementManager.sendFeedback(location, BlockSaverFeedback.DAMAGE_FAIL, player);
            return;
        }

        reinforcementManager.damageBlock(location, player.getName(), BlockSaverDamageCause.TOOL);

        // If the block is not reinforced, and blocks break when their RV reaches 0, we break the block.
        if (!reinforcementManager.isReinforced(location) && !leaveBlockAfterDeinforce) {
            event.setCancelled(false);
        }

        reinforcementManager.sendFeedback(location, BlockSaverFeedback.DAMAGE_SUCCESS, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(final PlayerInteractEvent event) {
        // If the player is left-clicking, the event is tested to see if it is an attempt to damage a reinforced block.
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        // If the block the player is holding cannot be used for reinforcement, the event is of no relevance.
        if (!reinforcementManager.canMaterialReinforce(event.getPlayer().getItemInHand().getType())) {
            if (event.getClickedBlock() == null) {
                return;
            }

            if (!reinforcementManager.isReinforced(event.getClickedBlock().getLocation())) {
                return;
            }

            if (!reinforcementManager.canToolBreakBlock(event.getClickedBlock().getType(), event.getPlayer().getItemInHand())) {
                reinforcementManager.sendFeedback(event.getClickedBlock().getLocation(), BlockSaverFeedback.HIT_FAIL, event.getPlayer());
                event.setCancelled(true);
            }

            return;
        }

        // An attempt is made to reinforce the block the player clicks, which, if not successful, exits the event.
        if (!reinforcementManager.attemptReinforcement(event.getClickedBlock(), event.getPlayer().getItemInHand().getType(), event.getPlayer().getName())) {
            reinforcementManager.sendFeedback(event.getClickedBlock().getLocation(), BlockSaverFeedback.REINFORCE_FAIL, event.getPlayer());
            return;
        }

        // The amount of the reinforcement material in the player's hand is decreased.
        if (!event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            if (event.getPlayer().getItemInHand().getAmount() > 1) {
                event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
            } else {
                event.getPlayer().getInventory().remove(event.getPlayer().getItemInHand());
            }
        }

        reinforcementManager.sendFeedback(event.getClickedBlock().getLocation(), BlockSaverFeedback.REINFORCE_SUCCESS, event.getPlayer());

        // The event is then cancelled.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        // If the block is not reinforced, it is allowed to burn normally.
        if (!reinforcementManager.isReinforced(event.getBlock().getLocation())) {
            return;
        }

        // If the block is reinforced, the burn event is cancelled for the block.
        event.setCancelled(true);

        // If fire is not allowed to damage blocks, the block damage fail effect is played and the event is exited.
        // Otherwise, the block successfully damaged event is played.
        if (!fireDamagesReinforcedBlocks) {
            reinforcementManager.sendFeedback(event.getBlock().getLocation(), BlockSaverFeedback.DAMAGE_FAIL, null);
            return;
        } else {
            reinforcementManager.sendFeedback(event.getBlock().getLocation(), BlockSaverFeedback.DAMAGE_SUCCESS, null);
        }

        // The block reinforcement is then damaged.
        reinforcementManager.damageBlock(event.getBlock().getLocation(), null, BlockSaverDamageCause.FIRE);

        if (!extinguishReinforcementFire) {
            return;
        }

        if (Math.random() > extinguishChance) {
            return;
        }

        for (final BlockFace face : BlockFace.values()) {
            final Block relative = event.getBlock().getRelative(face);
            if (relative.getType() == Material.FIRE) {
                relative.setType(Material.AIR);
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        if (event.blockList().isEmpty()) {
            return;
        }

        Entity entity = event.getEntity();

        // Make sure that we can later check if the entity is a dragon.
        if (entity instanceof EnderDragonPart) {
            entity = ((EnderDragonPart) entity).getParent();
        }

        // If the event is caused by neither TNT nor a dragon, nor a wither, it is of no relevance.
        if (!EntityType.PRIMED_TNT.equals(entity.getType()) && !EntityType.ENDER_DRAGON.equals(entity.getType()) && !EntityType.WITHER.equals(entity.getType()) && !EntityType.WITHER_SKULL.equals(entity.getType())) {
            return;
        }

        for (final Iterator<Block> iter = event.blockList().iterator(); iter.hasNext();) {
            final Block block = iter.next();

            if (!reinforcementManager.isReinforced(block.getLocation())) {
                continue;
            }

            // If TNT damage is enabled for reinforced blocks, then the block is damaged and the successful damage effect is played.
            // Otherwise, the damage failed is played. In both cases, the block is not destroyed by the blast.
            if (EntityType.PRIMED_TNT.equals(entity.getType()) && tntDamagesReinforcedBlocks) {
                reinforcementManager.sendFeedback(block.getLocation(), BlockSaverFeedback.DAMAGE_SUCCESS, null);
                if (tntStripReinforcementEntirely) {
                    reinforcementManager.removeReinforcement(block.getLocation());
                } else {
                    reinforcementManager.damageBlock(block.getLocation(), null, BlockSaverDamageCause.TNT);
                }
            } else if (EntityType.WITHER.equals(entity.getType()) || EntityType.WITHER_SKULL.equals(entity.getType())) {
                if (mobsInteractWithReinforcedBlocks) {
                    reinforcementManager.sendFeedback(block.getLocation(), BlockSaverFeedback.DAMAGE_SUCCESS, null);
                    reinforcementManager.removeReinforcement(block.getLocation());
                    continue;
                }
            } else if (EntityType.ENDER_DRAGON.equals(entity.getType())) {
                if (enderdragonInteractWithReinforcedBlocks) {
                    reinforcementManager.sendFeedback(block.getLocation(), BlockSaverFeedback.DAMAGE_SUCCESS, null);
                    reinforcementManager.removeReinforcement(block.getLocation());
                    continue;
                }
            } else {
                reinforcementManager.sendFeedback(block.getLocation(), BlockSaverFeedback.DAMAGE_FAIL, null);
            }

            iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        // This is to ensure TNT-based physics events are not processed.
        if (!event.getBlock().getType().equals(Material.SAND) && !event.getBlock().getType().equals(Material.GRAVEL)) {
            return;
        }

        if (!reinforcementManager.isReinforced(event.getBlock().getLocation())) {
            return;
        }

        if (!allowReinforcedBlockPhysics) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.removeReinforcement(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        final ListIterator<Block> iter = event.getBlocks().listIterator(event.getBlocks().size());

        while (iter.hasPrevious()) {
            final Block block = iter.previous();

            // If the next block is reinforced and piston reinforced block movement is disabled, the event is cancelled.
            if (reinforcementManager.isReinforced(block.getRelative(event.getDirection()).getLocation())) {
                if (!pistonsMoveReinforcedBlocks) {
                    event.setCancelled(true);
                    return;
                }
            }

            // If the block is not reinforced, we move on to the next block.
            if (!reinforcementManager.isReinforced(block.getLocation())) {
                continue;
            }

            if (!pistonsMoveReinforcedBlocks) {
                event.setCancelled(true);
                return;
            }

            reinforcementManager.moveReinforcement(block, event.getDirection());
        }

        // Handle the reinforcement on the piston itself.
        if (reinforcementManager.isReinforced(event.getBlock().getLocation())) {
            reinforcementManager.moveReinforcement(event.getBlock(), event.getDirection());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }

        final Block block = event.getBlock().getRelative(event.getDirection(), 2);

        if (!reinforcementManager.isReinforced(block.getLocation())) {
            return;
        }

        if (!pistonsMoveReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.moveReinforcement(block, event.getDirection().getOppositeFace());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFade(final BlockFadeEvent event) {
        if (!event.getBlock().getType().equals(Material.SNOW) && !event.getBlock().getType().equals(Material.ICE)) {
            return;
        }

        if (!reinforcementManager.isReinforced(event.getBlock().getLocation())) {
            return;
        }

        if (!allowBlockFading) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.removeReinforcement(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterPassThrough(final BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            return;
        }

        if (!reinforcementManager.isReinforced(event.getToBlock().getLocation())) {
            return;
        }

        if (!liquidsDestroyReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.removeReinforcement(event.getToBlock().getLocation());
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        if (!reinforcementManager.isReinforced(event.getBlock().getLocation())) {
            return;
        }

        if (event.getEntity() == null) {
            return;
        }

        if (EntityType.ENDERMAN.equals(event.getEntity().getType())) {
            // If the enderman is placing a block, ignore the event.
            if (event.getBlock().getType().equals(Material.AIR) && !event.getTo().equals(Material.AIR)) {
                return;
            }

            // If the enderman is picking up a block, and is allowed to do so, the reinforcement is removed from the block.
            if (!event.getBlock().getType().equals(Material.AIR) && event.getTo().equals(Material.AIR)) {
                if (mobsInteractWithReinforcedBlocks) {
                    reinforcementManager.removeReinforcement(event.getBlock().getLocation());
                } else {
                    event.setCancelled(true);
                }
            }
        }

        // Are sheep able to eat grass, and prevent withers from destroying blocks.
        if (EntityType.SHEEP.equals(event.getEntity().getType()) || EntityType.WITHER.equals(event.getEntity().getType()) || EntityType.WITHER_SKULL.equals(event.getEntity().getType())) {
            if (mobsInteractWithReinforcedBlocks) {
                reinforcementManager.removeReinforcement(event.getBlock().getLocation());
            } else {
                event.setCancelled(true);
            }
        }
    }
}
