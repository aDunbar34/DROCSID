import java.net.*;
import java.io.*;

/// The Server class listens for a connection
/// and then allows for the sending of messages
/// back and forth with the client
public class Server {

    public static void main(String[] args) {

        // Get port number from command line arguments
        int portNumber = Integer.parseInt(args[0]);

        // Set up a new server socket on the port number
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Awaiting connection...");

            // Create a new connection socket when a connection is established
            Socket connectionSocket = serverSocket.accept();

            System.out.println("Connection established.");

            // Set up writers and readers for UTF-8
            PrintWriter out = new PrintWriter(connectionSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            // The basic loop - Wait for a message from the client,
            // print the message, get a response from stdin,
            // and send the response to the client
            String receivedMessage;
            while (true) {
                receivedMessage = in.readLine();
                System.out.println("pal> " + receivedMessage);
                System.out.print("you> ");
                String message = stdIn.readLine();
                out.println(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
