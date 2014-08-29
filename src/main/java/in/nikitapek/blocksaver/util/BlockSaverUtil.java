package in.nikitapek.blocksaver.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Bed;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

public final class BlockSaverUtil {
    public static final short MILLISECONDS_PER_SECOND = 1000;
    public static final int HANDS_TOOL_CODE = -1;
    public static final int ALL_TOOL_CODE = -2;

    private BlockSaverUtil() {
    }

    public static Location getProperLocation(final Location location) {
        Block block = location.getBlock();
        Material blockType = block.getType();

        // Select the base of the piston.
        if (blockType.equals(Material.PISTON_EXTENSION)) {
            MaterialData data = block.getState().getData();
            BlockFace direction = null;

            // Check the block it pushed directly
            if (data instanceof PistonExtensionMaterial) {
                direction = ((PistonExtensionMaterial) data).getFacing();
            }

            if (direction != null) {
                return block.getRelative(direction.getOppositeFace()).getLocation();
            }
        } else if (blockType.equals(Material.WOODEN_DOOR) || blockType.equals(Material.IRON_DOOR)) {
            // If the user selected the top of a door, return the bottom.
            Block blockBelow = block.getRelative(BlockFace.DOWN);
            if (blockBelow.getType().equals(Material.WOODEN_DOOR) || blockBelow.getType().equals(Material.IRON_DOOR)) {
                return blockBelow.getLocation();
            }
        } else if (blockType.equals(Material.BED_BLOCK)) {
            Bed data = (Bed) block.getState().getData();

            // If the selected block is the head of the bed, return the selected block's location.
            if (data.isHeadOfBed()) {
                return location;
            }

            // Otherwise, return the location of the head of the bed.
            return block.getRelative(data.getFacing()).getLocation();
        }

        return location;
    }
}
