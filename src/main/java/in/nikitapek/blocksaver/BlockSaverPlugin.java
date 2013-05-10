package in.nikitapek.blocksaver;

import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import com.amshulman.mbapi.MbapiPlugin;

public class BlockSaverPlugin extends MbapiPlugin {
    private BlockSaverInfoManager infoManager;

    @Override
    public void onEnable() {
        BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);
        infoManager = configurationContext.infoManager;

        registerEventHandler(new BlockSaverListener(configurationContext));

        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (infoManager != null) {
            infoManager.unloadAll();
        }
    }
}
