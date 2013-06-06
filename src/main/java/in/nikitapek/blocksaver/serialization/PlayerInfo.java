package in.nikitapek.blocksaver.serialization;

public final class PlayerInfo {
    private boolean isReceivingTextFeedback;

    public PlayerInfo() {
        isReceivingTextFeedback = true;
    }

    public boolean isReceivingTextFeedback() {
        return isReceivingTextFeedback;
    }

    public void setReceivingTextFeedback(final boolean isReceivingTextFeedback) {
        this.isReceivingTextFeedback = isReceivingTextFeedback;
    }
}
