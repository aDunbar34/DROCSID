package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.Message;
import messageCommunication.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientProducer implements Runnable {
    private Socket socket = null;
    private String username = null;

    private String chatRoomId = null;
    ObjectMapper objectMapper = new ObjectMapper();

    public ClientProducer(Socket socket, String username, String chatRoomId) {
        this.socket = socket;
        this.username = username;
        this.chatRoomId = chatRoomId;
    }

    public void run() {
        try {
            //Initialise

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Message initialisationMessage = new Message(0, MessageType.INITIALISATION, username, (String) null, System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(initialisationMessage));

            initialisationMessage = new Message(0, MessageType.SELECT_ROOM, username, "test", System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(initialisationMessage));
            // Reads string from client and sends it back to the client String inputLine;
            System.out.println("Enter a message:");
            String message;
            while ((message = stdIn.readLine()) != null) {
                Message message_to_send = new Message(0, MessageType.TEXT, username, chatRoomId, System.currentTimeMillis(), message);//make message
                out.println(objectMapper.writeValueAsString(message_to_send));//send message to server
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}