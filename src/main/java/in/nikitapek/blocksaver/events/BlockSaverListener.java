package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverDamageCause;
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
import org.bukkit.inventory.ItemStack;

import java.util.ListIterator;

public final class BlockSaverListener implements Listener {
    private final ReinforcementManager reinforcementManager;

    private final boolean liquidsDestroyReinforcedBlocks;
    private final boolean allowReinforcedBlockPhysics;
    private final boolean pistonsMoveReinforcedBlocks;
    private final boolean allowBlockFading;
    private final boolean mobsInteractWithReinforcedBlocks;

    public BlockSaverListener(final BlockSaverConfigurationContext configurationContext) {
        this.reinforcementManager = configurationContext.getReinforcementManager();

        this.liquidsDestroyReinforcedBlocks = configurationContext.liquidsDestroyReinforcedBlocks;
        this.allowReinforcedBlockPhysics = configurationContext.allowReinforcedBlockPhysics;
        this.pistonsMoveReinforcedBlocks = configurationContext.pistonsMoveReinforcedBlocks;
        this.allowBlockFading = configurationContext.allowBlockFading;
        this.mobsInteractWithReinforcedBlocks = configurationContext.mobsInteractWithReinforcedBlocks;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        // If a block is being placed somewhere where there is already a reinforcement value, the reinforcement value is removed.
        // This is to prevent "reinforcement transfers" to blocks which could not normally obtain reinforcements.
        final Location location = event.getBlock().getLocation();

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        reinforcementManager.removeReinforcement(location);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();

        // If the block is not reinforced, this plugin does not stop the block break event.
        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // If the block is not successfully broken (e.g. the RV is not 0), then the event is cancelled.
        event.setCancelled(!reinforcementManager.attemptToBreakBlock(location, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        // Confirms that the block is not air.
        if (block == null) {
            return;
        }

        final Location location = block.getLocation();
        final ItemStack item = player.getItemInHand();

        // If the player is not left-clicking, then the player is not attempting to reinforce or damage a block.
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        // If the player is not attempting a reinforcement, they may be trying to damage a reinforced block, and so a check is performed.
        if (!reinforcementManager.canMaterialReinforce(item.getType())) {
            if (!reinforcementManager.canPlayerDamageBlock(location, player)) {
                event.setCancelled(true);
            }
            return;
        }

        // An attempt is made to reinforce the block the player clicks, which, if not successful, exits the event.
        if (!reinforcementManager.attemptReinforcement(location, item.getType(), player)) {
            return;
        }

        // The amount of the reinforcement material in the player's hand is decreased.
        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
        }

        // The event is then cancelled.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        final Location location = event.getBlock().getLocation();

        // If the block is not reinforced, it is allowed to burn normally.
        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // If the block is reinforced, the burn event is cancelled for the block.
        event.setCancelled(true);

        // The block reinforcement is then damaged.
        reinforcementManager.damageBlock(location, null, BlockSaverDamageCause.FIRE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        if (event.blockList().isEmpty()) {
            return;
        }

        final Entity entity;

        // Make sure that we can later check if the entity is a dragon.
        if (event.getEntity() instanceof EnderDragonPart) {
            entity = ((EnderDragonPart) event.getEntity()).getParent();
        } else {
            entity = event.getEntity();
        }

        final EntityType entityType = entity.getType();

        // If the event is caused by neither TNT, nor a dragon, nor a wither, it is of no relevance.
        if (!EntityType.PRIMED_TNT.equals(entityType) && !EntityType.ENDER_DRAGON.equals(entityType) && !EntityType.WITHER.equals(entityType) && !EntityType.WITHER_SKULL.equals(entityType)) {
            return;
        }

        reinforcementManager.explodeBlocks(event.blockList(), entityType);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final Material material = block.getType();

        // This is to ensure TNT-based physics events are not processed.
        if (!material.equals(Material.SAND) && !material.equals(Material.GRAVEL)) {
            return;
        }

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        if (!allowReinforcedBlockPhysics) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.removeReinforcement(location);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        final Block piston = event.getBlock();
        final BlockFace direction = event.getDirection();
        final ListIterator<Block> iter = event.getBlocks().listIterator(event.getBlocks().size());

        while (iter.hasPrevious()) {
            final Block block = iter.previous();

            // If the next block is reinforced and piston reinforced block movement is disabled, the event is cancelled.
            if (reinforcementManager.isReinforced(block.getRelative(direction).getLocation())) {
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

            reinforcementManager.moveReinforcement(block, direction);
        }

        // Handle the reinforcement on the piston itself.
        if (reinforcementManager.isReinforced(piston.getLocation())) {
            reinforcementManager.moveReinforcement(piston, direction);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }

        final BlockFace direction = event.getDirection();
        final Block block = event.getBlock().getRelative(direction, 2);

        if (!reinforcementManager.isReinforced(block.getLocation())) {
            return;
        }

        if (!pistonsMoveReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.moveReinforcement(block, direction.getOppositeFace());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFade(final BlockFadeEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final Material material = block.getType();

        if (!material.equals(Material.SNOW) && !material.equals(Material.ICE)) {
            return;
        }

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        if (!allowBlockFading) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.removeReinforcement(location);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterPassThrough(final BlockFromToEvent event) {
        final Location location = event.getToBlock().getLocation();

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // If the event is caused by a dragon egg moving to a new location, simply make sure it is not teleporting into a field.
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            reinforcementManager.removeReinforcement(location);
            return;
        }

        if (!liquidsDestroyReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.removeReinforcement(location);
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final Material fromMaterial = block.getType();
        final EntityType entityType = event.getEntityType();
        final Material toMaterial = event.getTo();

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        if (event.getEntity() == null) {
            return;
        }

        if (EntityType.ENDERMAN.equals(entityType)) {
            // If the enderman is placing a block, ignore the event.
            if (fromMaterial.equals(Material.AIR) && !toMaterial.equals(Material.AIR)) {
                return;
            }

            // If the enderman is picking up a block, and is allowed to do so, the reinforcement is removed from the block.
            if (!fromMaterial.equals(Material.AIR) && toMaterial.equals(Material.AIR)) {
                if (mobsInteractWithReinforcedBlocks) {
                    reinforcementManager.removeReinforcement(location);
                } else {
                    event.setCancelled(true);
                }
            }
        }

        // Are sheep able to eat grass, and prevent withers from destroying blocks.
        if (EntityType.SHEEP.equals(entityType) || EntityType.WITHER.equals(entityType) || EntityType.WITHER_SKULL.equals(entityType)) {
            if (mobsInteractWithReinforcedBlocks) {
                reinforcementManager.removeReinforcement(location);
            } else {
                event.setCancelled(true);
            }
        }

        // Prevent sand and gravel from falling, if the plugin is configured to do so. Otherwise, their reinforcements are removed.
        if (EntityType.FALLING_BLOCK.equals(entityType) && (Material.SAND.equals(fromMaterial) || Material.GRAVEL.equals(fromMaterial))) {
            if (allowReinforcedBlockPhysics) {
                reinforcementManager.removeReinforcement(location);
            } else {
                event.setCancelled(true);
            }
        }
    }
}
