package in.nikitapek.blocksaver.management;

import com.amshulman.mbapi.MbapiPlugin;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.BlockSaverFeedback;
import in.nikitapek.blocksaver.util.BlockSaverUtil;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.events.PrismCustomPlayerActionEvent;
import me.botsko.prism.exceptions.InvalidActionException;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FeedbackManager {
    private static final byte PITCH_SHIFT = 50;
    private static final String REINFORCE_EVENT = "bs-block-reinforced";
    private static final String DEINFORCE_EVENT = "bs-block-deinforced";

    private final BlockSaverInfoManager infoManager;
    private final MbapiPlugin plugin;
    private final Prism prism;

    private final Effect reinforcementDamageFailEffect;
    private final Effect reinforcementDamageSuccessEffect;
    private final Sound reinforceSuccessSound;
    private final Sound reinforceFailSound;
    private final Sound hitFailSound;

    private final boolean useParticleEffects;
    private final boolean enableLogBlockLogging;

    public FeedbackManager(final BlockSaverConfigurationContext configurationContext) {
        this.infoManager = configurationContext.infoManager;
        this.plugin = configurationContext.plugin;

        this.reinforcementDamageFailEffect = configurationContext.reinforcementDamageFailEffect;
        this.reinforcementDamageSuccessEffect = configurationContext.reinforcementDamageSuccessEffect;
        this.reinforceSuccessSound = configurationContext.reinforceSuccessSound;
        this.reinforceFailSound = configurationContext.reinforceFailSound;
        this.hitFailSound = configurationContext.hitFailSound;
        this.useParticleEffects = configurationContext.useParticleEffects;
        this.enableLogBlockLogging = configurationContext.enableLogBlockLogging;

        final Plugin tempPrism = configurationContext.plugin.getServer().getPluginManager().getPlugin("Prism");

        if (tempPrism == null) {
            prism = null;
            return;
        }

        prism = (Prism) tempPrism;

        // Register the custom events.
        try {
            Prism.getActionRegistry().registerCustomAction(plugin, new ActionType(REINFORCE_EVENT, "player", "reinforced"));
            Prism.getActionRegistry().registerCustomAction(plugin, new ActionType(DEINFORCE_EVENT, "player", "deinforced"));
        } catch (InvalidActionException e) {
            e.printStackTrace();
        }
    }

    private void logReinforcementEvent(String event, Player player, String msg) {
        PrismCustomPlayerActionEvent prismEvent = new PrismCustomPlayerActionEvent(plugin, event, player, msg);
        plugin.getServer().getPluginManager().callEvent(prismEvent);
    }

    public void sendFeedback(final Location location, final BlockSaverFeedback feedback, final Player player) {
        switch (feedback) {
            case REINFORCE_SUCCESS:
                location.getWorld().playSound(location, reinforceSuccessSound, 1.0f, PITCH_SHIFT);
                if (player == null) {
                    break;
                }
                logReinforcementEvent(REINFORCE_EVENT, player, String.valueOf((int) infoManager.getReinforcement(location).getReinforcementValue()));
                if (infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
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
                if (player == null) {
                    break;
                }
                if (infoManager.getPlayerInfo(player.getName()).isReceivingTextFeedback()) {
                    player.sendMessage(ChatColor.GRAY + "Damaged a reinforced block.");
                }
                if (useParticleEffects) {
                    BlockSaverUtil.sendParticleEffect(location, infoManager.getReinforcement(location).getReinforcementValue());
                } else {
                    location.getWorld().playEffect(location, reinforcementDamageSuccessEffect, 0);
                }
                // Log the breaking of a reinforcement.
                if (infoManager.getReinforcement(location).getReinforcementValue() == 0) {
                    logReinforcementEvent(DEINFORCE_EVENT, player, "");
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
}