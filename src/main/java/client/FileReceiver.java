package client;


import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

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
    private Socket socket;
    private boolean connectionEstablished;
    private static final int BUFFER_SIZE = 4096; // 4 KB

    public FileReceiver(String senderUsername, String senderHost, int portNo, String fileName) {
        this.senderUsername = senderUsername;
        this.senderHost = senderHost;
        this.portNo = portNo;
        this.fileName = fileName;
        this.connectionEstablished = false;
    }

    @Override
    public void run() {

        System.out.println("User <" + senderUsername + "> wants to send you a file '" + fileName + "'");
        System.out.println("Attempting to connect to <" + senderUsername + ">");

        // Schedule a connection attempt task with a timeout
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (attemptConnection()) {
                    timer.cancel();
                }
            }
        }, 0, 1000); // Attempt connection every 5 seconds

        // Wait for timeout or until a successful connection
        long startTime = System.currentTimeMillis();
        while (!connectionEstablished && (System.currentTimeMillis() - startTime) / 1000 < 60) {}

        if ((System.currentTimeMillis() - startTime) / 1000 >= 60) {
            System.out.println("ERROR: Unable to establish a connection with <" + senderUsername + ">");
            return;
        }

        try {
            receiveFile();
            socket.close();
        } catch (IOException e) {
            System.out.println("ERROR: Something went wrong during file transfer.");
        }

    }

    /**
     * Attempts to make a connection to the sender
     *
     * @return true if attempt successful, otherwise false
     *
     * @author Euan Gilmour
     */
    private boolean attemptConnection() {
        try  {
            socket = new Socket(senderHost, portNo);
            System.out.println("Connection established with <" + senderUsername + ">, beginning file transfer.");
            connectionEstablished = true;
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * Receives a file over TCP and saves it in the user's drocsidFiles directory
     *
     *
     * @author Euan Gilmour
     */
    private void receiveFile() throws IOException {

        System.out.println("Transfer begins here");

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

            System.out.println("Successfully received file '" + fileName + "' from user <" + senderUsername + ">");

        }

    }

}
