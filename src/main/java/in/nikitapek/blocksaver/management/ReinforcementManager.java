package in.nikitapek.blocksaver.management;

import com.amshulman.typesafety.TypeSafeMap;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverDamageCause;
import in.nikitapek.blocksaver.util.BlockSaverFeedback;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class ReinforcementManager {
    private static final byte NO_REINFORCEMENT_VALUE = -1;
    private static final byte PITCH_SHIFT = 50;

    private final BlockSaverInfoManager infoManager;
    private final Consumer logBlockConsumer;

    private final Effect reinforcementDamageFailEffect;
    private final Effect reinforcementDamageSuccessEffect;
    private final Sound reinforceSuccessSound;
    private final Sound reinforceFailSound;
    private final Sound hitFailSound;

    private final boolean accumulateReinforcementValues;
    private final boolean useParticleEffects;
    private final boolean allowReinforcementGracePeriod;
    private final boolean allowReinforcementHealing;
    private final boolean enableLogBlockLogging;
    private final int gracePeriodTime;
    private final int reinforcementHealingTime;

    private final TypeSafeMap<Material, Integer> reinforceableBlocks;
    private final TypeSafeMap<Material, List<Integer>> toolRequirements;

    public ReinforcementManager(BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;

        // Retrieves all of the configuration values relevant to Reinforcement managing from configurationContext.
        this.reinforcementDamageFailEffect = configurationContext.reinforcementDamageFailEffect;
        this.reinforcementDamageSuccessEffect = configurationContext.reinforcementDamageSuccessEffect;
        this.reinforceSuccessSound = configurationContext.reinforceSuccessSound;
        this.reinforceFailSound = configurationContext.reinforceFailSound;
        this.hitFailSound = configurationContext.hitFailSound;
        this.useParticleEffects = configurationContext.useParticleEffects;
        this.accumulateReinforcementValues = configurationContext.accumulateReinforcementValues;
        this.allowReinforcementGracePeriod = configurationContext.allowReinforcementGracePeriod;
        this.allowReinforcementHealing = configurationContext.allowReinforcementHealing;
        this.enableLogBlockLogging = configurationContext.enableLogBlockLogging;
        this.reinforceableBlocks = configurationContext.reinforceableBlocks;
        this.toolRequirements = configurationContext.toolRequirements;
        this.gracePeriodTime = configurationContext.gracePeriodTime;
        this.reinforcementHealingTime = configurationContext.reinforcementHealingTime;

        // Loads LogBlock related things in order to be able to record BlockSaver events.
        final LogBlock logBlockPlugin = (LogBlock) Bukkit.getPluginManager().getPlugin("LogBlock");
        if (enableLogBlockLogging && logBlockPlugin != null) {
            logBlockConsumer = logBlockPlugin.getConsumer();
        } else {
            logBlockConsumer = null;
        }

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

    public boolean isMaterialReinforceable(final Material material) {
        return reinforceableBlocks.containsKey(material);
    }

    public boolean attemptReinforcement(final Block block, final Material reinforcementMaterial, final String playerName) {
        // If the material cannot be used for reinforcement, the reinforcement fails.
        if (!reinforceableBlocks.containsKey(reinforcementMaterial)) {
            return false;
        }

        if (!isReinforceable(block)) {
            return false;
        }

        // Retrieves the reinforcement on the block, if the reinforcement exists.
        final Reinforcement reinforcement = infoManager.getReinforcement(block.getLocation());
        final float currentReinforcementValue;

        // If the block is not reinforced, we must designate the coefficient as such.
        if (reinforcement == null) {
            currentReinforcementValue = -1;
        } else {
            currentReinforcementValue = reinforcement.getReinforcementValue();
        }

        final int coefficient = getMaterialReinforcementCoefficient(block.getType());

        // Retrieves the amount the material will reinforce the block by.
        int additionalReinforcementValue = reinforceableBlocks.get(reinforcementMaterial);

        // If the material being used to reinforce has a reinforcement maximizing coefficient, then we want to set the block to its maximum possible enforcement.
        if (additionalReinforcementValue == BlockSaverUtil.REINFORCEMENT_MAXIMIZING_COEFFICIENT) {
            additionalReinforcementValue = coefficient;

            // If there is no reinforcement value cap, then we cannot set the block to its maximum reinforcement, therefore the reinforcement fails.
            if (accumulateReinforcementValues) {
                return false;
            }
        }

        // If the block is currently reinforced, we add the current reinforcement value to the value to reinforce the block by.
        if (currentReinforcementValue != -1) {
            additionalReinforcementValue += currentReinforcementValue;
        }

        // If we are accumulating reinforcement values, the block's reinforcement is increased by the additionalReinforcementValue which is simply the additional protection of the material being used added to the current reinforcement value of the block.
        // Otherwise, we simply attempt to increase the block's reinforcement by the amount provided by the material.
        if (accumulateReinforcementValues) {
            infoManager.setReinforcement(block.getLocation(), additionalReinforcementValue, playerName);
        } else {
            infoManager.setReinforcement(block.getLocation(), Math.min(additionalReinforcementValue, coefficient), playerName);
        }

        return true;
    }

    public boolean canMaterialReinforce(final Material material) {
        return reinforceableBlocks.containsKey(material);
    }

    public boolean canToolBreakBlock(final Material block, final ItemStack tool) {
        if (!toolRequirements.containsKey(block)) {
            return false;
        }

        for (final Entry<Material, List<Integer>> material : toolRequirements.entrySet()) {
            // If the material is not the same as the block being broken, the loop continues.
            if (!material.getKey().equals(block)) {
                continue;
            }
            // If any tool is allowed, the tool can break the block.
            else if (material.getValue().contains(-2)) {
                return true;
            }
            // If the ItemStack is empty or the type is 0, then the player is using their hand.
            // A check for whether or not hands are allowed to be used is done.
            else if ((tool == null || tool.getTypeId() == 0) && material.getValue().contains(-1)) {
                return true;
            }
            // Finally, a check is performed to see if the tool is valid.
            else if (material.getValue().contains(tool.getTypeId())) {
                return true;
            }
        }

        return false;
    }


    public void moveReinforcement(final Block block, final BlockFace direction) {
        final Reinforcement previousReinforcement = infoManager.getReinforcement(block.getLocation());
        infoManager.setReinforcement(block.getRelative(direction).getLocation(), removeReinforcement(block.getLocation()), previousReinforcement.getCreatorName());
    }

    public boolean removeReinforcementIfInvalid(final Block block) {
        // Removes the reinforcement from the un-reinforceable block.
        if (!isMaterialReinforceable(block.getType())) {
            removeReinforcement(block.getLocation());
            return true;
        }

        return false;
    }

    public void floorReinforcement(final Block block) {
        // If blocks are allowed to accumulate RV, then there is no need to floor the RV.
        if (accumulateReinforcementValues) {
            return;
        }

        // Checks to see if the maximum RV is less than the actual RV. If so, floors the RV.
        final int maximumReinforcement = getMaterialReinforcementCoefficient(block.getType());

        final Reinforcement reinforcement = infoManager.getReinforcement(block.getLocation());

        if (reinforcement == null) {
            return;
        }

        if (reinforcement.getReinforcementValue() > maximumReinforcement) {
            infoManager.setReinforcement(block.getLocation(), maximumReinforcement, reinforcement.getCreatorName());
        }
    }

    public void damageBlock(final Location location, final String playerName, BlockSaverDamageCause damageCause) {
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

        if (reinforcement.getReinforcementValue() <= 1 || !isFortified(reinforcement, playerName)) {
            removeReinforcement(location);
            return;
        }

        if (damageCause == BlockSaverDamageCause.TNT) {
            reinforcement.setReinforcementValue(reinforcement.getReinforcementValue()-((float) Math.pow(getMaterialReinforcementCoefficient(reinforcement.getBlock().getType()), 2)/100));
        } else {
            reinforcement.setReinforcementValue(reinforcement.getReinforcementValue() - 1);
        }
        infoManager.writeReinforcementToMetadata(reinforcement);
    }

    public boolean isFortified(final Reinforcement reinforcement, final String playerName) {
        if (!allowReinforcementGracePeriod) {
            return true;
        }

        if (reinforcement == null || playerName == null) {
            return true;
        }

        if (!reinforcement.isJustCreated()) {
            return true;
        }

        if (!reinforcement.getCreatorName().equals(playerName)) {
            return true;
        }

        if (System.currentTimeMillis() - reinforcement.getTimeStamp() > (gracePeriodTime * BlockSaverUtil.MILLISECONDS_PER_SECOND)) {
            return true;
        }

        return false;
    }

    private void logBlockEvent(String playerName, Location location) {
        logBlockConsumer.queueBlock(playerName, location, location.getBlock().getTypeId(), location.getBlock().getTypeId(), location.getBlock().getData());
    }

    public void sendFeedback(final Location location, final BlockSaverFeedback feedback, final Player player) {
        switch (feedback) {
            case REINFORCE_SUCCESS:
                location.getWorld().playSound(location, reinforceSuccessSound, 1.0f, PITCH_SHIFT);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
                    player.sendMessage(ChatColor.GRAY + "Reinforced a block.");
                }
                break;
            case REINFORCE_FAIL:
                location.getWorld().playSound(location, reinforceFailSound, 1.0f, PITCH_SHIFT);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
                    player.sendMessage(ChatColor.GRAY + "Failed to reinforce a block.");
                }
                break;
            case DAMAGE_SUCCESS:
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
                    player.sendMessage(ChatColor.GRAY + "Damaged a reinforced block.");
                }

                if (player != null && useParticleEffects) {
                    final List<Player> players = new ArrayList<Player>();
                    players.add(player);
                    BlockSaverUtil.sendParticleEffect(location, infoManager.getReinforcement(location).getReinforcementValue());
                } else {
                    location.getWorld().playEffect(location, reinforcementDamageSuccessEffect, 0);
                }
                break;
            case DAMAGE_FAIL:
                location.getWorld().playEffect(location, reinforcementDamageFailEffect, 0);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
                    player.sendMessage(ChatColor.GRAY + "Failed to damage a reinforced block.");
                }
                break;
            case HIT_FAIL:
                location.getWorld().playSound(location, hitFailSound, 1.0f, 0f);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
                    player.sendMessage(ChatColor.GRAY + "Your tool is insufficient to damage this reinforced block.");
                }
                break;
            default:
                break;
        }

        if (!enableLogBlockLogging) {
            return;
        }
    }

    public boolean isReinforced(Location location) {
        // If a part of the piston was damaged, retrieves the base of the piston.
        if (location.getBlock().getType().equals(Material.PISTON_EXTENSION)) {
            final MaterialData data = location.getBlock().getState().getData();
            BlockFace direction = null;

            // Check the block it pushed directly
            if (data instanceof PistonExtensionMaterial) {
                direction = ((PistonExtensionMaterial) data).getFacing();
            }

            if (direction != null) {
                location = location.getBlock().getRelative(direction.getOppositeFace()).getLocation();
            }
        }

        // Confirm that the reinforcement list is already tracking the chunk and location.
        if (!infoManager.getReinforcements().containsKey(location.getChunk()) || !infoManager.getReinforcements().get(location.getChunk()).contains(location)) {
            return false;
        }

        final Block block = location.getBlock();
        // If the block is being tracked by the reinforcement list, but does not have all the metadata required to be a proper reinforced block, it is removed from the list.
        if (!block.hasMetadata("RV") || !block.hasMetadata("RTS") || !block.hasMetadata("RJC") || !block.hasMetadata("RCN") || !block.hasMetadata("RLMV")) {
            Reinforcement.removeFromMetadata(block);
            return false;
        }

        return true;
    }

    public float removeReinforcement(final Location location) {
        final Reinforcement reinforcement = infoManager.getReinforcement(location);

        if (reinforcement == null) {
            return -1;
        }

        if (infoManager.getReinforcements().containsKey(location.getChunk())) {
            infoManager.getReinforcements().get(location.getChunk()).remove(location);
            if (infoManager.getReinforcements().get(location.getChunk()).isEmpty()) {
                infoManager.getReinforcements().remove(location.getChunk());
            }
        }

        final float reinforcementValue = reinforcement.getReinforcementValue();
        Reinforcement.removeFromMetadata(location.getBlock());
        return reinforcementValue;
    }
}
