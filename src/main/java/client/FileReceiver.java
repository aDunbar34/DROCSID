package client;


import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Connects to a peer, receives a file
 * and writes it to persistent storage
 *
 * @author Euan Gilmour
 */
public class FileReceiver implements Runnable {

    private final String senderUsername;
    private final String senderHost;
    private final int portNo;
    private String fileName;
    private static final int BUFFER_SIZE = 4096; // 4 KB

    public FileReceiver(String senderUsername, String senderHost, int portNo, String fileName) {
        this.senderUsername = senderUsername;
        this.senderHost = senderHost;
        this.portNo = portNo;
        this.fileName = fileName;
    }

    @Override
    public void run() {

        System.out.println("User <" + senderUsername + "> wants to send you a file '" + fileName + "'");
        System.out.println("Attempting to connect to <" + senderUsername + ">");

        // Establish a connection with sender
        try (Socket socket = new Socket(senderHost, portNo)) {

            System.out.println("Connection established with <" + senderUsername + ">, beginning file transfer.");
            receiveFile(socket);
            System.out.println("Successfully received file '" + fileName + "' from user <" + senderUsername + ">");

        } catch (UnknownHostException e) {
            System.out.println("ERROR: Sender host is unknown. Something has gone wrong.");
        } catch (IOException e) {
            System.out.println("ERROR: Something went wrong while trying to establish a connection to <" + senderUsername + ">");
        }

    }

    /**
     * Receives a file over TCP and saves it in the user's drocsidFiles directory
     *
     * @param socket the TCP socket the file will be read over
     *
     * @author Euan Gilmour
     */
    private void receiveFile(Socket socket) throws IOException {

        // Verify that drocsidFiles directory exists. Create it if not
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");
        if (!Files.exists(filesDirectory)) {
            try {
                Files.createDirectory(filesDirectory);
            } catch (IOException e) {
                System.out.println("ERROR: Something went wrong while trying to create files directory");
            }
        }

        // Check if a file with the same name already exists, rename new file if so
        Path filePath = Paths.get(filesDirectory.toString(), fileName);
        while (Files.exists(filePath)) {
            System.out.println("NOTICE: file '" + fileName + "' already exists. Saving new file as 'new_" + fileName + "'.");
            fileName = "new_" + fileName;
            filePath = Paths.get(filesDirectory.toString(), fileName);
        }

        // Setup input/output streams
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {

            // Setup buffer variables
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // Read bytes from socket and write to file
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

        }

    }

}
