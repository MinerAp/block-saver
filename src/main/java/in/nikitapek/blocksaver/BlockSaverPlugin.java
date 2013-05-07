package in.nikitapek.blocksaver;

import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import com.amshulman.mbapi.MbapiPlugin;

public class BlockSaverPlugin extends MbapiPlugin {
    @Override
    public void onEnable() {
        BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);

        registerEventHandler(new BlockSaverListener(configurationContext));

        super.onEnable();
    }
}
