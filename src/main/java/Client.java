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
        String username = args[2]; //used to uniquely identify user

        System.out.println("Attempting to connect to client '" + hostname + "' on port " + portNo + "...");
        try (Socket socket = new Socket(hostname, portNo)) {
            System.out.println("Connection established! Enjoy your chat!");
            chatLoop(socket, username);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Sets up threads and runs the primary chat loop
    public static void chatLoop(Socket socket, String username) {
        ClientSender peerInput = new ClientSender(socket, username, "test");
        ClientOutputMessage peerOutput = new ClientOutputMessage(socket);

        Thread inputThread = new Thread(peerInput);
        Thread outputThread = new Thread(peerOutput);

        inputThread.start();
        outputThread.start();

        while (inputThread.isAlive() && outputThread.isAlive()) {
        }
    }

}
