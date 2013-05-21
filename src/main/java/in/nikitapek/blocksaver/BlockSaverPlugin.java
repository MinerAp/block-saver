package in.nikitapek.blocksaver;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import in.nikitapek.blocksaver.commands.CommandBlockSaver;
import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import com.amshulman.mbapi.MbapiPlugin;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;

public class BlockSaverPlugin extends MbapiPlugin {
    private BlockSaverInfoManager infoManager;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);
        infoManager = configurationContext.infoManager;

        registerCommandExecutor(new CommandBlockSaver(configurationContext));
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

    public void sendParticleEffect(List<Player> players, Location location) {
        PacketContainer particle = ProtocolLibrary.getProtocolManager().createPacket(61);
        int data = 22;

        switch(infoManager.getReinforcementValue(location)) {
            case -1:
                // If the block is not reinforced, but has just been damaged as a reinforced block (presumably due to the grace period), then we play the "nearly broken" effect.
                data = 9;
                break;
            case 1:
                data = 9;
                break;
            case 2:
                data = 5;
                break;
            case 3:
                data = 3;
                break;
            case 4:
                data = 4;
                break;
            case 5:
                data = 7;
                break;
            case 6:
                data = 0;
                break;
            default:
                break;
        }

        particle.getIntegers().
            write(0, 2002).
            write(1, data).
            write(2, (int) location.getX()).
            write(3, (int) location.getY()).
            write(4, (int) location.getZ());
        particle.getBooleans().
            write(0, false);

        for (Player player : players)
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, particle);
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
            }
    }
}
