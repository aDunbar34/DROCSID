package messageCommunication;

/**
 * The messages that are used in history. These are what is sent between users.
 * @author Robbie Booth
 */
public class UserMessage implements Comparable<UserMessage> {

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

    /**
     * Sorts the UserMessages by timestamp. Lower timestamp being -1, same 0, and higher timestamp being 1
     * @param o the object to be compared.
     * @author Robbie Booth
     */
    @Override
    public int compareTo(UserMessage o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
