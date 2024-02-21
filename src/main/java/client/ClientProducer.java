package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.Message;
import messageCommunication.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Initialises the connection with the server sets the room and then any messages that the client sends will be sent to
 * the server socket given {@link #socket}.
 *
 * @author Robbie Booth
 */
public class ClientProducer implements Runnable {
    private final Socket socket;
    private final String username;

    private String chatRoomId;
    private final BufferedReader stdIn;
    PrintWriter out;

    ObjectMapper objectMapper = new ObjectMapper();

    public ClientProducer(Socket socket, String username, String chatRoomId) {
        this.socket = socket;
        this.username = username;
        this.chatRoomId = chatRoomId;
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
            initialisationMessage = new Message(0, MessageType.SELECT_ROOM, username, "test", System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(initialisationMessage));

            // Reads string from client and sends it back to the client String inputLine;
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
                default -> System.out.println("Unrecognized command: '" + commandArgs[0] + "'.");

            }
        } else { // Treat input as message
            sendTextMessage(userInput);
        }
    }

    /**
     * Prepares and sends a text message to the server.
     *
     * @param userInput The input to be sent as a message.
     *
     * @author Robbie Booth
     */
    private void sendTextMessage(String userInput) {
        Message message_to_send = new Message(0, MessageType.TEXT, username, chatRoomId, System.currentTimeMillis(), userInput);//make message
        try {
            out.println(objectMapper.writeValueAsString(message_to_send));//send message to server
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}