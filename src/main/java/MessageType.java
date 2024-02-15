import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MessageType {
    TEXT,
    ROOMS,
    INITIALISATION,

    SELECT_ROOM,

}//for future
