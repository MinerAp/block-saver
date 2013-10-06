package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.util.ConstructorFactory;
import in.nikitapek.blocksaver.serialization.PlayerInfo;

public final class PlayerInfoConstructorFactory extends ConstructorFactory<PlayerInfo> {
    @Override
    public PlayerInfo get() {
        return new PlayerInfo();
    }
}
