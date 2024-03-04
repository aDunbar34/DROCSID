package client;


/**
 * Stores the clients current room - this is to allow for synchronization of the chatRoomId between the {@link ClientConsumer} and {@link ClientProducer}
 * @author Robbie Booth
 */
public class ChatRoomData {
    private String chatRoomId;

    public ChatRoomData(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}
