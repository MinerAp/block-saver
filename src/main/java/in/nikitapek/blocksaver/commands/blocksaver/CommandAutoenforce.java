package in.nikitapek.blocksaver.commands.blocksaver;

import com.amshulman.mbapi.commands.PlayerOnlyCommand;
import com.amshulman.typesafety.TypeSafeCollections;
import com.amshulman.typesafety.TypeSafeList;
import in.nikitapek.blocksaver.commands.CommandBlockSaver.BlockSaverCommands;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandAutoenforce extends PlayerOnlyCommand {
    private static final String AUTOENFORCE_ENABLED = ChatColor.GRAY + "You are now in auto-reinforcement mode.";
    private static final String AUTOENFORCE_DISABLED = ChatColor.GRAY + "You are no longer in auto-reinforcement mode.";

    private final BlockSaverInfoManager infoManager;

    public CommandAutoenforce(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, BlockSaverCommands.AUTOENFORCE, 0, 0);
        infoManager = configurationContext.infoManager;
    }

    @Override
    protected boolean executeForPlayer(final Player player, final TypeSafeList<String> args) {
        final PlayerInfo playerInfo = infoManager.getPlayerInfo(player);

        playerInfo.isInReinforcementMode = !playerInfo.isInReinforcementMode;

        if (playerInfo.isReceivingTextFeedback) {
            if (playerInfo.isInReinforcementMode) {
                player.sendMessage(AUTOENFORCE_ENABLED);
            } else {
                player.sendMessage(AUTOENFORCE_DISABLED);
            }
        }

        return true;
    }

    @Override
    public TypeSafeList<String> onTabComplete(final CommandSender sender, final TypeSafeList<String> args) {
        return TypeSafeCollections.emptyList();
    }
}
