package in.nikitapek.blocksaver.management;

import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.TypeSafeSet;
import com.amshulman.typesafety.impl.TypeSafeSetImpl;
import in.nikitapek.blocksaver.events.BlockDeinforceEvent;
import in.nikitapek.blocksaver.events.BlockReinforceEvent;
import in.nikitapek.blocksaver.events.ReinforcedBlockDamageEvent;
import in.nikitapek.blocksaver.events.ReinforcedBlockExplodeEvent;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverDamageCause;
import in.nikitapek.blocksaver.util.BlockSaverFeedback;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import in.nikitapek.blocksaver.util.SupplementaryTypes;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public final class ReinforcementManager {
    private static final byte NO_REINFORCEMENT_VALUE = -1;

    private final FeedbackManager feedbackManager;
    private final BlockSaverInfoManager infoManager;

    private final boolean tntDamagesReinforcedBlocks;
    private final boolean tntStripReinforcementEntirely;
    private final boolean fireDamagesReinforcedBlocks;
    private final boolean extinguishReinforcementFire;
    private final boolean allowReinforcementGracePeriod;
    private final boolean allowReinforcementHealing;
    private final boolean leaveBlockAfterDeinforce;
    private final boolean mobsInteractWithReinforcedBlocks;
    private final boolean enderdragonInteractWithReinforcedBlocks;
    private final double extinguishChance;
    private final int reinforcementHealingTime;

    private final TypeSafeMap<Material, Integer> reinforceableBlocks;
    private final TypeSafeMap<Material, Integer> reinforcementBlocks;
    private final TypeSafeMap<Material, List<Integer>> toolRequirements;

    private final TypeSafeSet<FallingBlock> fallingEntities;

    public ReinforcementManager(BlockSaverConfigurationContext configurationContext) {
        this.feedbackManager = configurationContext.feedbackManager;
        this.infoManager = configurationContext.infoManager;
        infoManager.setReinforcementManager(this);

        // Retrieves all of the configuration values relevant to Reinforcement managing from configurationContext.
        this.tntDamagesReinforcedBlocks = configurationContext.tntDamagesReinforcedBlocks;
        this.tntStripReinforcementEntirely = configurationContext.tntStripReinforcementEntirely;
        this.fireDamagesReinforcedBlocks = configurationContext.fireDamagesReinforcedBlocks;
        this.extinguishReinforcementFire = configurationContext.extinguishReinforcementFire;
        this.allowReinforcementGracePeriod = configurationContext.allowReinforcementGracePeriod;
        this.allowReinforcementHealing = configurationContext.allowReinforcementHealing;
        this.leaveBlockAfterDeinforce = configurationContext.leaveBlockAfterDeinforce;
        this.mobsInteractWithReinforcedBlocks = configurationContext.mobsInteractWithReinforcedBlocks;
        this.enderdragonInteractWithReinforcedBlocks = configurationContext.enderdragonInteractWithReinforcedBlocks;
        this.extinguishChance = configurationContext.extinguishChance;
        this.reinforcementHealingTime = configurationContext.reinforcementHealingTime;

        this.reinforceableBlocks = configurationContext.reinforceableBlocks;
        this.reinforcementBlocks = configurationContext.reinforcementBlocks;
        this.toolRequirements = configurationContext.toolRequirements;

        fallingEntities = new TypeSafeSetImpl<>(new HashSet<FallingBlock>(), SupplementaryTypes.FALLING_BLOCK);
    }

    public boolean isReinforceable(final Block block) {
        final int coefficient = getMaterialReinforcementCoefficient(block.getType());

        // If the block's material cannot be reinforced, the reinforcement fails.
        if (coefficient == NO_REINFORCEMENT_VALUE) {
            return false;
        }

        // Retrieves the reinforcement on the block, if the reinforcement exists.
        final Reinforcement reinforcement = infoManager.getReinforcement(block.getLocation());

        // If the block is not reinforced, it can be reinforced further.
        if (reinforcement == null) {
            return true;
        }

        final float reinforcementValue = reinforcement.getReinforcementValue(coefficient);

        // If the RV of the block has not beed set to RVC after being deserialized, it is not reinforceable.
        if (reinforcementValue == BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
            return false;
        }

        // If reinforcement values are being capped, and the RV is already at RVC, the block cannot be reinforced further.
        return reinforcementValue < coefficient;
    }

    public Reinforcement getReinforcement(final Location location) {
        return infoManager.getReinforcement(location);
    }

    public int getMaterialReinforcementCoefficient(final Material material) {
        return isMaterialReinforceable(material) ? reinforceableBlocks.get(material) : NO_REINFORCEMENT_VALUE;
    }

    private boolean isMaterialReinforceable(final Material material) {
        return reinforceableBlocks.containsKey(material);
    }

    public boolean canMaterialReinforce(final Material material) {
        return reinforcementBlocks.containsKey(material);
    }

    public boolean isPlayerInReinforcementMode(final Player player) {
        return infoManager.getPlayerInfo(player).isInReinforcementMode;
    }

    private Material getReinforcingMaterial(final Player player) {
        // If the player is not in reinforcement mode, then we only check the item in their hand.
        if (!isPlayerInReinforcementMode(player)) {
            Material itemInHandMaterial = player.getItemInHand().getType();
            if (canMaterialReinforce(itemInHandMaterial)) {
                return itemInHandMaterial;
            } else {
                return Material.AIR;
            }
        }

        // Otherwise, loop through all the reinforcement items and check if the player has one of these.
        for (Material material : reinforcementBlocks.keySet()) {
            int index = player.getInventory().first(material);

            if (index != -1) {
                // If the player has a reinforcing material, then a check is made to see if this is the item being placed.
                // If there is, and there is only 1 left, then it is not used as the reinforcing item is being used up by being placed on the ground.
                if (index == 0) {
                    if (player.getItemInHand().getAmount() == 1) {
                        continue;
                    }
                }

                return material;
            }
        }

        return Material.AIR;
    }

    private boolean canToolDamageBlock(final Location location, final ItemStack tool) {
        final Material blockMaterial = location.getBlock().getType();

        // If the location does not contain a reinforcement, there is nothing stopping the tool from damaging the block.
        if (!isReinforced(location)) {
            return true;
        }

        // If the tool can be used for reinforcement, then it cannot be used to break a reinforced block.
        if (canMaterialReinforce(tool.getType())) {
            return false;
        }

        // If the block does not contain a valid tool for use, the block is not breakable by the tool provided.
        if (!toolRequirements.containsKey(blockMaterial)) {
            return false;
        }

        for (final Entry<Material, List<Integer>> blockTools : toolRequirements.entrySet()) {
            // If the material is not the same as the block being broken, the loop continues.
            if (!blockTools.getKey().equals(blockMaterial)) {
                continue;
            }

            // Gets the list of tools which can be used on blockMaterial.
            final List<Integer> toolList = blockTools.getValue();

            // If any tool is allowed for this material, the tool can break the block.
            if (toolList.contains(BlockSaverUtil.ALL_TOOL_CODE)) {
                return true;
            }
            // If the ItemStack is empty or the type is 0, then the player is using their hand.
            // A check for whether or not hands are allowed to be used is done.
            else if (tool.getTypeId() == 0) {
                return toolList.contains(BlockSaverUtil.HANDS_TOOL_CODE);
            }
            // Finally, a check is performed to see if the tool is valid.
            else if (toolList.contains(tool.getTypeId())) {
                return true;
            }

            return false;
        }

        return false;
    }

    public boolean canPlayerDamageBlock(final Location location, final Player player, final boolean feedback) {
        Location properLocation = getProperLocation(location);

        if (player.hasPermission("blocksaver.exempt")) {
            return true;
        }

        if (!player.hasPermission("blocksaver.damage") && isReinforced(properLocation)) {
            if (feedback) {
                feedbackManager.sendFeedback(properLocation, BlockSaverFeedback.PERMISSIONS_FAIL, player);
            }

            return false;
        }

        if (!canToolDamageBlock(properLocation, player.getItemInHand())) {
            if (feedback) {
                feedbackManager.sendFeedback(properLocation, BlockSaverFeedback.HIT_FAIL, player);
            }
            return false;
        }

        return true;
    }

    public void attemptReinforcement(final Location location, final Player player) {
        final Block block = location.getBlock();
        final Material material = getReinforcingMaterial(player);
        if (Material.AIR.equals(material)) {
            return;
        }
        final ItemStack item = player.getInventory().getItem(player.getInventory().first(material));

        // If the material cannot be used for reinforcement, the reinforcement fails.
        if (!canMaterialReinforce(material)) {
            feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_FAIL, player);
            return;
        }

        if (!isReinforceable(block)) {
            feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_FAIL, player);
            return;
        }

        if (!player.hasPermission("blocksaver.reinforce")) {
            feedbackManager.sendFeedback(location, BlockSaverFeedback.PERMISSIONS_FAIL, player);
            return;
        }

        Bukkit.getServer().getPluginManager().callEvent(new BlockReinforceEvent(block, player.getName(), true));

        feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_SUCCESS, player);

        // The amount of the reinforcement material in the player's hand is decreased.
        if (!GameMode.CREATIVE.equals(player.getGameMode())) {
            PlayerInfo playerInfo = infoManager.getPlayerInfo(player);

            // If the player has already reinforced with this material, retrieves the number of remaining uses for the player; otherwise, use the default value for the material.
            int usesLeft = playerInfo.hasUsed(material) ? playerInfo.getRemainingUses(material) : reinforcementBlocks.get(material);

            if (usesLeft == 1) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                player.updateInventory();

                // Reset the number of uses.
                playerInfo.setRemainingUses(material, reinforcementBlocks.get(material));
            } else {
                playerInfo.setRemainingUses(material, usesLeft - 1);
            }
        }
    }

    /**
     * Moves a reinforcement from its location to the location one block over in the given direction, without modifying the reinforcement object.
     *
     * @param location  the initial location of the reinforcement.
     * @param direction the target direction of the movement operation.
     */
    public void moveReinforcement(Location location, BlockFace direction) {
        infoManager.moveReinforcement(location, location.getBlock().getRelative(direction).getLocation());
    }

    public void damageBlock(final Location location, final Player player, final BlockSaverDamageCause damageCause) {
        Location properLocation = getProperLocation(location);
        Reinforcement reinforcement = infoManager.getReinforcement(properLocation);
        Block block = properLocation.getBlock();
        Material material = block.getType();
        String playerName = player == null ? null : player.getName();

        if (reinforcement == null) {
            return;
        }

        // Heals the block if the plugin is configured to do so and the required amount of time has elapsed.
        float coefficient = getMaterialReinforcementCoefficient(location.getBlock().getType());
        if (allowReinforcementHealing) {
            if ((System.currentTimeMillis() - reinforcement.getTimeStamp()) >= (reinforcementHealingTime * BlockSaverUtil.MILLISECONDS_PER_SECOND)) {
                // This calls the infoManager.reinforce() method because there is no need to log reinforcement healing.
                infoManager.reinforce(properLocation, reinforcement.getCreatorName(), coefficient);
            }
        }

        if (BlockSaverDamageCause.FIRE.equals(damageCause)) {
            // If fire is not allowed to damage blocks, the block damage fail feedback is provided and the damage fails.
            if (!fireDamagesReinforcedBlocks) {
                feedbackManager.sendFeedback(properLocation, BlockSaverFeedback.DAMAGE_FAIL, null);
                return;
            }

            // Attempt to extinguish the fires on the reinforced block if the configuration allows for it.
            if (extinguishReinforcementFire && Math.random() > extinguishChance) {
                for (final BlockFace face : BlockFace.values()) {
                    final Block relative = block.getRelative(face);
                    if (Material.FIRE.equals(relative.getType())) {
                        relative.setType(Material.AIR);
                    }
                }
            }
        }

        feedbackManager.sendFeedback(properLocation, BlockSaverFeedback.DAMAGE_SUCCESS, player);

        // Damage the reinforcement on the block.
        // If the cause of damage is TNT, handle the RV decrease specially.
        if (BlockSaverDamageCause.EXPLOSION.equals(damageCause)) {
            Bukkit.getServer().getPluginManager().callEvent(new ReinforcedBlockExplodeEvent(block, playerName, true));
        } else {
            Bukkit.getServer().getPluginManager().callEvent(new ReinforcedBlockDamageEvent(block, playerName, true));
        }

        // The reinforcement is removed if the reinforcement value has reached zero, or if the reinforcement is not yet fully active for the player (grace period).
        // This uses less than 1 in case TNT sets the RV to a number which would typically ceil to 1 (e.g. 0.97).
        if (reinforcement.getReinforcementValue(coefficient) < 1 || (player != null && !isFortified(reinforcement, playerName))) {
            Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, playerName, true));
        }
    }

    public boolean isReinforced(final Location location) {
        // If a part of a piston was damaged, retrieves the base of the piston.
        final Location properLocation = getProperLocation(location);
        final Block block = properLocation.getBlock();

        if (!infoManager.isReinforced(properLocation)) {
            return false;
        }

        // Removes the reinforcement from the un-reinforceable block.
        if (!isMaterialReinforceable(block.getType())) {
            Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
            return false;
        }

        return true;
    }

    public static Location getProperLocation(final Location location) {
        final Block block = location.getBlock();

        if (block.getType().equals(Material.PISTON_EXTENSION)) {
            final MaterialData data = block.getState().getData();
            BlockFace direction = null;

            // Check the block it pushed directly
            if (data instanceof PistonExtensionMaterial) {
                direction = ((PistonExtensionMaterial) data).getFacing();
            }

            if (direction != null) {
                return block.getRelative(direction.getOppositeFace()).getLocation();
            }
        }

        return location;
    }

    private boolean isFortified(final Reinforcement reinforcement, final String playerName) {
        if (!allowReinforcementGracePeriod) {
            return true;
        }

        if (reinforcement == null || playerName == null) {
            return true;
        }

        if (!playerName.equals(reinforcement.getCreatorName())) {
            return true;
        }

        if (!reinforcement.isJustCreated()) {
            return true;
        }

        return false;
    }

    public boolean attemptToBreakBlock(final Location location, final Player player) {
        if (player == null) {
            return false;
        }

        if (!canPlayerDamageBlock(location, player, false)) {
            return false;
        }

        damageBlock(location, player, BlockSaverDamageCause.TOOL);

        // If the block is reinforced, or blocks are not configured to break when their RV reaches 0, the event is not allowed to proceed.
        if (isReinforced(location) || leaveBlockAfterDeinforce) {
            return false;
        }

        return true;
    }

    public void explodeBlocks(final List<Block> blockList, final EntityType entityType) {

        for (final Iterator<Block> iter = blockList.iterator(); iter.hasNext(); ) {
            final Block block = iter.next();
            final Location location = block.getLocation();

            if (!isReinforced(location)) {
                continue;
            }

            // If the exploding entity is not configured to interact with blocks, then the explosion fails.
            if ((EntityType.CREEPER.equals(entityType) || EntityType.WITHER.equals(entityType) || EntityType.WITHER_SKULL.equals(entityType)) && !mobsInteractWithReinforcedBlocks) {
                iter.remove();
                continue;
            } else if (EntityType.PRIMED_TNT.equals(entityType) && !tntDamagesReinforcedBlocks) {
                iter.remove();
                continue;
            } else if (EntityType.ENDER_DRAGON.equals(entityType) && !enderdragonInteractWithReinforcedBlocks) {
                iter.remove();
                continue;
            }

            // If TNT damage is enabled for reinforced blocks and TNT reinforcement stripping is also enabled, then the block reinforcement is stripped.
            // Otherwise, the block is simply damaged.
            if (EntityType.PRIMED_TNT.equals(entityType) && !tntStripReinforcementEntirely) {
                damageBlock(location, null, BlockSaverDamageCause.EXPLOSION);
            } else {
                Bukkit.getServer().getPluginManager().callEvent(new BlockDeinforceEvent(block, "Environment", true));
            }

            // This probably shouldn't be here...
            // removeReinforcement(location);
            feedbackManager.sendFeedback(location, BlockSaverFeedback.DAMAGE_SUCCESS, null);
            iter.remove();
        }
    }

    public boolean isWorldActive(String worldName) {
        return infoManager.isWorldLoaded(worldName);
    }

    public void storeFallingEntity(FallingBlock entity) {
        fallingEntities.add(entity);
    }

    public boolean restoreFallingEntity(FallingBlock entity) {
        if (!fallingEntities.remove(entity)) {
            return false;
        }

        Block block = entity.getLocation().getBlock();

        Bukkit.getServer().getPluginManager().callEvent(new BlockReinforceEvent(block, "Environment", false));
        return true;
    }
}
