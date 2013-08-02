package in.nikitapek.blocksaver.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

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

    public static void sendParticleEffect(final Location location, final int reinforcementValue, final int reinforcementValueCoefficient) {
        World world = location.getWorld();

        Firework firework = world.spawn(new Location(world, location.getBlockX() + 0.5d, location.getBlockY() + 0.5d, location.getBlockZ() + 0.5d), Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.clearEffects();
        fireworkMeta.setPower(1);
        Color color;

        // Normalize the RV so that the change from RVC to 0 is gradual.
        int numberOfColors = 6;
        double fadeInterval = reinforcementValueCoefficient / numberOfColors;
        int fadeStage = (int) Math.floor(reinforcementValue / fadeInterval);

        switch (fadeStage) {
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

        fireworkMeta.addEffects(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(fireworkMeta);

        try {
            Object nms_world = getMethod(world.getClass(), "getHandle").invoke(world);
            getMethod(nms_world.getClass(), "broadcastEntityEffect").invoke(nms_world, new Object[] { getMethod(firework.getClass(), "getHandle").invoke(firework), (byte) 17 });
            firework.remove();
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private static Method getMethod(Class<?> cl, String method) {
        for (Method m : cl.getMethods()) {
            if (m.getName().equals(method)) {
                return m;
            }
        }
        return null;
    }

    public static void playMusicalEffect(final Location location, final int reinforcementValue) {
        // Because 2 is the highest pitch value (the sound is played at twice the regular speed), then we set pitch to 2 and then lower the pitch depending on how high the RV is.
        float pitch = (2 - reinforcementValue * 0.1f);

        // Pitch ranges from 0.5 to 2.0 so for RV of 15 or higher the sound does not go lower.
        if (pitch < 0.5) {
            pitch = 0.5f;
        }

        location.getWorld().playSound(location, Sound.NOTE_PIANO, 1.0f, pitch);
    }
}
