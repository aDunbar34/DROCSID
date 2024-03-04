package client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Listens for a connection from a peer and
 * sends a file when established
 *
 * @author Euan Gilmour
 */
public class FileSender implements Runnable {

    private final File file;
    private final int portNo;
    private final String recipientUsername;
    private final String recipientHost;
    private boolean transferComplete;
    private static final int TIMEOUT = 60000; // 60 seconds
    private static final int BUFFER_SIZE = 4096; // 4 KB

    public FileSender(File file, int portNo, String recipientUsername, String recipientHost) {
        this.file = file;
        this.portNo = portNo;
        this.recipientUsername = recipientUsername;
        this.recipientHost = recipientHost;
        this.transferComplete = false;
    }

    @Override
    public void run() {

        System.out.println("Attempting to send file '" + file.getName() + "' to user <" + recipientUsername + "> over port " + portNo);

        // Set up TCP listener
        try (ServerSocket listenerSocket = new ServerSocket(portNo)) {

            // Set timeout
            listenerSocket.setSoTimeout(TIMEOUT);

            System.out.println("Awaiting connection from peer...");

            // Await connection from peer
            while (!transferComplete) {
                try (Socket socket = listenerSocket.accept()) {
                    // Check connection is from correct recipient
                    String connectionHost = socket.getInetAddress().getHostAddress();
                    if (connectionHost.equals(recipientHost)) {
                        System.out.println("Connection with <" + recipientUsername + "> established. Beginning file transmission.");
                        sendFile(socket);
                        System.out.println("File '" + file.getName() + "' successfully sent to user <" + recipientUsername + ">");
                        transferComplete = true;
                    } else {
                        System.out.println("Connection received from incorrect client...");
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("ERROR: TCP Listener timed out. Please ensure the recipient's username is correct and that they are online");
                    break;
                } catch (IOException e) {
                    System.out.println("ERROR: Something went wrong with the TCP connection to <" + recipientUsername + ">");
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("ERROR: Something went wrong while trying to establish a TCP listener on port " + portNo);
        }

    }

    /**
     * Sends a file over TCP
     *
     * @param socket the TCP socket to transmit the file over
     * @author Euan Gilmour
     */
    private void sendFile(Socket socket) throws IOException {

        // Set up input/output streams
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {

            // Declare buffer variables
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // Transmit the file
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }

        }

    }

}
