package messageCommunication;

/**
 * The messages that are used in history. These are what is sent between users.
 * @author Robbie Booth
 */
public class UserMessage {

    private final String senderId;//Id/username of sender

    private final long timestamp;

    private final String message;


    public UserMessage() {
        this.senderId = null;
        this.timestamp = System.currentTimeMillis();
        this.message = null;
    }

    public UserMessage(String senderId, long timestamp, String message) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
