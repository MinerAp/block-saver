package in.nikitapek.blocksaver.util;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public final class BlockSaverUtil {
    public static final short MILLISECONDS_PER_SECOND = 1000;
    public static final byte REINFORCEMENT_MAXIMIZING_COEFFICIENT = -2;

    // The ID of the packet used for particle effects.
    private static final byte PARTICLE_EFFECT_PACKET = 61;
    // The default potion to be used to represent a block's state.
    private static final byte DEFAULT_POTION_EFFECT = 22;
    // The data value for the particle effect packet required to send a potion effect.
    private static final int POTION_PARTICLE_EFFECT_ID = 2002;

    private BlockSaverUtil() {}

    public static void sendParticleEffect(final Location location, final float reinforcementValue) {
        final PacketContainer particle = ProtocolLibrary.getProtocolManager().createPacket(PARTICLE_EFFECT_PACKET);
        int data = DEFAULT_POTION_EFFECT;

        switch ((int) reinforcementValue) {
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
                write(0, POTION_PARTICLE_EFFECT_ID).
                write(1, data).
                write(2, (int) location.getX()).
                write(3, (int) location.getY()).
                write(4, (int) location.getZ());
        particle.getBooleans().
                write(0, false);

        for (final Player player : Bukkit.getServer().getOnlinePlayers()) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, particle);
            } catch (final InvocationTargetException e) {
                Bukkit.getLogger().log(Level.INFO, "Failed to send packet to: " + player.getName());
            }
        }
    }
}
