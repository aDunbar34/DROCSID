package messageCommunication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Interpreted by the server to understand what the context of message it's attached to is
 *
 * @author Robbie Booth
 */
public enum MessageType {
    TEXT,
    ROOMS,
    INITIALISATION,

    SELECT_ROOM,

    HISTORY,

    ONLINE_STATUSES,
    CREATE_ROOM,

    ADD_MEMBERS,

    FRIENDS_LIST,
    SEND_FRIEND_REQUEST,

    FRIEND_REQUEST_LIST,
    ACCEPT_FRIEND,

    FILE_SEND_SIGNAL,
    FILE_RECEIVE_SIGNAL,
    FILE_LISTEN_SIGNAL,

    STREAM_SIGNAL;



    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static MessageType fromJson(String value) {
        for (MessageType messageType : values()) {
            if (messageType.name().equalsIgnoreCase(value)) {
                return messageType;
            }
        }
        throw new IllegalArgumentException("Invalid messageCommunication.MessageType: " + value);
    }

}//for future
