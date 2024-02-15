import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    TEXT,
    ROOMS,
    INITIALISATION,

    SELECT_ROOM;

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
        throw new IllegalArgumentException("Invalid MessageType: " + value);
    }

}//for future
