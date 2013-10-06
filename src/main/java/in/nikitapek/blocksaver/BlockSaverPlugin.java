package in.nikitapek.blocksaver;

import in.nikitapek.blocksaver.commands.CommandBlockSaver;
import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import com.amshulman.mbapi.MbapiPlugin;

public final class BlockSaverPlugin extends MbapiPlugin {
    @Override
    public void onEnable() {
        final BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);

        registerCommandExecutor(new CommandBlockSaver(configurationContext));
        registerEventHandler(new BlockSaverListener(configurationContext));

        super.onEnable();
    }
}
