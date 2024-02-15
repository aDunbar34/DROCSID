import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;

public class ClientData {
    private String username;
    private SocketChannel userChannel;
    private Set<String> userRooms;
    private String currentRoom;

    public ClientData(String username, SocketChannel userChannel, Set<String> userRooms) {
        this.username = username;
        this.userChannel = userChannel;
        this.userRooms = userRooms;
    }

    public ClientData(String username, SocketChannel userChannel, Set<String> userRooms, String currentRoom) {
        this.username = username;
        this.userChannel = userChannel;
        this.userRooms = userRooms;
        this.currentRoom = currentRoom;
    }

    public SocketChannel getUserChannel() {
        return userChannel;
    }

    public void setUserChannel(SocketChannel userChannel) {
        this.userChannel = userChannel;
    }

    public Set<String> getUserRooms() {
        return userRooms;
    }

    public void addRoom(String room){
        userRooms.add(room);
    }

    /**
     * Removes the room and if the users current room is that room it sets the current room to null
     * */
    public void removeRoom(String room){
        if(currentRoom.equals(room)){
            currentRoom = null;
        }
        userRooms.remove(room);
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public String getUsername() {
        return username;
    }
}
