package messageCommunication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * Used in communication with the server in the drocsid application.
 * Contains the information that is passed to the server and back to client.
 *
 * @author Robbie Booth
 */
public class Message {
    private final int length;

    @JsonProperty("type")
    private final MessageType type;
    private final String senderId;//Id/username of sender
    private final String targetId;//Id of room
    private final long timestamp;


    private final byte[] payload;

    private static final Charset MESSAGE_CHARACTER_SET = StandardCharsets.UTF_8;

    public Message(){
        length = 0;
        type = null;
        senderId = null;
        targetId = null;
        timestamp = System.currentTimeMillis();
        payload = new byte[0];
    }

    public Message(int length, MessageType type, String senderId, String targetId, long timestamp, byte[] payload) {
        this.length = length;
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    /**
     * Constructs the message
     * @param length
     * @param type
     * @param senderId
     * @param targetId
     * @param timestamp
     * @param message if message is null payload is set to empty byte array
     */
    public Message(int length, MessageType type, String senderId, String targetId, long timestamp, String message) {
        this.length = length;
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.timestamp = timestamp;
        if(message == null){
            this.payload = new byte[0];
        }else{
            this.payload = message.getBytes(MESSAGE_CHARACTER_SET);
        }
    }


    /**
     * Gets the string representation of the payload.
     * WARNING: will work even if message type is not of text
     * @return string representation of payload
     * @author Robbie Booth
     */
    @JsonIgnore
    public String getTextMessage(){
        String message = new String(getPayload(), MESSAGE_CHARACTER_SET);
        return message;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getTargetId() {
        return targetId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPayload() {
        return payload;
    }

    public MessageType getType() {
        return type;
    }
}
