package in.nikitapek.blocksaver.commands.blocksaver;

import com.amshulman.mbapi.commands.PlayerOnlyCommand;
import com.amshulman.typesafety.TypeSafeCollections;
import com.amshulman.typesafety.TypeSafeList;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import in.nikitapek.blocksaver.commands.CommandBlockSaver.BlockSaverCommands;
import in.nikitapek.blocksaver.management.BlockSaverInfoManager;
import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.serialization.PlayerInfo;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandReinforce extends PlayerOnlyCommand {
    private final BlockSaverInfoManager infoManager;
    private final ReinforcementManager reinforcementManager;

    public CommandReinforce(final BlockSaverConfigurationContext configurationContext) {
        super(configurationContext, BlockSaverCommands.REINFORCE, 0, 0);
        infoManager = configurationContext.infoManager;
        reinforcementManager = configurationContext.getReinforcementManager();
    }

    @Override
    protected boolean executeForPlayer(final Player player, final TypeSafeList<String> args) {
        PlayerInfo playerInfo = infoManager.getPlayerInfo(player.getName());
        WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

        Selection selection = worldEditPlugin.getSelection(player);

        if (selection.getArea() == 0) {
            if (playerInfo.isReceivingTextFeedback) {
                player.sendMessage("No blocks were selected for reinforcement.");
            }
            return true;
        }

        Iterable<BlockVector> selector;
        if (!(selection instanceof CuboidSelection || selection instanceof Polygonal2DRegion)) {
            try {
                selector = selection.getRegionSelector().getRegion();
            } catch (IncompleteRegionException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            if (playerInfo.isReceivingTextFeedback) {
                player.sendMessage("Invalid selection.");
            }
            return false;
        }

        int reinforceCount = 0;
        for (BlockVector blockVector : selector) {
            Block block = selection.getWorld().getBlockAt(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());

            if (!reinforcementManager.isReinforceable(block)) {
                continue;
            }

            reinforcementManager.reinforce(player.getName(), block.getLocation());
            reinforceCount++;
        }

        if (playerInfo.isReceivingTextFeedback) {
            if (reinforceCount == 0) {
                player.sendMessage(ChatColor.GRAY + "You failed to reinforce any blocks.");
            }
            player.sendMessage(ChatColor.GRAY + "Successfully reinforced " + reinforceCount + " blocks.");
        }

        return true;
    }

    @Override
    public TypeSafeList<String> onTabComplete(final CommandSender sender, final TypeSafeList<String> args) {
        return TypeSafeCollections.emptyList();
    }
}
