package server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class History {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path roomsPathDir = Paths.get("rooms");
    private static final Path usersPathDir = Paths.get("users");


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
     * @param username Name of user to create. If empty no file is created.
     *
     * @author Robbie Booth
     */
    public static void createUser(String username) throws IOException {
        //create user path directory if it does not exist
        History.checkPathElseCreate(usersPathDir);

        File file = new File(usersPathDir.toFile(), username+".json");
        if(file.exists()){
            return;
        }
        //username invalid
        if(username == null || username.isBlank()){
            return;
        }
        RandomAccessFile fileWriter = null;
        FileChannel channel = null;
        FileLock lock = null;
        String jsonContent = "{ \"username\": \""+username+"\", \"rooms\": [] }";
        try {

            // Open the file in read-write mode
            fileWriter = new RandomAccessFile(file, "rw");
            channel = fileWriter.getChannel();

            // Acquire an exclusive lock on the file
            lock = channel.lock();

            // Set the file pointer to the beginning of the file to overwrite its content
            fileWriter.seek(0);

            // Truncate the file to remove any existing content
            fileWriter.setLength(0);

            fileWriter.writeBytes(jsonContent);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(lock != null){
                lock.release();
            }
            if(channel != null){
                channel.close();
            }
            if(fileWriter != null){
                fileWriter.close();
            }
        }
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

    /**
     * If path doesn't exist then it creates it.
     * @param path path to check if exists/create
     * @throws IOException
     * @author Robbie Booth
     */
    public static void checkPathElseCreate(Path path) throws IOException {
        if(!Files.exists(path)){
            Files.createDirectories(path);
        }
    }


    public static void main(String[] args) throws IOException {
        History.createUser("robert");
    }
}
