package in.nikitapek.blocksaver;

import com.amshulman.mbapi.MbapiPlugin;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import de.diddiz.LogBlock.LogBlock;
import in.nikitapek.blocksaver.commands.CommandBlockSaver;
import in.nikitapek.blocksaver.events.BlockSaverListener;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public final class BlockSaverPlugin extends MbapiPlugin {
    private BlockSaverInfoManager infoManager;
    private LogBlock logBlockPlugin;

    @Override
    public void onEnable() {
        logBlockPlugin = (LogBlock) Bukkit.getPluginManager().getPlugin("LogBlock");

        final BlockSaverConfigurationContext configurationContext = new BlockSaverConfigurationContext(this);
        infoManager = configurationContext.infoManager;

        registerCommandExecutor(new CommandBlockSaver(configurationContext));
        registerEventHandler(new BlockSaverListener(configurationContext));

        super.onEnable();
    }

    public void sendParticleEffect(final Location location) {
        final PacketContainer particle = ProtocolLibrary.getProtocolManager().createPacket(61);
        int data = 22;

        switch ((int) infoManager.getReinforcement(location).getReinforcementValue()) {
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

        for (final Player player : getServer().getOnlinePlayers()) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, particle);
            } catch (final InvocationTargetException e) {
                Bukkit.getLogger().log(Level.INFO, "Failed to send packet to: " + player.getName());
            }
        }
    }
}
