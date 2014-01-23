package in.nikitapek.blocksaver;

import com.amshulman.mbapi.MbapiPlugin;
import in.nikitapek.blocksaver.commands.CommandBlockSaver;
import in.nikitapek.blocksaver.listeners.BlockDeinforceListener;
import in.nikitapek.blocksaver.listeners.BlockReinforceListener;
import in.nikitapek.blocksaver.listeners.GeneralListener;
import in.nikitapek.blocksaver.listeners.ReinforcedBlockDamageListener;
import in.nikitapek.blocksaver.listeners.ReinforcedBlockExplodeListener;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

public final class BlockSaverPlugin extends MbapiPlugin {
    @Override
    public void onEnable() {
        final BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);

        registerCommandExecutor(new CommandBlockSaver(configurationContext));
        registerEventHandler(new GeneralListener(configurationContext));
        registerEventHandler(new BlockReinforceListener(configurationContext));
        registerEventHandler(new BlockDeinforceListener(configurationContext));
        registerEventHandler(new ReinforcedBlockDamageListener(configurationContext));
        registerEventHandler(new ReinforcedBlockExplodeListener(configurationContext));

        super.onEnable();
    }
}
