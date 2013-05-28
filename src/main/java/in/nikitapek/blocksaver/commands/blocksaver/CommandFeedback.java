package in.nikitapek.blocksaver.commands.blocksaver;

import in.nikitapek.blocksaver.commands.CommandBlockSaver.BlockSaverCommands;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.amshulman.mbapi.commands.PlayerOnlyCommand;
import com.amshulman.typesafety.TypeSafeCollections;
import com.amshulman.typesafety.TypeSafeList;

public final class CommandFeedback extends PlayerOnlyCommand {
    private static final String FEEDBACK_ENABLED = ChatColor.GRAY + "You are now recieving reinforcement-related text feedback.";
    private static final String FEEDBACK_DISABLED = ChatColor.GRAY + "You are no longer recieving reinforcement-related text feedback.";

    private final BlockSaverInfoManager infoManager;

    public CommandFeedback(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, BlockSaverCommands.FEEDBACK, 0, 0);
        infoManager = configurationContext.infoManager;
    }

    @Override
    protected boolean executeForPlayer(final Player player, final TypeSafeList<String> args) {
        final PlayerInfo playerInfo = infoManager.getPlayerInfo(player.getName());

        playerInfo.setRecievingTextFeedback(!playerInfo.isRecievingTextFeedback());

        if (playerInfo.isRecievingTextFeedback()) {
            player.sendMessage(FEEDBACK_ENABLED);
        } else {
            player.sendMessage(FEEDBACK_DISABLED);
        }

        return true;
    }

    @Override
    public TypeSafeList<String> onTabComplete(final CommandSender sender, final TypeSafeList<String> args) {
        return TypeSafeCollections.emptyList();
    }
}