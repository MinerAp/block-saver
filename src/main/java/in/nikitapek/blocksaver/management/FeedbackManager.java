package in.nikitapek.blocksaver.management;

import in.nikitapek.blocksaver.serialization.Reinforcement;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverFeedback;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FeedbackManager {
    private static final byte PITCH_SHIFT = 50;

    private final BlockSaverInfoManager infoManager;

    private final Effect reinforcementDamageFailEffect;
    private final Sound reinforceSuccessSound;
    private final Sound reinforceFailSound;
    private final Sound hitFailSound;

    private final String primaryFeedback;

    public FeedbackManager(final BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;

        this.reinforcementDamageFailEffect = configurationContext.reinforcementDamageFailEffect;
        this.reinforceSuccessSound = configurationContext.reinforceSuccessSound;
        this.reinforceFailSound = configurationContext.reinforceFailSound;
        this.hitFailSound = configurationContext.hitFailSound;
        this.primaryFeedback = configurationContext.primaryFeedback;

        if (!configurationContext.prismLogging && !configurationContext.insightLogging) {
            return;
        }
    }

    public void sendFeedback(final Location location, final BlockSaverFeedback feedback, final Player player) {
        final Reinforcement reinforcement = infoManager.getReinforcement(location);

        switch (feedback) {
            case REINFORCE_SUCCESS:
                location.getWorld().playSound(location, reinforceSuccessSound, 1.0f, PITCH_SHIFT);
                if (player == null) {
                    break;
                }
                if (infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback && player.hasPermission("blocksaver.feedback.reinforce.success")) {
                    player.sendMessage(ChatColor.GRAY + "Reinforced a block.");
                }
                break;
            case REINFORCE_FAIL:
                location.getWorld().playSound(location, reinforceFailSound, 1.0f, PITCH_SHIFT);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback && player.hasPermission("blocksaver.feedback.reinforce.fail")) {
                    player.sendMessage(ChatColor.GRAY + "Failed to reinforce a block.");
                }
                break;
            case DAMAGE_SUCCESS:
                if (player == null) {
                    break;
                }
                if (infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback && player.hasPermission("blocksaver.feedback.damage.success")) {
                    player.sendMessage(ChatColor.GRAY + "Damaged a reinforced block.");
                }
                if ("visual".equals(primaryFeedback)) {
                    BlockSaverUtil.sendParticleEffect(location, (int) reinforcement.getReinforcementValue(), infoManager.getReinforcementManager().getMaterialReinforcementCoefficient(location.getBlock().getType()));
                } else if ("auditory".equals(primaryFeedback)) {
                    BlockSaverUtil.playMusicalEffect(location, (int) reinforcement.getReinforcementValue());
                }
                break;
            case DAMAGE_FAIL:
                location.getWorld().playEffect(location, reinforcementDamageFailEffect, 0);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback && player.hasPermission("blocksaver.feedback.damage.fail")) {
                    player.sendMessage(ChatColor.GRAY + "Failed to damage a reinforced block.");
                }
                break;
            case HIT_FAIL:
                location.getWorld().playSound(location, hitFailSound, 1.0f, 0f);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback && player.hasPermission("blocksaver.feedback.hit")) {
                    player.sendMessage(ChatColor.GRAY + "Your tool is insufficient to damage this reinforced block.");
                }
                break;
            case PERMISSIONS_FAIL:
                location.getWorld().playSound(location, hitFailSound, 1.0f, 0f);
                if (player != null && infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback && player.hasPermission("blocksaver.feedback.permissions")) {
                    player.sendMessage(ChatColor.GRAY + "You do not have the necessary permissions for this action.");
                }
                break;
            default:
                break;
        }
    }
}
