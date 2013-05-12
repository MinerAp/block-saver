package in.nikitapek.blocksaver;

import org.bukkit.Location;

import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import com.amshulman.mbapi.MbapiPlugin;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

public class BlockSaverPlugin extends MbapiPlugin {
    private BlockSaverInfoManager infoManager;
    private ProtocolManager protocolManager;

    @Override
    public void onLoad() {
        super.onLoad();

        protocolManager = ProtocolLibrary.getProtocolManager();
    }

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

    /*
    public void sendParticleEffect(Location location) {
        PacketContainer particle = protocolManager.createPacket(60);

        particle.getDoubles().
            write(0, location.getX()).
            write(1, location.getY()).
            write(2, location.getZ());
        particle.getFloat().
            write(0, 3.0F);

        protocolManager.sendServerPacket(player, particle);
    }*/
}
