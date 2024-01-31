import java.net.*;
import java.io.*;

public class Server {

    public static void main(String[] args) {

        int portNumber = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Awaiting connection...");

            Socket connectionSocket = serverSocket.accept();

            System.out.println("Connection established.");

            PrintWriter out = new PrintWriter(connectionSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            String receivedMessage;
            while ((receivedMessage = in.readLine()) != null) {
                System.out.println("Received: '" + receivedMessage + "'");
                try {
                    int number = Integer.parseInt(receivedMessage.trim());
                    number *= 2;
                    out.println(number);
                } catch (NumberFormatException e) {
                    out.println(receivedMessage.toUpperCase());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
