import java.net.*;

public class Client {

    public static void main(String[] args) {

        System.out.println("""
                ██████╗ ██████╗  ██████╗  ██████╗███████╗██╗██████╗
                ██╔══██╗██╔══██╗██╔═══██╗██╔════╝██╔════╝██║██╔══██╗
                ██║  ██║██████╔╝██║   ██║██║     ███████╗██║██║  ██║
                ██║  ██║██╔══██╗██║   ██║██║     ╚════██║██║██║  ██║
                ██████╔╝██║  ██║╚██████╔╝╚██████╗███████║██║██████╔╝
                ╚═════╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝╚══════╝╚═╝╚═════╝
                    """);
        System.out.println("You are now experiencing DROCSID");


        String hostname = args[0];
        int portNo = Integer.parseInt(args[1]);

        System.out.println("Attempting to connect to client '" + hostname + "' on port " + portNo + "...");
        try (Socket socket = new Socket(hostname, portNo)) {
            System.out.println("Connection established! Enjoy your chat!");
            chatLoop(socket);
        } catch (Exception e) {
            e.printStackTrace();
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
