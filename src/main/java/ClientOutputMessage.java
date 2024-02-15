import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientOutputMessage implements Runnable {
    private Socket socket = null;
    private ObjectMapper objectMapper = new ObjectMapper();
    public ClientOutputMessage(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
//        try {
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            String inputLine;
//            System.out.println("Waiting for input");
//            while ((inputLine = in.readLine()) != null) {
//                System.out.println("DROCSID user> " + inputLine);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
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
