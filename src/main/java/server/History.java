package server;

import client.ClientData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messageCommunication.UserMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class History {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path roomsPathDir = Paths.get("rooms");
    private static final Path usersPathDir = Paths.get("users");

    private static final String roomMessageFileName = "roomMessages.json";
    private static final int maxHistorySize = 10;
    private static final String roomDataFileName = "roomData.json";


    /**
     * Returns a json user based on the parameters given
     * @param username
     * @param rooms
     * @param friends
     * @param outgoingFriendRequests
     * @param incomingFriendRequests
     * @return json user based on the parameters given
     * @author Robbie Booth
     */
    private static String getUserString(String username, String rooms, String friends, String outgoingFriendRequests, String incomingFriendRequests) {
        String changedContent = "{ \"username\": \""+ username +"\", \"rooms\": "+rooms+" , \"friends\": "+friends+" , \"outgoingFriendRequests\": "+outgoingFriendRequests+" , \"incomingFriendRequests\": "+incomingFriendRequests+" }";
        return changedContent;
    }

    /**
     * Returns the json which is stored in history of the clientData. If error then default user is returned.
     * @param clientData
     * @return json of the clientData. If error then default user is returned.
     * @author Robbie Booth
     */
    private static String clientDataToString(ClientData clientData) {

        String rooms = "[]";
        String friends = "[]";
        String outgoingFriendRequests = "[]";
        String incomingFriendRequests = "[]";
        try {
            rooms = objectMapper.writeValueAsString(clientData.getUserRooms());
            friends = objectMapper.writeValueAsString(clientData.getFriends());
            outgoingFriendRequests = objectMapper.writeValueAsString(clientData.getOutgoingFriendRequests());
            incomingFriendRequests = objectMapper.writeValueAsString(clientData.getIncomingFriendRequests());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return getUserString(clientData.getUsername(), rooms, friends, outgoingFriendRequests, incomingFriendRequests);
    }

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
     * Reads the username specified user file if it exists returns the {@link ClientData} else returns null
     * @param username username to be read
     * @return {@link ClientData} if the user exists else it returns null
     * @author Robbie Booth
     */
    public static ClientData readUser(String username) throws IOException {
        File file = new File(usersPathDir.toFile(), username+".json");
        if(!file.exists()){
            return null;
        }

        String jsonString = readFileContents(file);
        return getClientData(username, jsonString);
    }

    /**
     * Refactor of code to get the client data of a json string. Throws Json processing error.
     * @param username
     * @param jsonString
     * @return
     */
    private static ClientData getClientData(String username, String jsonString) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jsonNode = objectMapper.readTree(jsonString);
        Set<String> roomsSet = convertNodeToSet(jsonNode.get("rooms"));
        Set<String> outgoingFriendRequestsSet = convertNodeToSet(jsonNode.get("outgoingFriendRequests"));
        Set<String> incomingFriendRequestsSet = convertNodeToSet(jsonNode.get("incomingFriendRequests"));
        Set<String> friendsSet = convertNodeToSet(jsonNode.get("friends"));
        return new ClientData(username, roomsSet, incomingFriendRequestsSet, outgoingFriendRequestsSet, friendsSet);
    }

    /**
     * Takes a json node and if its an array it loops through the object and returns a set. If the node is null, an empty set is returned.
     * @param node node to be converted into a set. If the node is null, an empty set is returned.
     * @return a set of the values of the node. If the node is null, an empty set is returned.
     * @author Robbie Booth
     */
    private static Set<String> convertNodeToSet(JsonNode node){
        Set<String> set = new HashSet<>();
        if(node == null || node.isNull()){
            return set;
        }

        if (node.isArray()) {
            for (JsonNode textNode : node) {
                set.add(textNode.asText());
            }
        }
        return set;
    }

    /**
     * Accepts the friend request that the requester sent to the receiver becoming friends. If either the requester
     * or receiver file doesn't exist, it is created.
     * @param requester Person who sent the friend request
     * @param receiver Person who accepted the friend request
     * @author Robbie Booth
     */
    public static void acceptFriendRequest(String requester, String receiver) throws IOException {
        updateUserFriends(requester, receiver, HistoryFriendType.ACCEPT);
    }

    /**
     * Adds the friend request to the requesters outgoing and receivers incoming files. If either the requester
     * or receiver file doesn't exist, it is created.
     * @param requester Person who sent the friend request
     * @param receiver Person who received the friend request
     * @author Robbie Booth
     */
    public static void sendFriendRequest(String requester, String receiver) throws IOException {
        updateUserFriends(requester, receiver, HistoryFriendType.SEND);
    }

    /**
     * Refactored. Locks the requesters file and the receivers file and does the history type operation on them. If the file does not exist it is created.
     * @param requester person who made the friend request
     * @param receiver person who received the friend request
     * @param type the type of the friend request either send or accept
     * @throws IOException
     * @author Robbie Booth
     */
    private static void updateUserFriends(String requester, String receiver, HistoryFriendType type) throws IOException{
        //create user path directory if it does not exist
        History.checkPathElseCreate(usersPathDir);

        File requesterFile = new File(usersPathDir.toFile(), requester+".json");
        File receiverFile = new File(usersPathDir.toFile(), receiver+".json");

        RandomAccessFile requesterWriter = null;
        FileChannel requesterChannel = null;
        FileLock requesterLock = null;

        RandomAccessFile receiverWriter = null;
        FileChannel receiverChannel = null;
        FileLock receiverLock = null;

        //User stuff:

        try {
            // Open the file in read-write mode
            requesterWriter = new RandomAccessFile(requesterFile, "rw");
            requesterChannel = requesterWriter.getChannel();

            receiverWriter = new RandomAccessFile(receiverFile, "rw");
            receiverChannel = receiverWriter.getChannel();

            //Lock both requester and receiver channel
            requesterLock = requesterChannel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document
            receiverLock = receiverChannel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

            // Read the content of requester:
            // Set the file pointer to the beginning of the file to overwrite its content
            requesterWriter.seek(0);
            long fileLength = requesterWriter.length();
            byte[] content = new byte[(int) fileLength];
            requesterWriter.readFully(content);
            // Convert the byte array to a String
            String requesterJsonString = new String(content);
            ClientData requesterClientData = getClientData(requester,requesterJsonString);


            // Read the content of receiver:
            // Set the file pointer to the beginning of the file to overwrite its content
            receiverWriter.seek(0);
            fileLength = receiverWriter.length();
            content = new byte[(int) fileLength];
            receiverWriter.readFully(content);
            // Convert the byte array to a String
            String receiverJsonString = new String(content);
            ClientData receiverClientData = getClientData(receiver,receiverJsonString);


            //If they are already friends don't add the request
            if(receiverClientData.getFriends().contains(requester) && requesterClientData.getFriends().contains(receiver)){
                return;
            }

            if(type.equals(HistoryFriendType.SEND)){//sending a request
                //If the receiver has an outgoing request to the requester then accept - as they both want to be friends:
                if(receiverClientData.getOutgoingFriendRequests().contains(requester) || requesterClientData.getIncomingFriendRequests().contains(receiver)){
                    //accept
                    acceptFriendRequest(receiverClientData, requesterClientData);
                }else{
                    //add outgoing requesters and incoming receivers
                    requesterClientData.getOutgoingFriendRequests().add(receiver);
                    receiverClientData.getIncomingFriendRequests().add(requester);
                }
            }else if(type.equals(HistoryFriendType.ACCEPT)){//accepting a request

                if(!receiverClientData.getIncomingFriendRequests().contains(requester)){
                    System.out.println("Receiver: "+receiver +" has no incoming friend requests in history for requester: "+requester+"!");
                }else if(!requesterClientData.getOutgoingFriendRequests().contains(receiver)){
                    System.out.println("Requester: "+requester +" has no outgoing friend requests in history for reciever: "+receiver+"!");
                }else{
                    //accept if request is actually there:
                    acceptFriendRequest(receiverClientData, requesterClientData);
                }
            }

            //write to both files
            receiverWriter.seek(0);
            receiverWriter.setLength(0);
            receiverWriter.writeBytes(clientDataToString(receiverClientData));

            requesterWriter.seek(0);
            requesterWriter.setLength(0);
            requesterWriter.writeBytes(clientDataToString(requesterClientData));

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }finally {
            //release locks
            if(requesterLock != null){
                requesterLock.release();
            }
            if(receiverLock != null){
                receiverLock.release();
            }


            if(requesterChannel != null){
                requesterChannel.close();
            }
            if(requesterWriter != null){
                requesterWriter.close();
            }


            if(receiverChannel != null){
                receiverChannel.close();
            }
            if(receiverWriter != null){
                receiverWriter.close();
            }
        }
    }

    /**
     * Refactored to become friends of requester and receiver.
     * @param receiverClientData
     * @param requesterClientData
     */
    private static void acceptFriendRequest(ClientData receiverClientData, ClientData requesterClientData) {
        receiverClientData.getIncomingFriendRequests().remove(requesterClientData.getUsername());
        requesterClientData.getIncomingFriendRequests().remove(receiverClientData.getUsername());

        receiverClientData.getOutgoingFriendRequests().remove(requesterClientData.getUsername());
        requesterClientData.getOutgoingFriendRequests().remove(receiverClientData.getUsername());

        receiverClientData.getFriends().add(requesterClientData.getUsername());
        requesterClientData.getFriends().add(receiverClientData.getUsername());
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

            //Remove lock so we can see document getting overwritten when it shouldn't be
//            // Acquire an exclusive lock on the file
//            lock = channel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

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

            //Remove lock to show problems for testing
//            // Acquire an exclusive lock on the file
//            lock = channel.lock(0, Long.MAX_VALUE, true);//We acquire a shared lock to read the document data

            // Move the file pointer to the beginning of the file
            fileReader.seek(0);

            // Determine the length of the file
            long fileLength = fileReader.length();

            // Create a byte array to hold the file content
            byte[] content = new byte[(int) fileLength];

            // Read the entire content into the byte array
            fileReader.readFully(content);

            // Convert the byte array to a String
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

        String jsonContent = getUserString(username, "[]","[]","[]","[]");
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
    public static void addUsersToRoom(String roomName, List<String> usernames) throws IOException {
        //Room stuff:
        File roomLocation = new File(roomsPathDir.toFile(), roomName);
        //create user path directory if it does not exist
        History.checkPathElseCreate(roomLocation.toPath());
        File roomMessagesFile = new File(roomLocation, roomMessageFileName);
        File roomDataFile = new File(roomLocation, roomDataFileName);
        RandomAccessFile roomFileWriter = null;
        FileChannel roomChannel = null;
        FileLock roomLock = null;

        //User stuff:

        try {
            // Open the file in read-write mode
            roomFileWriter = new RandomAccessFile(roomDataFile, "rw");
            roomChannel = roomFileWriter.getChannel();

            roomLock = roomChannel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

            // Read the content:
            // Set the file pointer to the beginning of the file to overwrite its content
            roomFileWriter.seek(0);
            long fileLength = roomFileWriter.length();
            byte[] content = new byte[(int) fileLength];
            roomFileWriter.readFully(content);

            // Convert the byte array to a String
            String jsonString = new String(content);
            ObjectMapper objectMapper = new ObjectMapper();

            HashSet<String> usersInRoom = new HashSet<>(usernames);//add all the new users to the set
            //then get the ones currently in it and add them as well
            String changedContent = null;
            if(!jsonString.isBlank()) {
                try {
                    JsonNode rootNode = objectMapper.readTree(jsonString);
                    JsonNode roomsNode = rootNode.get("users");
                    if (roomsNode.isArray()) {
                        for (JsonNode roomNode : roomsNode) {
                            usersInRoom.add(roomNode.asText());
                        }
                    }
                    ArrayNode newUsersNode = objectMapper.valueToTree(usersInRoom);
                    ((ObjectNode) rootNode).set("users", newUsersNode);
                    changedContent = objectMapper.writeValueAsString(rootNode);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }else{
                String users = objectMapper.writeValueAsString(usersInRoom);
                changedContent = "{ \"roomName\": \""+roomName+"\", \"users\": "+users+" }";
            }
            if(changedContent != null){
                //write new history to file
                roomFileWriter.seek(0);
                roomFileWriter.setLength(0);
                roomFileWriter.writeBytes(changedContent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(roomLock != null){
                roomLock.release();
            }
            if(roomChannel != null){
                roomChannel.close();
            }
            if(roomFileWriter != null){
                roomFileWriter.close();
            }
        }
        //If the room didn't exist we also need to make the roomMessages for that room
        if(!roomMessagesFile.exists()){
            createRoom(roomName);
        }

        //Work through each user adding room to their rooms
        for(String username: usernames){
            addRoomToUser(username, roomName);
        }
    }



    /**
     * Adds the roomName given to the users rooms. If the user does not exist they are created with that room name.
     * DO NOT USE FOR UPDATING ROOMS THAT USER BELONGS TO: USE {@link #addUsersToRoom(String, List)} INSTEAD!
     * @param username name of user to have room added or created
     * @param roomName name of room to add to the user
     * @throws IOException
     * @author Robbie Booth
     */
    private static void addRoomToUser(String username, String roomName) throws IOException {
        File file = new File(usersPathDir.toFile(), username+".json");
        History.checkPathElseCreate(usersPathDir);

        RandomAccessFile fileWriter = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {

            // Open the file in read-write mode
            fileWriter = new RandomAccessFile(file, "rw");
            channel = fileWriter.getChannel();

            // Acquire an exclusive lock on the file
            //lock = channel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

            // Read the content:
            // Set the file pointer to the beginning of the file to overwrite its content
            fileWriter.seek(0);
            long fileLength = fileWriter.length();
            byte[] content = new byte[(int) fileLength];
            fileWriter.readFully(content);

            // Convert the byte array to a String
            String jsonString = new String(content);
            ObjectMapper objectMapper = new ObjectMapper();

            HashSet<String> usersRooms = new HashSet<>();//add all the new users to the set
            usersRooms.add(roomName);//add the new room

            //then get the ones currently in it and add them as well
            String changedContent = null;
            if(!jsonString.isBlank()) {
                try {
                    JsonNode rootNode = objectMapper.readTree(jsonString);
                    JsonNode roomsNode = rootNode.get("rooms");
                    if (roomsNode.isArray()) {
                        for (JsonNode roomNode : roomsNode) {
                            usersRooms.add(roomNode.asText());
                        }
                    }
                    ArrayNode newRoomsNode = objectMapper.valueToTree(usersRooms);
                    ((ObjectNode) rootNode).set("rooms", newRoomsNode);
                    changedContent = objectMapper.writeValueAsString(rootNode);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }else{
                //no json string problem
                String usersRoomsString = objectMapper.writeValueAsString(usersRooms);
                changedContent = getUserString(username, usersRoomsString, "[]", "[]", "[]");
            }
            if(changedContent != null){
                //write new history to file
                fileWriter.seek(0);
                fileWriter.setLength(0);
                fileWriter.writeBytes(changedContent);
            }



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
     * Adds a message to the history file of that room. If room doesn't exist it is created.
     * If history is more than {@link #maxHistorySize} then oldest message in history file is removed and replaced with message given.
     * @param message message to be added to the rooms history
     * @param roomName room to add the message to
     * @throws IOException
     *
     * @author Robbie Booth
     */
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

            //remove lock so we can show error for history
            //lock = channel.lock(0, Long.MAX_VALUE, false);//We acquire an exclusive lock to write to the document

            // Read the content:
            // Set the file pointer to the beginning of the file to overwrite its content
            fileWriter.seek(0);
            long fileLength = fileWriter.length();
            byte[] content = new byte[(int) fileLength];
            fileWriter.readFully(content);

            // Convert the byte array to a String
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
                Collections.sort(userMessages);//Sort the messages by timestamp
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

    /**
     * Reads all the rooms that are contained in the rooms' directory. Returns empty list if rooms directory doesn't exist.
     * @return list of names of rooms
     * @author Robbie Booth
     */
    public static Collection<String> readAllRoomNames() {
        Set<String> rooms = new HashSet<>();

        //Path doesn't exist so return empty list
        if(!Files.isDirectory(roomsPathDir)){
            return rooms;
        }

        DirectoryStream<Path> directoryStream = null;
        try {
            directoryStream = Files.newDirectoryStream(roomsPathDir);

            for (Path path : directoryStream) {
                //Check if the current path is a directory
                if (Files.isDirectory(path)) {
                    rooms.add(path.getFileName().toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try{
                if(directoryStream != null){
                    directoryStream.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }

        }
        return rooms;
    }
}