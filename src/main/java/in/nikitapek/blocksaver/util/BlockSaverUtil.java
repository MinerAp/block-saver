package in.nikitapek.blocksaver.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public final class BlockSaverUtil {
    public static final short MILLISECONDS_PER_SECOND = 1000;
    public static final byte REINFORCEMENT_MAXIMIZING_COEFFICIENT = -2;
    public static final int HANDS_TOOL_CODE = -1;
    public static final int ALL_TOOL_CODE = -2;

    // The ID of the packet used for particle effects.
    private static final byte PARTICLE_EFFECT_PACKET = 61;
    // The default potion to be used to represent a block's state.
    private static final byte DEFAULT_POTION_EFFECT = 22;
    // The data value for the particle effect packet required to send a potion effect.
    private static final int POTION_PARTICLE_EFFECT_ID = 2002;

    private BlockSaverUtil() {}

    public static void sendParticleEffect(final Location location, final float reinforcementValue) {
        //final PacketContainer particle = ProtocolLibrary.getProtocolManager().createPacket(PARTICLE_EFFECT_PACKET);
        //int data = DEFAULT_POTION_EFFECT;

        Firework firework = location.getWorld().spawn(location, Firework.class);
        firework.setVelocity(new Vector(0, -60, 0));
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.setPower(1);
        Color color;

        switch ((int) reinforcementValue) {
            case 0:
                // If the block is not reinforced, but has just been damaged as a reinforced block (presumably due to the grace period), then we play the "nearly broken" effect.
                color = Color.RED;
                break;
            case 1:
                color = Color.ORANGE;
                break;
            case 2:
                color = Color.YELLOW;
                break;
            case 3:
                color = Color.GREEN;
                break;
            case 4:
                color = Color.BLUE;
                break;
            default:
                color = Color.PURPLE;
                break;
        }

        fireworkMeta.addEffects(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.BALL_LARGE).build());
        firework.setFireworkMeta(fireworkMeta);

        /**
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
         **/
    }
}
