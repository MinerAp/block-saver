package in.nikitapek.blocksaver;

import com.amshulman.mbapi.MbapiPlugin;
import com.comphenix.protocol.ProtocolLibrary;

import in.nikitapek.blocksaver.commands.CommandBlockSaver;
import in.nikitapek.blocksaver.listeners.BlockDeinforceListener;
import in.nikitapek.blocksaver.listeners.BlockReinforceListener;
import in.nikitapek.blocksaver.listeners.GeneralListener;
import in.nikitapek.blocksaver.listeners.PacketListener;
import in.nikitapek.blocksaver.listeners.ReinforcedBlockExplodeListener;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

public final class BlockSaverPlugin extends MbapiPlugin {
    @Override
    public void onEnable() {
        final BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);

        // Register all blocksaver commands.
        registerCommandExecutor(new CommandBlockSaver(configurationContext));

        // Register the listener for all non-blocksaver events.
        registerEventHandler(new GeneralListener(configurationContext));

        // Register the listeners for all blocksaver events.
        registerEventHandler(new BlockReinforceListener(configurationContext));
        registerEventHandler(new BlockDeinforceListener(configurationContext));
        registerEventHandler(new ReinforcedBlockExplodeListener(configurationContext));

        // Add a PacketListener to listen for the Client.BLOCK_DIG packet (for slowing down digging.
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(configurationContext));

        super.onEnable();
    }
}
