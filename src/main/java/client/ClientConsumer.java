package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import customExceptions.InvalidMessageException;
import messageCommunication.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

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
                    case FILE_RECEIVE_SIGNAL -> handleFileReceiveSignalMessage(messageParsed);
                    case ONLINE_STATUSES -> handleOnlineStatuses(messageParsed);
                }
            }
        } catch (IOException | InvalidMessageException e) {
            throw new RuntimeException(e);
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
