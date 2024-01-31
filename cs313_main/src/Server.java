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
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            String receivedMessage;
            while (true) {
                receivedMessage = in.readLine();
                System.out.print("you> ");
                String message;
                while ((message = stdIn.readLine()) != null) {
                    out.println(message);
                    System.out.println("you> " + message);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
