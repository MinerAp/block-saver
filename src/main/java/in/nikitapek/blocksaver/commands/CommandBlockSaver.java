package in.nikitapek.blocksaver.commands;

import in.nikitapek.blocksaver.commands.blocksaver.CommandAutoenforce;
import in.nikitapek.blocksaver.commands.blocksaver.CommandFeedback;
import in.nikitapek.blocksaver.commands.blocksaver.CommandReinforce;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.Commands;

import com.amshulman.mbapi.commands.DelegatingCommand;
import com.amshulman.mbapi.util.PermissionsEnum;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public final class CommandBlockSaver extends DelegatingCommand {
    public CommandBlockSaver(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, Commands.BLOCKSAVER, 1, 1);
        registerSubcommand(new CommandFeedback(configurationContext));
        registerSubcommand(new CommandAutoenforce(configurationContext));

        // Handle the registration of WorldEdit-related commands.
        if (!configurationContext.integrateWorldEdit) {
            return;
        } else if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            Bukkit.getLogger().log(Level.WARNING, "\"integrateWorldEdit\" true but WorldEdit not found. WorldEdit integration will not be enabled.");
            return;
        }
        registerSubcommand(new CommandReinforce(configurationContext));
    }

    public enum BlockSaverCommands implements PermissionsEnum {
        FEEDBACK, AUTOENFORCE, REINFORCE;

        private static final String PREFIX;

        static {
            PREFIX = Commands.BLOCKSAVER.getPrefix() + Commands.BLOCKSAVER.name() + ".";
        }

        @Override
        public String getPrefix() {
            return PREFIX;
        }
    }
}
