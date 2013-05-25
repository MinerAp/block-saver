package in.nikitapek.blocksaver.serialization;

public final class PlayerInfo {
    private boolean isRecievingTextFeedback;

    public PlayerInfo() {
        isRecievingTextFeedback = true;
    }

    public boolean isRecievingTextFeedback() {
        return isRecievingTextFeedback;
    }

    public void setRecievingTextFeedback(final boolean isRecievingTextFeedback) {
        this.isRecievingTextFeedback = isRecievingTextFeedback;
    }
}
