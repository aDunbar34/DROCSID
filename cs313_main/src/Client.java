import java.net.*;
import java.io.*;

public class Client {

    public static void main(String[] args) {

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (Socket clientSocket = new Socket(hostName, portNumber)) {

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Input a message to send to the server.");

            String message;
            while ((message = stdIn.readLine()) != null) {
                out.println(message);
                System.out.println("Response: '" + in.readLine() + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
