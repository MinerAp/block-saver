package in.nikitapek.blocksaver.serialization;

import com.amshulman.mbapi.util.CoreTypes;
import com.amshulman.typesafety.TypeSafeMap;
import com.amshulman.typesafety.impl.TypeSafeMapImpl;
import in.nikitapek.blocksaver.util.SupplementaryTypes;
import org.bukkit.Material;

import java.util.HashMap;

public final class PlayerInfo {
    public boolean isReceivingTextFeedback;
    public boolean isInReinforcementMode;
    private TypeSafeMap<Material, Integer> reinforcementUsesLeft;

    public PlayerInfo() {
        isReceivingTextFeedback = true;
        isInReinforcementMode = false;
        reinforcementUsesLeft = new TypeSafeMapImpl<>(new HashMap<Material, Integer>(), SupplementaryTypes.MATERIAL, CoreTypes.INTEGER);
    }

    public boolean hasUsed(Material material) {
        return reinforcementUsesLeft.containsKey(material);
    }

    public int getRemainingUses(Material material) {
        return reinforcementUsesLeft.get(material);
    }

    public void setRemainingUses(Material material, int remainingUses) {
        reinforcementUsesLeft.put(material, remainingUses);
    }
}
