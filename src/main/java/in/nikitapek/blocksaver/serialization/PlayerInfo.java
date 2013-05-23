package in.nikitapek.blocksaver.serialization;

public class PlayerInfo {
    private boolean isRecievingTextFeedback;

    public PlayerInfo() {
        isRecievingTextFeedback = true;
    }

    public boolean isRecievingTextFeedback() {
        return isRecievingTextFeedback;
    }

    public void setRecievingTextFeedback(boolean isRecievingTextFeedback) {
        this.isRecievingTextFeedback = isRecievingTextFeedback;
    }
}
