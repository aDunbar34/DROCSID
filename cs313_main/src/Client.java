import java.net.*;
import java.io.*;

/// The client class makes a connection to the server
/// and allows for sending and receiving messages
public class Client {

    public static void main(String[] args) {

        // Get hostname and port number of the server form the command line
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        // Try to establish a connection to the server
        try (Socket clientSocket = new Socket(hostName, portNumber)) {

            System.out.println("Connection established.");

            // Set up writers and readers for UTF-8
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            // The basic loop - Get a message from stdin,
            // wait for a response from the server,
            // print response, repeat
            while (true) {
                System.out.print("you> ");

                String message;
                message = stdIn.readLine();
                    out.println(message);
                    String response = in.readLine();
                    System.out.println("buddy> " + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
