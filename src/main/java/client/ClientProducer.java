package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.Message;
import messageCommunication.MessageType;
import server.NonBlockingServerProducer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.*;

/**
 * Initialises the connection with the server sets the room and then any messages that the client sends will be sent to
 * the server socket given {@link #socket}.
 *
 * @author Robbie Booth
 */
public class ClientProducer implements Runnable {
    private final Socket socket;
    private final String username;

    private ChatRoomData chatRoomData;
    private final BufferedReader stdIn;
    PrintWriter out;


    ObjectMapper objectMapper = new ObjectMapper();

    public ClientProducer(Socket socket, String username, ChatRoomData chatRoomData) {
        this.socket = socket;
        this.username = username;
        this.chatRoomData = chatRoomData;
        stdIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            out = new PrintWriter(this.socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void run() {
        try {
            //Initialise
            Message initialisationMessage = new Message(0, MessageType.INITIALISATION, username, (String) null, System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(initialisationMessage));


            //Select room
            //In future this will allow room choosing
//            initialisationMessage = new Message(0, MessageType.SELECT_ROOM, username, "test", System.currentTimeMillis(), "");
//            out.println(objectMapper.writeValueAsString(initialisationMessage));

            // Reads string from client and sends it back to the client String inputLine;
            displayHomePage();
            System.out.println("Enter a message:");
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                parseInput(userInput);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Parses the input for special commands and calls the relevant command function.
     * Input is treated as a message and if no command found
     *
     * @param userInput The input to be parsed.
     *
     * @author Euan Gilmour
     */
    public void parseInput(String userInput) {

        // Check for empty string
        if (userInput.isEmpty()) {
            return;
        }

        // Check for special command
        if (userInput.charAt(0) == '\\') {
            String[] commandArgs = userInput.split(" ");

            switch (commandArgs[0]) {
                case "\\files" -> showFiles();
                case "\\view" -> viewImage(commandArgs);
                case "\\play" -> playVideo(commandArgs);
                case "\\sendFile" -> sendFile(commandArgs);
                case "\\join" -> joinRoom(commandArgs);
                case "\\create" -> createRoom(commandArgs);
                case "\\add" -> addMembersToRoom(commandArgs);
                case "\\exit" -> exitRoom();
                case "\\online" -> showOnline();
                case "\\friends" -> showFriends();
                case "\\sendRequest" -> handleSendRequests(commandArgs);
                case "\\accept" -> handleAcceptRequest(commandArgs);
                default -> System.out.println("Unrecognized command: '" + commandArgs[0] + "'.");
            }
        } else { // Treat input as message
            if(userInput.toLowerCase().equals("disconnect")) {
                //disconnect

            }
            synchronized (chatRoomData) {
                if (chatRoomData.getChatRoomId() == null) {
                    parseHomePage(userInput);
                } else {
                    sendTextMessage(userInput);
                }
            }
        }
    }

    /**
     * Accepts a friend request from another user
     *
     * @param args The input command \accept <Username>
     * @author Lewis Brogan
     */

    private void handleAcceptRequest(String[] args) {
        if (args.length <= 1) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\accept <member>");
            return;
        }

        String potentialFriend = args[1];

        try{
            //Send message to add members to room
            Message toServer = new Message(0, MessageType.ACCEPT_FRIEND, username, chatRoomData.getChatRoomId(), System.currentTimeMillis(), potentialFriend);
            out.println(objectMapper.writeValueAsString(toServer));
        } catch (JsonProcessingException e) {
            System.out.println("Error: Friend Request could not be parsed");
        }
    }


    /**
     * Sends a Friend Request to the input user
     *
     * @param args The input command \sendRequest <Username>
     * @author Adam Dunbar
     */

    private void handleSendRequests(String[] args) {
        // Check for incorrect number of arguments
        if (args.length <= 1) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\sendRequest <member>");
            return;
        }
        // Get name of user to be sent request
        String potentialFriend = args[1];

        //should error on server if user not in room
        try{
            //Send message to add members to room
            Message toServer = new Message(0, MessageType.SEND_FRIEND_REQUEST, username, chatRoomData.getChatRoomId(), System.currentTimeMillis(), potentialFriend);
            out.println(objectMapper.writeValueAsString(toServer));
        } catch (JsonProcessingException e) {
            System.out.println("Error: Friend Request could not be parsed");
        }
    }

    /**
     * Displays a list of the all users in the Clients friend list
     *
     * @author Adam Dunbar
     */
    private void showFriends() {
        Message toServer = new Message(0, MessageType.FRIENDS_LIST, username, chatRoomData.getChatRoomId(), System.currentTimeMillis());
        try {
            out.println(objectMapper.writeValueAsString(toServer));
        } catch (JsonProcessingException e) {
            System.out.print("Error: Showing friend list could not be parsed");
        }
    }


    /**
     * Sends a request to server to add members to the room given
     * @param args \add &lt;roomName&gt; &lt;member&gt; - no limit to members given
     * @author Robbie Booth
     */
    private void addMembersToRoom(String[] args) {
        // Check for incorrect number of arguments
        if (args.length <= 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\add <roomName> <member>");
            return;
        }

        // Get room name
        String roomName = args[1];
        HashSet<String> members = new HashSet<>();
        for (int i = 2; i < args.length; i++) {
            members.add(args[i]);
        }

        //should error on server if user not in room
        try{
            //Send message to add members to room
            Message addMembersMessage = new Message(0, MessageType.ADD_MEMBERS, username, roomName, System.currentTimeMillis(), objectMapper.writeValueAsString(members.toArray()));
            out.println(objectMapper.writeValueAsString(addMembersMessage));
        } catch (JsonProcessingException e) {
            System.out.println("Error: add members to room could not be parsed");
        }
    }

    /**
     * Sends a request to server to create a room of name provided with members given
     * @param args \create &lt;roomName&gt; &lt;member&gt; - no limit to members given
     * @author Robbie Booth
     */
    private void createRoom(String[] args) {
        // Check for incorrect number of arguments
        if (args.length < 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\create <roomName> <member>");
            return;
        }

        // Get room name
        String roomName = args[1];
        HashSet<String> members = new HashSet<>();
        for (int i = 2; i < args.length; i++) {
            members.add(args[i]);
        }
        members.add(this.username);//add in the user connected if not already added by themselves

        try{
            //Send message to create room
            Message joinRoomMessage = new Message(0, MessageType.CREATE_ROOM, username, roomName, System.currentTimeMillis(), objectMapper.writeValueAsString(members.toArray()));
            out.println(objectMapper.writeValueAsString(joinRoomMessage));
        } catch (JsonProcessingException e) {
            System.out.println("Error: create room could not be parsed");
        }
    }

    /**
     * Sends a request to join the room specified in the second arg
     * @param args \join &lt;roomName&gt;
     * @author Robbie Booth
     */
    private void joinRoom(String[] args){
        // Check for incorrect number of arguments
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\join <roomName>");
            return;
        }

        // Get room name
        String roomName = args[1];

        try{
            //Send message to join room
            Message joinRoomMessage = new Message(0, MessageType.SELECT_ROOM, username, roomName, System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(joinRoomMessage));
        } catch (JsonProcessingException e) {
            System.out.println("Error: Room could not be parsed");
        }
    }

    /**
     * Parses the options that can be selected for the home page.
     *
     * @param option the value associated with the home page selections/options
     * @author Robbie Booth
     */
    private void parseHomePage(String option){
        try{
            Integer choice = Integer.parseInt(option);
            switch(choice){
                case 1 ->{
                    System.out.println("You've selected join existing room");
                    //ask server for users rooms then display them (consumer needs to display)
                    requestRooms();
                    System.out.println("To join a room type \\join <RoomName>");
                }
                case 2 ->{
                    System.out.println("You've selected create a room");
                    //look for input from user for selecting a room
                    System.out.println("To create a room enter \\create <roomName>");
                    System.out.println("Then <username> space, for each user in room");
                    System.out.println("E.g: for six users \\create TheCrew martin adam robbie lewis max euan");
                }
                case 3 ->{
                    System.out.println("You've selected add user to room");
                    //display info on adding user to room
                    System.out.println("To add members to a room enter \\add <roomName>");
                    System.out.println("Then <username> space, for each user to be added to room");
                    System.out.println("E.g: for adding two users \\add TheCrew fred scooby");
                }
                case 4 ->{
                    System.out.println("You've selected to view your friend requests");
                    viewFriendRequests();
                    System.out.println("To send a Friend Request do \\sendRequest <Username>");
                    System.out.println("To Accept a Friend Request do \\accept <Username>");
                }
                case 5 ->{
                    System.out.println("You've selected disconnect");
                    System.out.println("NOT IMPLEMENTED YET");//TODO implement
                    //disconnect
                }
                default -> System.out.println("Unrecognized option!");
            }
        }catch (NumberFormatException e){
            System.out.println("Unrecognized option!");
        }
    }

    private void viewFriendRequests() {
        Message toServer = new Message(0, MessageType.FRIEND_REQUEST_LIST, username, chatRoomData.getChatRoomId(), System.currentTimeMillis());
        try {
            out.println(objectMapper.writeValueAsString(toServer));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Terrible way of doing it but displays the possible options on home page
     *
     * @author Robbie Booth
     */
    public static void displayHomePage(){
        System.out.println("Entering Home Page:");
        System.out.println("1. Join existing room");
        System.out.println("2. Create room");
        System.out.println("3. Add user to room");
        System.out.println("4. View friend requests");
        System.out.println("5. Disconnect");
        System.out.println("Enter number to select option:");
    }


    /**
     * Sends a request to the server for the users rooms
     * @author Robbie Booth
     */
    private void requestRooms(){
        Message message_to_send = new Message(0, MessageType.ROOMS, username, (String) null, System.currentTimeMillis(), "");//make message
        try {
            out.println(objectMapper.writeValueAsString(message_to_send));//send message to server
        } catch (JsonProcessingException e) {
            System.out.println("Error creating rooms request!");
        }
    }

    /**
     * Sends a request to the server for the user to leave the room they currently are in.
     * @author Robbie Booth
     */
    private void exitRoom(){
        try{
            //Send message to join room
            Message leaveRoomMessage = new Message(0, MessageType.SELECT_ROOM, username, null, System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(leaveRoomMessage));
        } catch (JsonProcessingException e) {
            System.out.println("Error: Leaving room - could not be parsed");
        }
    }


    /**
     * Displays a list of the currently online users in the chatroom
     *
     * @author Adam Dunbar
     */
    public void showOnline() {
        Message toServer = new Message(0, MessageType.ONLINE_STATUSES, username, chatRoomData.getChatRoomId(), System.currentTimeMillis());//make message
        try {
            out.println(objectMapper.writeValueAsString(toServer)); //send message to server
        } catch (JsonProcessingException e) {
            System.out.println("Error: Showing online members could not be parsed");
        }

        return;
    }

    /**
     * Prints a list of files in the drocsidFiles directory.
     * If no such directory exists, it creates it.
     */
    private void showFiles() {

        // Construct path to files directory
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");

        // Create directory if it does not exist
        if (!Files.exists(filesDirectory)) {
            try {
                Files.createDirectory(filesDirectory);
            } catch (IOException e) {
                System.out.println("ERROR: Something went wrong while trying to create files directory");
            }
        }

        // Display filenames
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(filesDirectory)) {
            Iterator<Path> iterator = directoryStream.iterator();
            if (iterator.hasNext()) {
                System.out.println("Files:");
                while (iterator.hasNext()) {
                    Path filePath = iterator.next();
                    System.out.println(filePath.getFileName().toString());
                }
            } else {
                System.out.println("No files!");
            }
        } catch (IOException e) {
            System.out.println("ERROR: Something went wrong while trying to view the files directory");
        }

    }

    /**
     * Prepares and displays an image via ImageViewer
     *
     * @param args the args the command was called with
     *
     * @author Euan Gilmour
     */
    private void viewImage(String[] args) {

        // Check for incorrect number of arguments
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\view <filename>");
            return;
        }

        // Get filename
        String fileName = args[1];

        // Verify that file exists
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");
        Path filePath = filesDirectory.resolve(fileName);
        if (!(Files.exists(filePath) && Files.isRegularFile(filePath))) {
            System.out.println("Error trying to view file '" + fileName + "': no such file exists.");
            return;
        }

        // Verify that file is a valid image file
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff"};
        if (Arrays.stream(imageExtensions).noneMatch(fileName::endsWith)) {
            System.out.println("Error trying to view file '" + fileName + "': file is not a valid image file");
            return;
        }

        // Set up ImageViewer and view image
        ImageViewer imageViewer = new ImageViewer();
        imageViewer.viewImage(filePath.toString());

    }

    /**
     * Prepares and plays a video via VideoPlayer
     *
     * @param args the args the command was called with
     *
     * @author Euan Gilmour
     */
    private void playVideo(String[] args) {

        // Check for incorrect number of arguments
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\play <filename>");
            return;
        }

        // Get filename
        String fileName = args[1];

        // Verify that file exists
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");
        Path filePath = filesDirectory.resolve(fileName);
        if (!(Files.exists(filePath) && Files.isRegularFile(filePath))) {
            System.out.println("Error trying to play file '" + fileName + "': no such file exists.");
            return;
        }

        // Verify that file is a valid video file
        String[] videoExtensions = {".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mpeg", ".mpg"};
        if (Arrays.stream(videoExtensions).noneMatch(fileName::endsWith)) {
            System.out.println("Error trying to play file '" + fileName + "': file is not a valid video file");
            return;
        }

        // Set up VideoPlayer and play video
        VideoPlayer videoPlayer = new VideoPlayer();
        videoPlayer.playVideo(filePath.toString());

    }

    /**
     * Validates file to send and sends new
     * FILE_SEND_SIGNAL to the server
     *
     * @param args The args the command was called with.
     *
     * @author Euan Gilmour
     */
    private void sendFile(String[] args) {

        // Check for incorrect number of arguments
        if (args.length != 4) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\sendFile <pathToFile> <recipientUsername> <portNo>");
            return;
        }

        // Unpack args
        String pathString = args[1];
        String recipient = args[2];
        int portNo;
        try {
            portNo = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Port must be a number.");
            System.out.println("USAGE: \\sendFile <pathToFile> <recipientUsername> <portNo>");
            return;
        }

        // Verify that file exists
        Path filePath = Paths.get(pathString);
        if (!(Files.exists(filePath) && Files.isRegularFile(filePath))) {
            System.out.println("ERROR: The given path is incorrect or file does not exist.");
            System.out.println("USAGE: \\sendFile <pathToFile> <recipientUsername> <portNo>");
            return;
        }

        // Verify that file is under the size limit
        int sizeLimit = 104857600; // 100 MB in bytes
        try {
            if (Files.size(filePath) > sizeLimit) {
                System.out.println("ERROR: The file is over the size limit of 100MB");
                return;
            }
        } catch (IOException e) {
            System.out.println("ERROR: something went wrong while assessing the size of the file.");
            return;
        }

        // Prepare and send new FILE_SEND_SIGNAL Message to the server
        String fileName = filePath.getFileName().toString();
        String payload = fileName + ',' + pathString + ',' + recipient + ',' + portNo;
        synchronized (chatRoomData) {
            Message fileListenSignalMessage = new Message(0, MessageType.FILE_SEND_SIGNAL, username, chatRoomData.getChatRoomId(), System.currentTimeMillis(), payload);
            try {
                out.println(objectMapper.writeValueAsString(fileListenSignalMessage));
            } catch (JsonProcessingException e) {
                System.out.println("ERROR: Something went wrong while trying to send a FILE_SEND_SIGNAL message to the server");
                return;
            }
        }

        System.out.println("Signalling file transfer to server...");

    }

    /**
     * Prepares and sends a text message to the server.
     *
     * @param userInput The input to be sent as a message.
     *
     * @author Robbie Booth
     */
    private void sendTextMessage(String userInput) {
        synchronized (chatRoomData) {
            Message message_to_send = new Message(0, MessageType.TEXT, username, chatRoomData.getChatRoomId(), System.currentTimeMillis(), userInput);//make message
            try {
                out.println(objectMapper.writeValueAsString(message_to_send));//send message to server
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}