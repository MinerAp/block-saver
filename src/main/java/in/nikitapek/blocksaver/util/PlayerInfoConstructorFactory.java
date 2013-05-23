package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.serialization.PlayerInfo;

import com.amshulman.mbapi.util.ConstructorFactory;

public class PlayerInfoConstructorFactory extends ConstructorFactory<PlayerInfo> {
    @Override
    public PlayerInfo get() {
        return new PlayerInfo();
    }
}
