package in.nikitapek.blocksaver.commands;

import in.nikitapek.blocksaver.commands.blocksaver.CommandFeedback;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import in.nikitapek.blocksaver.util.Commands;

import com.amshulman.mbapi.commands.DelegatingCommand;
import com.amshulman.mbapi.util.PermissionsEnum;

public final class CommandBlockSaver extends DelegatingCommand {
    public CommandBlockSaver(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, Commands.BLOCKSAVER, 1, 1);
        registerSubcommand(new CommandFeedback(configurationContext));
    }

    public enum BlockSaverCommands implements PermissionsEnum {
        FEEDBACK, RMODE;

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
