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

                System.out.println(messageParsed.getSenderId()+"> " + messageParsed.getTextMessage());
            }
        } catch (IOException | InvalidMessageException e) {
            throw new RuntimeException(e);
        }
    }
}
