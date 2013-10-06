package in.nikitapek.blocksaver;

import com.amshulman.mbapi.MbapiPlugin;
import in.nikitapek.blocksaver.commands.CommandBlockSaver;
import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

public final class BlockSaverPlugin extends MbapiPlugin {
    @Override
    public void onEnable() {
        final BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);

        registerCommandExecutor(new CommandBlockSaver(configurationContext));
        registerEventHandler(new BlockSaverListener(configurationContext));

        super.onEnable();
    }
}
