package server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class History {

    private ObjectMapper objectMapper = new ObjectMapper();


    /**
     * If room exists returns array of messages, else returns empty array
     * @param roomName Name of room to get history of messages
     *
     * @author Robbie Booth
     */
    public void readRoomHistory(String roomName){
        //If room exists

    }

    /**
     * Reads the users rooms that they have access to and returns a list of rooms they have access to. If file
     * doesn't exist empty array is returned.
     * @param username name of user whose rooms they belong to
     *
     * @author Robbie Booth
     */
    public void readUsersRooms(String username){

    }

    /**
     * Reads all the users in room file and returns list of users. If room doesn't exist empty array is returned.
     * @param roomName Name of room to get users who have access to
     *
     * @author Robbie Booth
     */
    public void readUsersAllowedInRoom(String roomName){

    }

    /**
     * Creates a room file by the roomName. If file exists then nothing happens.
     * @param roomName Name of room to create
     *
     * @author Robbie Booth
     */
    public void createRoom(String roomName){

    }


    /**
     * Creates a user file of the username provided. If file exists nothing happens.
     * @param username Name of user to create
     *
     * @author Robbie Booth
     */
    public void createUser(String username){

    }

    /**
     * Adds the usernames to the room specified, if the room doesn't exist it is created. If the user is already part
     * of room they will remain in room.
     * @param roomName name of room to add users to
     * @param usernames List of users to be added to room
     *
     * @author Robbie Booth
     */
    public void addUsersToRoom(String roomName, List<String> usernames){

    }

    public void addMessageToHistory(){

    }
}
