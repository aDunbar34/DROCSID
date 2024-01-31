import java.net.*;
import java.io.*;
import java.util.regex.Pattern;

//java EchoServer 7
public class TCPServer {
    public static void main(String[] args) {
        // Read port number from command line
        int portNumber = Integer.parseInt(args[0]);

        try {
            // Create a socket server. Need to say what port this server is listening on.
            ServerSocket serverSocket = new ServerSocket(portNumber);
            // Accept a connection from a client
            Socket clientSocket = serverSocket.accept();
            //Create way of inputting that is capable of threading
            InputRunner inputRunner = new InputRunner(clientSocket);
            //Create way of outputting that is capable of threading
            OutputRunner outputRunner = new OutputRunner(clientSocket);
            //Create the threads
            Thread outputThread = new Thread(outputRunner);
            Thread inputThread = new Thread(inputRunner);
            //start the threads
            outputThread.start();
            inputThread.start();
            //continue whilst the threads are alive/keep program alive
            while(outputThread.isAlive() && inputThread.isAlive()) {

            }
        } catch (IOException ioe) {
            System.err.println("1/0 error");
            ioe.printStackTrace();
            System.exit(1);
        }
    }
}
