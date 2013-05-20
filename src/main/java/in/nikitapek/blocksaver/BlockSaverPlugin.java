package in.nikitapek.blocksaver;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

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
        PacketContainer particle = ProtocolLibrary.getProtocolManager().createPacket(63);

        String name = "portal";

        switch(infoManager.getReinforcementValue(location)) {
            case 1:
                name = "flame";
                break;
            case 2:
                name = "lava";
                break;
            case 3:
                name = "spell";
                break;
            case 4:
                name = "fireworksSpark";
                break;
            case 5:
                name = "smoke";
                break;
            case 6:
                name = "witchMagic";
                break;
            default:
                break;
        }

        particle.getStrings().
            write(0, name);
        particle.getIntegers().
            write(0, 30);

        for (Player player : players)
            try {
                particle.getFloat().
                    write(0, ((float) location.getX())).
                    write(1, ((float) location.getY())).
                    write(2, ((float) location.getZ())).
                    write(3, 0F).
                    write(4, 0F).
                    write(5, 0F).
                    write(6, 0.6F);

                ProtocolLibrary.getProtocolManager().sendServerPacket(player, particle);
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
            }
    }
}
