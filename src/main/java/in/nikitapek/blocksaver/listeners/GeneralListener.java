package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.events.BlockDeinforceEvent;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverDamageCause;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ListIterator;

public final class GeneralListener implements Listener {
    private final BlockSaverInfoManager infoManager;
    private final ReinforcementManager reinforcementManager;

    private final boolean liquidsDestroyReinforcedBlocks;
    private final boolean allowReinforcedBlockPhysics;
    private final boolean pistonsMoveReinforcedBlocks;
    private final boolean allowBlockFading;
    private final boolean mobsInteractWithReinforcedBlocks;

    public GeneralListener(final BlockSaverConfigurationContext configurationContext) {
    	this.infoManager = configurationContext.infoManager;
        this.reinforcementManager = configurationContext.getReinforcementManager();

        this.liquidsDestroyReinforcedBlocks = configurationContext.liquidsDestroyReinforcedBlocks;
        this.allowReinforcedBlockPhysics = configurationContext.allowReinforcedBlockPhysics;
        this.pistonsMoveReinforcedBlocks = configurationContext.pistonsMoveReinforcedBlocks;
        this.allowBlockFading = configurationContext.allowBlockFading;
        this.mobsInteractWithReinforcedBlocks = configurationContext.mobsInteractWithReinforcedBlocks;
    }

    /**
     * Listen for block place events by the player.
     * If the player is placing blocks while in reinforcement mode, automatically
     * reinforce placed blocks using reinforcement materials from the player's
     * inventory.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Location location = event.getBlock().getLocation();
        final String worldName = location.getWorld().getName();
        final Player player = event.getPlayer();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        // If the player has placed a block and is currently in auto-reinforce mode, an attempt is made to reinforce the newly placed block.
        if (infoManager.getPlayerInfo(player).isInReinforcementMode) {
            reinforcementManager.attemptReinforcement(location, player);
        }
    }

    /**
     * Listen for block break events.
     * If the block break event occurs for a block in a reinforcement-active world,
     * and the block itself is reinforced, then the event is cancelled and the
     * block is not broken.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final String worldName = location.getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        // If the block is not reinforced, this plugin does not stop the block break event.
        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // If the block is not successfully broken (e.g. the RV is not 0), then the event is cancelled.
        event.setCancelled(true);
    }

    /**
     * Listens for block interact events.
     * If a player is left clicking a block, and the material in the player's hand is
     * reinforcing material, then the event is cancelled, and an attempt is made
     * to reinforce the block being clicked.
     * If a player is left clicking a block, but the player is not holding
     * reinforcing material in their hand, then the player is attempting to break
     * the block, and the event is cancelled if the tool in the user's hand is
     * incapable of destroying the reinforced block.
     * If a PHYSICAL action is being performed (e.g. a player is stepping on
     * reinforced soil), then the event is cancelled.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        // Confirms that the block is not air.
        if (block == null) {
            return;
        }

        final Location location = block.getLocation();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(location.getWorld().getName())) {
            return;
        }

        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
                // If the player cannot reinforce with the material in their hand, then they are attempting to damage the block.
                if (!reinforcementManager.canMaterialReinforce(player.getItemInHand().getType())) {
                    if (!reinforcementManager.canPlayerDamageBlock(location, player, true)) {
                        event.setCancelled(true);
                    }
                    return;
                }

                // The event is cancelled because if the reinforcement fails, we do not want left click actions registering with reinforcement blocks anyways.
                event.setCancelled(true);

                // An attempt is made to reinforce the block the player clicks, which, if not successful, exits the event.
                reinforcementManager.attemptReinforcement(location, player);
                break;
            case PHYSICAL:
                // A player or other entity is performing a PHYSICAL (e.g. stepping/jumping) action on the block.
                if (Material.SOIL.equals(block.getType()) && reinforcementManager.isReinforced(location)) {
                    event.setCancelled(true);
                    break;
                }
        	default:
        		break;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(final BlockBurnEvent event) {
        final Location location = event.getBlock().getLocation();
        final String worldName = location.getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        // If the block is not reinforced, it is allowed to burn normally.
        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // If the block is reinforced, the burn event is cancelled for the block.
        event.setCancelled(true);

        // The block reinforcement is then damaged.
        reinforcementManager.damageBlock(location, null, BlockSaverDamageCause.FIRE);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent event) {
        final Location location = event.getBlock().getLocation();
        final String worldName = location.getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        // If the block is not reinforced, it is allowed to burn normally.
        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        Player player = null;

        if (BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL.equals(event.getCause())) {
            player = event.getPlayer();
        }

        // If the block is reinforced, the burn event is cancelled for the block.
        event.setCancelled(true);

        // The block reinforcement is then damaged.
        reinforcementManager.damageBlock(location, player, BlockSaverDamageCause.FIRE);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(final EntityExplodeEvent event) {
        final String worldName = event.getLocation().getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

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

        reinforcementManager.explodeBlocks(event.blockList(), entity.getType());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
    	final String worldName = event.getBlock().getLocation().getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

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

            reinforcementManager.moveReinforcement(block.getLocation(), direction);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
    	final String worldName = event.getBlock().getLocation().getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        if (!event.isSticky()) {
            return;
        }

        final BlockFace direction = event.getDirection();
        final Block block = event.getBlock().getRelative(direction, 2);
        Location location = block.getLocation();

        if (!reinforcementManager.isReinforced(block.getLocation())) {
            return;
        }

        if (!pistonsMoveReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }

        reinforcementManager.moveReinforcement(location, direction.getOppositeFace());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockFade(final BlockFadeEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final String worldName = location.getWorld().getName();
        final Material material = block.getType();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        if (!Material.SNOW.equals(material) && !Material.ICE.equals(material) && !Material.SOIL.equals(material)) {
            return;
        }

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        if (!allowBlockFading) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWaterPassThrough(final BlockFromToEvent event) {
        final Block block = event.getToBlock();
        final Location location = block.getLocation();
        final String worldName = location.getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        if (!reinforcementManager.isReinforced(location)) {
            return;
        }

        // If the event is caused by a dragon egg moving to a new location, simply make sure it is not teleporting into a field.
        if (Material.DRAGON_EGG.equals(event.getBlock().getType())) {
            Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
            return;
        }

        if (!liquidsDestroyReinforcedBlocks) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final String worldName = location.getWorld().getName();
        final Material fromMaterial = block.getType();
        final Entity entity = event.getEntity();
        final EntityType entityType = event.getEntityType();
        final Material toMaterial = event.getTo();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        if (!reinforcementManager.isReinforced(location)) {
            if (Material.AIR.equals(fromMaterial) && entity instanceof FallingBlock) {
                reinforcementManager.restoreFallingEntity((FallingBlock) entity);
            }
            return;
        }

        if (entity == null) {
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
                    Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
                } else {
                    event.setCancelled(true);
                }
            }
        }

        // Are sheep able to eat grass, and prevent withers from destroying blocks.
        if (EntityType.SHEEP.equals(entityType) || EntityType.WITHER.equals(entityType) || EntityType.WITHER_SKULL.equals(entityType)) {
            if (mobsInteractWithReinforcedBlocks) {
                Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
            } else {
                event.setCancelled(true);
            }
        }

        // Prevent sand and gravel from falling, if the plugin is configured to do so. Otherwise, their reinforcements are removed.
        if (entity instanceof FallingBlock && (Material.SAND.equals(fromMaterial) || Material.GRAVEL.equals(fromMaterial))) {
            if (allowReinforcedBlockPhysics) {
                reinforcementManager.storeFallingEntity((FallingBlock) entity);
                Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteract(final EntityInteractEvent event) {
        final Block block = event.getBlock();

        // Confirms that the block is not null.
        if (block == null) {
            return;
        }

        final Location location = block.getLocation();
        final String worldName = location.getWorld().getName();

        // If the world isn't handled by BlockSaver, ignore it.
        if (!infoManager.isWorldLoaded(worldName)) {
            return;
        }

        // If the affected block is soil, and it is reinforced, then the soil is not allowed to decay.
        if (Material.SOIL.equals(block.getType()) && reinforcementManager.isReinforced(location)) {
            event.setCancelled(true);
        }
    }
}
