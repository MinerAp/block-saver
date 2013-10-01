package in.nikitapek.blocksaver.serialization;

public final class PlayerInfo {
    public boolean isReceivingTextFeedback;
    public boolean isInReinforcementMode;

    public PlayerInfo() {
        isReceivingTextFeedback = true;
        isInReinforcementMode = false;
    }
}
