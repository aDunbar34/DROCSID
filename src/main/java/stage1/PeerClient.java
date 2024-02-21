package stage1;

import java.net.*;

public class PeerClient {

    public static void main(String[] args) {

        System.out.println("""
                ██████╗ ██████╗  ██████╗  ██████╗███████╗██╗██████╗
                ██╔══██╗██╔══██╗██╔═══██╗██╔════╝██╔════╝██║██╔══██╗
                ██║  ██║██████╔╝██║   ██║██║     ███████╗██║██║  ██║
                ██║  ██║██╔══██╗██║   ██║██║     ╚════██║██║██║  ██║
                ██████╔╝██║  ██║╚██████╔╝╚██████╗███████║██║██████╔╝
                ╚═════╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝╚══════╝╚═╝╚═════╝
                    """);
        System.out.println("You are now experiencing DROCSID in PEER MODE");

        // Determine whether to listen or connect based on the number of command line
        // arguments given
        if (args.length >= 2) {
            String hostname = args[0];
            int portNo = Integer.parseInt(args[1]);

            System.out.println("Attempting to connect to client '" + hostname + "' on port " + portNo + "...");
            try (Socket socket = new Socket(hostname, portNo)) {
                System.out.println("Connection established! Enjoy your chat!");
                chatLoop(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (args.length == 1) {
            int portNo = Integer.parseInt(args[0]);

            try (ServerSocket serverSocket = new ServerSocket(portNo)) {
                System.out.println("Listening for connection on port " + portNo + "...");
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("Connection established! Enjoy your chat!");
                    chatLoop(socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // Sets up threads and runs the primary chat loop
    public static void chatLoop(Socket socket) {
        PeerInput peerInput = new PeerInput(socket);
        PeerOutput peerOutput = new PeerOutput(socket);

        Thread inputThread = new Thread(peerInput);
        Thread outputThread = new Thread(peerOutput);

        inputThread.start();
        outputThread.start();

        while (inputThread.isAlive() && outputThread.isAlive()) {
        }
    }

}
