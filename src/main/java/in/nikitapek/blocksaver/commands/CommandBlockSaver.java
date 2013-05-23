package in.nikitapek.blocksaver.commands;

import in.nikitapek.blocksaver.commands.blocksaver.CommandFeedback;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.Commands;

import com.amshulman.mbapi.commands.DelegatingCommand;

public class CommandBlockSaver extends DelegatingCommand {
    public CommandBlockSaver(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, Commands.BLOCKSAVER, 1, 1);
        registerSubcommand(new CommandFeedback(configurationContext));
    }
}
