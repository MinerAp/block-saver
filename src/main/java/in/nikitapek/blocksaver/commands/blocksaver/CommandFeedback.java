package in.nikitapek.blocksaver.commands.blocksaver;

import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.amshulman.mbapi.commands.PlayerOnlyCommand;
import com.amshulman.typesafety.TypeSafeCollections;
import com.amshulman.typesafety.TypeSafeList;

public class CommandFeedback extends PlayerOnlyCommand {
    private static final String FEEDBACK_ENABLED = ChatColor.GRAY + "You are now recieving reinforcement-related text feedback.";
    private static final String FEEDBACK_DISABLED = ChatColor.GRAY + "You are no longer recieving reinforcement-related text feedback.";

    private final BlockSaverInfoManager infoManager;

    public CommandFeedback(BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, Commands.FEEDBACK, 0, 0);
        infoManager = configurationContext.infoManager;
    }

    @Override
    protected boolean executeForPlayer(Player player, TypeSafeList<String> args) {
        PlayerInfo playerInfo = infoManager.getPlayerInfo(player.getName());

        playerInfo.setRecievingTextFeedback(!playerInfo.isRecievingTextFeedback());

        if (playerInfo.isRecievingTextFeedback())
            player.sendMessage(FEEDBACK_ENABLED);
        else
            player.sendMessage(FEEDBACK_DISABLED);

        return true;
    }

    @Override
    public TypeSafeList<String> onTabComplete(CommandSender sender, TypeSafeList<String> args) {
        return TypeSafeCollections.emptyList();
    }
}
