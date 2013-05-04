package in.nikitapek.blocksaver.events;

import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import com.amshulman.mbapi.util.CoreTypes;
import com.amshulman.mbapi.util.LocationUtil;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;

public class BlockSaverListener implements Listener {

    private final TypeSafeMap<String, Long> cooldownTimes;
    private final int cooldownMillis;
    private final Random random;

    public BlockSaverListener(BlockSaverConfigurationContext configurationContext) {
        cooldownTimes = new TypeSafeMapImpl<String, Long>(new HashMap<String, Long>(), CoreTypes.STRING, CoreTypes.LONG);

        //cooldownMillis = configurationContext.pearlCooldownTime * 1000;
        cooldownMillis = 0;
        random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(final BlockBreakEvent event) {
        if (event.getPlayer().getItemInHand().getType().equals(Material.STONE_PICKAXE) && event.getBlock().getType().equals(Material.GRASS)) {
            if (random.nextBoolean()) {
                event.getPlayer().playEffect(event.getBlock().getLocation(), Effect.POTION_BREAK, 0);
                event.setCancelled(true);
            }
        }
    }
}
