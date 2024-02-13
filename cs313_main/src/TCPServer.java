import java.net.*;
import java.io.*;

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
            PeerInput peerInput = new PeerInput(clientSocket);
            //Create way of outputting that is capable of threading
            PeerOutput peerOutput = new PeerOutput(clientSocket);
            //Create the threads
            Thread outputThread = new Thread(peerOutput);
            Thread inputThread = new Thread(peerInput);
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
