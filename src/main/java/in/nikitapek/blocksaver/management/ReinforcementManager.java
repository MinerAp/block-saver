package in.nikitapek.blocksaver.management;

import com.amshulman.typesafety.TypeSafeMap;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverDamageCause;
import in.nikitapek.blocksaver.util.BlockSaverFeedback;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public final class ReinforcementManager {
    private static final byte NO_REINFORCEMENT_VALUE = -1;

    private final FeedbackManager feedbackManager;
    private final BlockSaverInfoManager infoManager;

    private final boolean accumulateReinforcementValues;
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
    private final int gracePeriodTime;
    private final int reinforcementHealingTime;

    private final TypeSafeMap<Material, Integer> reinforceableBlocks;
    public final TypeSafeMap<Material, Integer> reinforcementBlocks;
    private final TypeSafeMap<Material, List<Integer>> toolRequirements;

    public ReinforcementManager(BlockSaverConfigurationContext configurationContext) {
        this.feedbackManager = configurationContext.feedbackManager;
        this.infoManager = configurationContext.infoManager;

        // Retrieves all of the configuration values relevant to Reinforcement managing from configurationContext.
        this.accumulateReinforcementValues = configurationContext.accumulateReinforcementValues;
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
        this.gracePeriodTime = configurationContext.gracePeriodTime;
        this.reinforcementHealingTime = configurationContext.reinforcementHealingTime;

        this.reinforceableBlocks = configurationContext.reinforceableBlocks;
        this.reinforcementBlocks = configurationContext.reinforcementBlocks;
        this.toolRequirements = configurationContext.toolRequirements;
    }

    public boolean isReinforceable(final Block block) {
        final int coefficient = getMaterialReinforcementCoefficient(block.getType());

        // If the block's material cannot be reinforced, the reinforcement fails.
        if (coefficient == -1) {
            return false;
        }

        // Retrieves the reinforcement on the block, if the reinforcement exists.
        final Reinforcement reinforcement = infoManager.getReinforcement(block.getLocation());

        // If the block is not reinforced, it can be reinforced further.
        if (reinforcement == null) {
            return true;
        }

        final float reinforcementValue = reinforcement.getReinforcementValue();

        // If reinforcement values are being accumulated, the RV cannot have reached RVC, and therefore the block is reinforceable.
        if (accumulateReinforcementValues) {
            return true;
        }

        // If reinforcement values are being capped, and the RV is already at RVC, the block cannot be reinforced further.
        return reinforcementValue < coefficient;
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
        if (!player.hasPermission("blocksaver.damage")) {
            if (feedback) {
                feedbackManager.sendFeedback(location, BlockSaverFeedback.PERMISSIONS_FAIL, player);
            }

            return false;
        }

        if (!canToolDamageBlock(location, player.getItemInHand())) {
            if (feedback) {
                feedbackManager.sendFeedback(location, BlockSaverFeedback.HIT_FAIL, player);
            }
            return false;
        }

        return true;
    }

    public boolean attemptReinforcement(final Location location, final Material reinforcementMaterial, final Player player) {
        final Block block = location.getBlock();
        final String playerName = player.getName();

        // If the material cannot be used for reinforcement, the reinforcement fails.
        if (!canMaterialReinforce(reinforcementMaterial)) {
            feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_FAIL, player);
            return false;
        }

        if (!isReinforceable(block)) {
            feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_FAIL, player);
            return false;
        }

        if (!player.hasPermission("blocksaver.reinforce")) {
            feedbackManager.sendFeedback(location, BlockSaverFeedback.PERMISSIONS_FAIL, player);
            return false;
        }

        // Retrieves the amount the material will reinforce the block by.
        int additionalReinforcementValue = reinforcementBlocks.get(reinforcementMaterial);

        // If the material being used to reinforce has a reinforcement maximizing coefficient, then we want to set the block to its maximum possible enforcement.
        if (additionalReinforcementValue == BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
            additionalReinforcementValue = getMaterialReinforcementCoefficient(block.getType());

            // If there is no reinforcement value cap, then we cannot set the block to its maximum reinforcement, therefore the reinforcement fails.
            if (accumulateReinforcementValues) {
                feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_FAIL, player);
                return false;
            }
        }

        reinforce(location, additionalReinforcementValue, playerName);

        feedbackManager.sendFeedback(location, BlockSaverFeedback.REINFORCE_SUCCESS, player);
        return true;
    }

    public void moveReinforcement(final Block block, final BlockFace direction) {
        final Reinforcement previousReinforcement = infoManager.getReinforcement(block.getLocation());
        infoManager.setReinforcement(block.getRelative(direction).getLocation(), previousReinforcement.getReinforcementValue(), previousReinforcement.getCreatorName());
        removeReinforcement(block.getLocation());
    }

    public void floorReinforcement(final Reinforcement reinforcement) {
        // If blocks are allowed to accumulate RV, then there is no need to floor the RV.
        if (accumulateReinforcementValues) {
            return;
        }

        if (reinforcement == null) {
            return;
        }

        // Checks to see if the maximum RV is less than the actual RV. If so, floors the RV.
        final int maximumReinforcement = getMaterialReinforcementCoefficient(reinforcement.getBlock().getType());

        if (reinforcement.getReinforcementValue() > maximumReinforcement) {
            infoManager.setReinforcement(reinforcement.getLocation(), maximumReinforcement, reinforcement.getCreatorName());
        }
    }

    public void damageBlock(final Location location, final Player player, BlockSaverDamageCause damageCause) {
        final Reinforcement reinforcement = infoManager.getReinforcement(location);

        if (reinforcement == null) {
            return;
        }

        // Heals the block if the plugin is configured to do so and the required amount of time has elapsed.
        if (allowReinforcementHealing) {
            if ((System.currentTimeMillis() - reinforcement.getTimeStamp()) >= (reinforcementHealingTime * BlockSaverUtil.MILLISECONDS_PER_SECOND)) {
                reinforcement.setReinforcementValue(reinforcement.getLastMaximumValue());
            }
        }

        if (BlockSaverDamageCause.FIRE.equals(damageCause)) {
            // If fire is not allowed to damage blocks, the block damage fail feedback is provided and the damage fails.
            if (!fireDamagesReinforcedBlocks) {
                feedbackManager.sendFeedback(location, BlockSaverFeedback.DAMAGE_FAIL, null);
                return;
            }

            // Attempt to extinguish the fires on the reinforced block if the configuration allows for it.
            if (extinguishReinforcementFire && Math.random() > extinguishChance) {
                for (final BlockFace face : BlockFace.values()) {
                    final Block relative = location.getBlock().getRelative(face);
                    if (Material.FIRE.equals(relative.getType())) {
                        relative.setType(Material.AIR);
                    }
                }
            }
        }

        // Damage the reinforcement on the block.
        // If the cause of damage is TNT, handle the RV decrease specially.
        if (BlockSaverDamageCause.EXPLOSION.equals(damageCause)) {
            reinforcement.setReinforcementValue(reinforcement.getReinforcementValue() - ((float) Math.pow(getMaterialReinforcementCoefficient(reinforcement.getBlock().getType()), 2) / 100));
        } else {
            reinforcement.setReinforcementValue(reinforcement.getReinforcementValue() - 1);
        }

        feedbackManager.sendFeedback(location, BlockSaverFeedback.DAMAGE_SUCCESS, player);

        // The reinforcement is removed if the reinforcement value has reached zero, or if the reinforcement is not yet fully active for the player (grace period).
        // This uses less than 1 incase TNT sets the RV to a number which would typically ceil to 1 (e.g. 0.97).
        if (reinforcement.getReinforcementValue() < 1 || (player != null && !isFortified(reinforcement, player.getName()))) {
            removeReinforcement(location);
            return;
        }
    }

    public void reinforce(final Location location, final String playerName) {
        infoManager.reinforce(location, getMaterialReinforcementCoefficient(location.getBlock().getType()), playerName);
    }

    public void reinforce(final Location location, final float amount, final String playerName) {
        infoManager.reinforce(location, amount, playerName);
    }

    public boolean isReinforced(final Location location) {
        // If a part of a piston was damaged, retrieves the base of the piston.
        final Location properLocation = getProperLocation(location);
        final Block block = properLocation.getBlock();
        final Chunk chunk = properLocation.getChunk();

        // Confirm that the reinforcement list is already tracking the chunk and location.
        if (!infoManager.getReinforcements().containsKey(chunk) || !infoManager.getReinforcements().get(chunk).contains(properLocation)) {
            return false;
        }

        // If the block is being tracked by the reinforcement list, but does not have all the metadata required to be a proper reinforced block, it is removed from the list.
        if (!block.hasMetadata("RV") || !block.hasMetadata("RTS") || !block.hasMetadata("RCN") || !block.hasMetadata("RLMV") || !block.hasMetadata("RTC")) {
            removeReinforcement(properLocation);
            return false;
        }

        // Removes the reinforcement from the un-reinforceable block.
        if (!isMaterialReinforceable(block.getType())) {
            removeReinforcement(properLocation);
            return false;
        }

        return true;
    }

    public void removeReinforcement(final Location location) {
        final Location properLocation = getProperLocation(location);
        final Chunk chunk = properLocation.getChunk();

        if (infoManager.getReinforcements().containsKey(chunk)) {
            infoManager.getReinforcements().get(chunk).remove(properLocation);
            if (infoManager.getReinforcements().get(chunk).isEmpty()) {
                infoManager.getReinforcements().remove(chunk);
            }
        }

        Reinforcement.removeFromMetadata(properLocation.getBlock());
    }

    public Location getProperLocation(final Location location) {
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

        if (!reinforcement.getCreatorName().equals(playerName)) {
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

        // If the block is no longer reinforced, and blocks are configured to break when their RV reaches 0, the event is allowed to proceed and break the block.
        if (!isReinforced(location) && !leaveBlockAfterDeinforce) {
            return true;
        }

        return false;
    }

    public void explodeBlocks(final List<Block> blockList, final EntityType entityType) {
        // If the exploding entity is not configured to interact with blocks, then the explosion fails.
        if ((EntityType.WITHER.equals(entityType) || EntityType.WITHER_SKULL.equals(entityType)) && !mobsInteractWithReinforcedBlocks) {
            return;
        } else if (EntityType.PRIMED_TNT.equals(entityType) && !tntDamagesReinforcedBlocks) {
            return;
        } else if (EntityType.ENDER_DRAGON.equals(entityType) && !enderdragonInteractWithReinforcedBlocks) {
            return;
        }

        for (final Iterator<Block> iter = blockList.iterator(); iter.hasNext();) {
            final Block block = iter.next();
            final Location location = block.getLocation();

            if (!isReinforced(location)) {
                continue;
            }

            // If TNT damage is enabled for reinforced blocks and TNT reinforcement stripping is also enabled, then the block reinforcement is stripped.
            // Otherwise, the block is simply damaged.
            if (EntityType.PRIMED_TNT.equals(entityType) && !tntStripReinforcementEntirely) {
                damageBlock(location, null, BlockSaverDamageCause.EXPLOSION);
            } else {
                removeReinforcement(location);
            }

            // This probably shouldn't be here...
            // removeReinforcement(location);
            feedbackManager.sendFeedback(location, BlockSaverFeedback.DAMAGE_SUCCESS, null);
            iter.remove();
        }
    }
}
