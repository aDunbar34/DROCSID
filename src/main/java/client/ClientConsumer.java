package client;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    public ClientConsumer(Socket socket) {
        this.socket = socket;
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
                    case TEXT -> handleTextMessage(messageParsed);
                    case FILE_RECEIVE_SIGNAL -> handleFileReceiveSignalMessage(messageParsed);
                }
            }
        } catch (IOException | InvalidMessageException e) {
            throw new RuntimeException(e);
        }
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
