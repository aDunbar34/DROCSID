package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.UserMessage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class History {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path roomsPathDir = Paths.get("rooms");
    private static final Path usersPathDir = Paths.get("users");

    private static final String roomMessageFileName = "roomMessages.json";
    private static final int maxHistorySize = 10;
    private static final String roomDataFileName = "roomData.json";


    /**
     * If room exists returns array of messages, else returns empty array
     *
     * @param roomName Name of room to get history of messages
     * @return
     * @author Robbie Booth
     */
    public static List<UserMessage> readRoomHistory(String roomName) throws IOException {
        List<UserMessage> userMessages = new ArrayList<>();
        File roomPath = new File(roomsPathDir.toFile(), roomName);
        File roomMessages = new File(roomPath, roomMessageFileName);

        //Room doesn't exist
        if(!roomMessages.exists()){
            return userMessages;
        }
        String jsonString = readFileContents(roomMessages);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            userMessages = objectMapper.readValue(jsonString, new TypeReference<List<UserMessage>>() {});
        }catch (JsonProcessingException e){
            e.printStackTrace();
            return new ArrayList<>();
        }

        return userMessages;
    }

    /**
     * Reads the users rooms that they have access to and returns a list of rooms they have access to. If file
     * doesn't exist empty array is returned.
     *
     * @param username name of user whose rooms they belong to
     * @return list of rooms user has access to
     * @author Robbie Booth
     */
    public static List<String> readUsersRooms(String username) throws IOException {
        List<String> rooms = new ArrayList<>();
        File file = new File(usersPathDir.toFile(), username+".json");
        if(!file.exists()){
            return rooms;
        }

        String jsonString = readFileContents(file);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode roomsNode = jsonNode.get("rooms");
            if (roomsNode.isArray()) {
                for (JsonNode roomNode : roomsNode) {
                    rooms.add(roomNode.asText());
                }
            }
        }catch (JsonProcessingException e){
            e.printStackTrace();
            return new ArrayList<>();
        }

        return rooms;
    }

    /**
     * Reads all the users in room file and returns list of users. If room doesn't exist empty array is returned.
     *
     * @param roomName Name of room to get users who have access to
     * @return
     * @author Robbie Booth
     */
    public static List<String> readUsersAllowedInRoom(String roomName) throws IOException {
        List<String> usersAllowedInRoom = new ArrayList<>();
        File roomLocation = new File(roomsPathDir.toFile(), roomName);
        //create user path directory if it does not exist
        File roomDataFile = new File(roomLocation, roomDataFileName);
        if(!roomDataFile.exists()){
            return usersAllowedInRoom;
        }

        String jsonString = readFileContents(roomDataFile);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode roomsNode = jsonNode.get("users");
            if (roomsNode.isArray()) {
                for (JsonNode roomNode : roomsNode) {
                    usersAllowedInRoom.add(roomNode.asText());
                }
            }
        }catch (JsonProcessingException e){
            e.printStackTrace();
            return new ArrayList<>();
        }

        return usersAllowedInRoom;
    }

    /**
     * Creates a file with jsonContent if it doesn't already exist else it overrides it with jsonContent.
     * @param file file to be created
     * @param jsonContent content to be in file
     * @throws IOException
     */
    public static void createOrOverrideFile(File file, String jsonContent) throws IOException {
        RandomAccessFile fileWriter = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {

            // Open the file in read-write mode
            fileWriter = new RandomAccessFile(file, "rw");
            channel = fileWriter.getChannel();

            // Acquire an exclusive lock on the file
            lock = channel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

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
     * Reads the contents of the file provided. If the file doesn't exist null is returned. The file is read using a shared lock.
     * @param file file to be read
     * @return content of the file
     * @throws IOException
     * @author Robbie Booth
     */
    public static String readFileContents(File file) throws IOException {
        if(!file.exists()){
            return null;
        }
        RandomAccessFile fileReader = null;
        FileChannel channel = null;
        FileLock lock = null;
        String fileContent = null;
        try {
            // Open the file in read-write mode
            fileReader = new RandomAccessFile(file, "r");
            channel = fileReader.getChannel();

            // Acquire an exclusive lock on the file
            lock = channel.lock(0, Long.MAX_VALUE, true);//We acquire a shared lock to read the document data

            // Move the file pointer to the beginning of the file
            fileReader.seek(0);

            // Determine the length of the file
            long fileLength = fileReader.length();

            // Create a byte array to hold the file content
            byte[] content = new byte[(int) fileLength];

            // Read the entire content into the byte array
            fileReader.readFully(content);

            // Convert the byte array to a String (assuming the file contains text)
            fileContent = new String(content);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(lock != null){
                lock.release();
            }
            if(channel != null){
                channel.close();
            }
            if(fileReader != null){
                fileReader.close();
            }
        }
        return fileContent;
    }

    /**
     * Creates a room file by the roomName. If file exists then nothing happens.
     * @param roomName Name of room to create
     *
     * @author Robbie Booth
     */
    public static void createRoom(String roomName) throws IOException {
        //roomName invalid
        if(roomName == null || roomName.isBlank()){
            return;
        }

        File roomLocation = new File(roomsPathDir.toFile(), roomName);
        //create user path directory if it does not exist
        History.checkPathElseCreate(roomLocation.toPath());
        File roomDataFile = new File(roomLocation, roomDataFileName);
        File roomMessagesFile = new File(roomLocation, roomMessageFileName);
        if(roomDataFile.exists() && roomMessagesFile.exists()){//if both exists don't create
            return;
        }

        if(!roomDataFile.exists()){
            String roomDataFileContent = "{ \"roomName\": \""+roomName+"\", \"users\": [] }";
            createOrOverrideFile(roomDataFile, roomDataFileContent);
        }
        if(!roomMessagesFile.exists()){
            String roomMessageFileContent = "[]";
            createOrOverrideFile(roomMessagesFile, roomMessageFileContent);
        }
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

        String jsonContent = "{ \"username\": \""+username+"\", \"rooms\": [] }";
        createOrOverrideFile(file, jsonContent);
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

    public static void addMessageToHistory(UserMessage message, String roomName) throws IOException {
        File roomLocation = new File(roomsPathDir.toFile(), roomName);
        //create user path directory if it does not exist
        History.checkPathElseCreate(roomLocation.toPath());
        File roomMessagesFile = new File(roomLocation, roomMessageFileName);
        File roomDataFile = new File(roomLocation, roomDataFileName);
        RandomAccessFile fileWriter = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {

            // Open the file in read-write mode
            fileWriter = new RandomAccessFile(roomMessagesFile, "rw");
            channel = fileWriter.getChannel();

            lock = channel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

            // Read the content:
            // Set the file pointer to the beginning of the file to overwrite its content
            fileWriter.seek(0);
            long fileLength = fileWriter.length();
            byte[] content = new byte[(int) fileLength];
            fileWriter.readFully(content);

            // Convert the byte array to a String (assuming the file contains text)
            String fileContent = new String(content);
            List<UserMessage> userMessages = new ArrayList<>();

            //If the file doesn't exist we will get an empty file content so we will want not read value as that causes error and is unnecessary code
            if(!fileContent.isBlank()){
                ObjectMapper objectMapper = new ObjectMapper();
                try{
                    userMessages = objectMapper.readValue(fileContent, new TypeReference<List<UserMessage>>() {});
                }catch (JsonProcessingException e){
                e.printStackTrace();
                }
            }
            if(userMessages.size() >= maxHistorySize){
                userMessages.remove(0);//remove the oldest one
            }
            userMessages.add(message);

            String newHistory = objectMapper.writeValueAsString(userMessages);


            //write new history to file
            fileWriter.seek(0);
            fileWriter.setLength(0);
            fileWriter.writeBytes(newHistory);

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
        //If the room didn't exist we also need to make the roomData for that room
        if(!roomDataFile.exists()){
            createRoom(roomName);
        }
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
//        History.createRoom("hello");
//        List<UserMessage> userMessages = History.readRoomHistory("hello");
//        // Perform operations on the list of UserMessage objects
//        System.out.println(userMessages.size());
//        UserMessage userMessage = new UserMessage("Robbie", System.currentTimeMillis(), "hello new message22");
//        History.addMessageToHistory(userMessage, "he");
//        List<UserMessage> userMessages = History.readRoomHistory("he");
//
//        // Perform operations on the list of UserMessage objects
//        for (UserMessage userMessage : userMessages) {
//            System.out.println("Read UserMessage: " + userMessage.getMessage());
//        }

//        List<String> usersRooms = History.readUsersRooms("robert");
//        System.out.println("size:" + usersRooms.size());
//        for (String room: usersRooms){
//            System.out.println(room);
//        }

//        List<String> usersAllowedInRoom = History.readUsersAllowedInRoom("hello");
//        System.out.println("size:" + usersAllowedInRoom.size());
//        for (String room: usersAllowedInRoom){
//            System.out.println(room);
//        }
    }
}
