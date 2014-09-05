package in.nikitapek.blocksaver.listeners;

import in.nikitapek.blocksaver.management.ReinforcementManager;
import in.nikitapek.blocksaver.util.BlockSaverConfigurationContext;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

public class PacketListener extends PacketAdapter {
	ReinforcementManager reinforcementManager;

    public PacketListener(BlockSaverConfigurationContext configurationContext) {
        super(configurationContext.plugin, PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        plugin = configurationContext.plugin;
        reinforcementManager = configurationContext.getReinforcementManager();
    }

	@Override
    public void onPacketReceiving(PacketEvent event) {
        // Only monitor for Client.BLOCK_DIG events.
        if (!PacketType.Play.Client.BLOCK_DIG.equals(event.getPacketType())) {
        	return;
        }

        // Retrieve the position and digStatus information from the packet.
        final StructureModifier<Integer> ints = event.getPacket().getIntegers();
        final int x = ints.read(0);
        final int y = ints.read(1);
        final int z = ints.read(2);
        int digStatus = ints.read(4);

        final Player player = event.getPlayer();

        // Begin slowly damaging the block the player is targetting.
        reinforcementManager.damageBlockNew(plugin, digStatus, player, x, y, z);
    }
}
