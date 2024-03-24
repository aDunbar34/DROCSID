package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import customExceptions.InvalidMessageException;
import messageCommunication.Message;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Listens to the servers socket {@link #socket} and outputs the appropriate message to the users terminal if message received
 *
 * @author Robbie Booth
 * @see Message
 */
public class ClientConsumer implements Runnable {
    private Socket socket = null;
    private ObjectMapper objectMapper = new ObjectMapper();

    private ChatRoomData chatRoomData;

    public ClientConsumer(Socket socket, ChatRoomData chatRoomData) {
        this.socket = socket;
        this.chatRoomData = chatRoomData;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            System.out.println("Waiting for input");

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                Message messageParsed;
                try{
                    String receivedData = new String(buffer, 0, bytesRead);
                    messageParsed = objectMapper.readValue(receivedData, Message.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    throw new InvalidMessageException("Message being parsed is invalid");
                }

                // Route message to appropriate handler function
                switch (messageParsed.getType()) {
                    case SELECT_ROOM -> handleRoomSelection(messageParsed);
                    case ROOMS -> handleRooms(messageParsed);
                    case CREATE_ROOM -> handleCreateRoom(messageParsed);
                    case ADD_MEMBERS -> handleAddMembersToRoom(messageParsed);
                    case TEXT -> handleTextMessage(messageParsed);
                    case FILE_LISTEN_SIGNAL -> handleFileListenSignalMessage(messageParsed);
                    case FILE_RECEIVE_SIGNAL -> handleFileReceiveSignalMessage(messageParsed);
                    case ONLINE_STATUSES -> handleOnlineStatuses(messageParsed);
                    case STREAM_SIGNAL -> handleStreamSignal(messageParsed);
                }
            }
        } catch (IOException | InvalidMessageException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets up web app for streaming
     *
     * @param message the message
     *
     * @author Euan Gilmour
     */
    private void handleStreamSignal(Message message) {

        String username = message.getTextMessage();
        String initiator = message.getSenderId();

        System.out.println("User <" + initiator + "> is trying to stream to you");

        String uri = "http://localhost:8080?recipient," + socket.getInetAddress().getHostAddress() + "," + username + "," + initiator;

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(uri));
            } else {
                System.out.println("Browse action unsupported. Please open the following URL in your browser to stream with <" + initiator + ">: " + uri);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the list of users currently online
     *
     * @param message
     *
     * @author Adam Dunbar
     */
    public void handleOnlineStatuses(Message message) {
        System.out.println(message.getTextMessage());
    }

    /**
     * Handles servers response of selecting room and updates the clients local information
     * @param messageParsed message given from server
     * @author Robbie Booth
     */
    private void handleRoomSelection(Message messageParsed) {
        if(!(messageParsed.getTextMessage().equals("success"))){
            //room not successfully joined
            System.out.println(messageParsed.getTextMessage());
            return;
        }

        synchronized (chatRoomData){
            String newRoom = messageParsed.getTargetId();
            chatRoomData.setChatRoomId(newRoom);
        }

        if(messageParsed.getTargetId() == null){
            System.out.println("Room successfully left!");
            ClientProducer.displayHomePage();//Display home page options
        }else{
            System.out.println("Room changed to: "+messageParsed.getTargetId());
        }
    }


    /**
     * Handles servers response of getting all rooms and displays them
     * @param messageParsed message given from server
     * @author Robbie Booth
     */
    private void handleRooms(Message messageParsed){
        try{
            String[] usersRooms = objectMapper.readValue(messageParsed.getPayload(), String[].class);
            if(usersRooms.length == 0){
                System.out.println("No rooms found!");
                return;
            }

            System.out.println("Your rooms:");
            for(String room: usersRooms){
                System.out.println(room);
            }
        } catch (StreamReadException e) {
            System.out.println("Error: couldn't read rooms!");
        } catch (DatabindException e) {
            System.out.println("Error: couldn't read rooms!");
        } catch (IOException e) {
            System.out.println("Error: couldn't read rooms!");
        }

    }

    /**
     * Handles servers response of creating room
     * @param messageParsed message given from server
     * @author Robbie Booth
     */
    private void handleCreateRoom(Message messageParsed){
        System.out.println(messageParsed.getTextMessage());
    }

    /**
     * Handles servers response of adding members to a room
     * @param messageParsed message given from server
     * @author Robbie Booth
     */
    private void handleAddMembersToRoom(Message messageParsed){
        System.out.println(messageParsed.getTextMessage());
    }

    /**
     * Prints the text content of the message
     *
     * @param message the message
     *
     * @author Robbie Booth
     */
    private void handleTextMessage(Message message) {
        System.out.println(message.getSenderId()+"> " + message.getTextMessage());
    }

    /**
     * Parses the payload of the necessary data and
     * prepares new FileSender thread
     *
     * @param message The message
     *
     * @author Euan Gilmour
     */
    private void handleFileListenSignalMessage(Message message) {

        String[] payloadParts = message.getTextMessage().split(",");
        String filePath = payloadParts[0];
        String recipientUsername = payloadParts[1];
        String receiverHost = payloadParts[2];
        int portNo = Integer.parseInt(payloadParts[3]);
        File file = new File(filePath);

        // Start up a new FileSender thread
        Thread fileSender = new Thread(new FileSender(file, portNo, recipientUsername, receiverHost));
        fileSender.start();

    }

    /**
     * Parses the payload of the message for necessary data and
     * prepares new FileReceiver thread
     *
     * @param message the message
     *
     * @author Euan Gilmour
     */
    private void handleFileReceiveSignalMessage(Message message) {

        String senderUsername = message.getSenderId();

        // Parse message payload for file transfer parameters
        String messageContent = message.getTextMessage();
        String[] fileTransferDetails = messageContent.split(",");
        String senderHost = fileTransferDetails[0];
        int portNo = Integer.parseInt(fileTransferDetails[1]);
        String fileName = fileTransferDetails[2];

        // Prepare and start a new FileReceiver thread
        Thread fileReceiver = new Thread(new FileReceiver(senderUsername, senderHost, portNo, fileName));
        fileReceiver.start();

    }
}
