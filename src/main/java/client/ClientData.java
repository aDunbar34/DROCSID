package client;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The data associated with the client
 * @author Robbie Booth
 */
public class ClientData {
    private String username;
    private SocketChannel userChannel;
    private Set<String> userRooms;

    private Set<String> incomingFriendRequests;
    private Set<String> outgoingFriendRequests;
    private Set<String> friends;
    private String currentRoom;

    public ClientData(String username, Set<String> userRooms, String currentRoom) {
        this.username = username;
        this.userRooms = userRooms;
        this.currentRoom = currentRoom;
        this.incomingFriendRequests = new HashSet<>();
        this.outgoingFriendRequests = new HashSet<>();
        this.friends = new HashSet<>();
    }

    public ClientData(String username, SocketChannel userChannel, Set<String> userRooms) {
        this.username = username;
        this.userChannel = userChannel;
        this.userRooms = userRooms;
        this.incomingFriendRequests = new HashSet<>();
        this.outgoingFriendRequests = new HashSet<>();
        this.friends = new HashSet<>();
    }

    public ClientData(String username, SocketChannel userChannel, Set<String> userRooms, String currentRoom) {
        this.username = username;
        this.userChannel = userChannel;
        this.userRooms = userRooms;
        this.currentRoom = currentRoom;
        this.incomingFriendRequests = new HashSet<>();
        this.outgoingFriendRequests = new HashSet<>();
        this.friends = new HashSet<>();
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
     *
     * @author Robbie Booth
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

    public Set<String> getIncomingFriendRequests() { return incomingFriendRequests; }

    public void setIncomingFriendRequests(Set<String> incomingFriendRequests) {this.incomingFriendRequests = incomingFriendRequests;}

    public Set<String> getOutgoingFriendRequests() {return outgoingFriendRequests;}

    public void setOutgoingFriendRequests(Set<String> outgoingFriendRequests) {this.outgoingFriendRequests = outgoingFriendRequests;}

    public Set<String> getFriends() {return friends;}

    public void setFriends(Set<String> friends) {this.friends = friends;}
}
