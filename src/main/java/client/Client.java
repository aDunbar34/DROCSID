package client;

import java.net.*;

/**
 *Main Client used for communication to the drocsid server
 *
 * @author Euan Gilmour
 * @author Robbie Booth
 */
public class Client {


    /**
     * Starts up the Drocsid client and begins communication with the server hostname and port number given.<br>
     * The username argument given is the users unique username
     * @param args the hostname of the server, the port number of the server, the unique username
     *
     * @author Euan Gilmour
     * @author Robbie Booth
     */
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

        // Set up web server
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        ClientWebServer clientWebServer = new ClientWebServer();
        clientWebServer.startServer();

        String hostname = args[0];//ip address of server or localhost
        int portNo = Integer.parseInt(args[1]);
        String username = args[2]; //used to uniquely identify user

        System.out.println("Attempting to connect to server '" + hostname + "' on port " + portNo + "...");
        try (Socket socket = new Socket(hostname, portNo)) {
            System.out.println("Connection established! Enjoy your chat!");
            chatLoop(socket, username);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * The chat loop that starts up two threads.
     * <br>
     * One for sending messages to the server
     * <br>
     * Another for receiving messages from the server
     * <br>
     * @param socket the server socket connection
     * @param username the unique username that the client is using
     *
     * @author Euan Gilmour
     * @author Robbie Booth
     */
    public static void chatLoop(Socket socket, String username) {
        ChatRoomData chatRoom = new ChatRoomData(null);

        ClientProducer peerInput = new ClientProducer(socket, username,  chatRoom);
        ClientConsumer peerOutput = new ClientConsumer(socket, chatRoom);

        Thread inputThread = new Thread(peerInput);
        Thread outputThread = new Thread(peerOutput);

        inputThread.start();
        outputThread.start();

        while (inputThread.isAlive() && outputThread.isAlive()) {
        }
    }

}
