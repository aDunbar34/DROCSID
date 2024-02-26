package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.Message;
import messageCommunication.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Initialises the connection with the server sets the room and then any messages that the client sends will be sent to
 * the server socket given {@link #socket}.
 *
 * @author Robbie Booth
 */
public class ClientProducer implements Runnable {
    private final Socket socket;
    private final String username;

    private String chatRoomId;
    private final BufferedReader stdIn;
    PrintWriter out;

    ObjectMapper objectMapper = new ObjectMapper();

    public ClientProducer(Socket socket, String username, String chatRoomId) {
        this.socket = socket;
        this.username = username;
        this.chatRoomId = chatRoomId;
        stdIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            out = new PrintWriter(this.socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void run() {
        try {
            //Initialise
            Message initialisationMessage = new Message(0, MessageType.INITIALISATION, username, (String) null, System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(initialisationMessage));

            //Select room
            //In future this will allow room choosing
            initialisationMessage = new Message(0, MessageType.SELECT_ROOM, username, "test", System.currentTimeMillis(), "");
            out.println(objectMapper.writeValueAsString(initialisationMessage));

            // Reads string from client and sends it back to the client String inputLine;
            System.out.println("Enter a message:");
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                parseInput(userInput);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Parses the input for special commands and calls the relevant command function.
     * Input is treated as a message and if no command found
     *
     * @param userInput The input to be parsed.
     *
     * @author Euan Gilmour
     */
    public void parseInput(String userInput) {

        // Check for empty string
        if (userInput.isEmpty()) {
            return;
        }

        // Check for special command
        if (userInput.charAt(0) == '\\') {
            String[] commandArgs = userInput.split(" ");

            switch (commandArgs[0]) {
                case "\\files" -> showFiles();
                case "\\view" -> viewImage(commandArgs);
                case "\\play" -> playVideo(commandArgs);
                case "\\sendFile" -> sendFile(commandArgs);
                default -> System.out.println("Unrecognized command: '" + commandArgs[0] + "'.");

            }
        } else { // Treat input as message
            sendTextMessage(userInput);
        }
    }

    /**
     * Prints a list of files in the drocsidFiles directory.
     * If no such directory exists, it creates it.
     */
    private void showFiles() {

        // Construct path to files directory
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");

        // Create directory if it does not exist
        if (!Files.exists(filesDirectory)) {
            try {
                Files.createDirectory(filesDirectory);
            } catch (IOException e) {
                System.out.println("ERROR: Something went wrong while trying to create files directory");
            }
        }

        // Display filenames
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(filesDirectory)) {
            Iterator<Path> iterator = directoryStream.iterator();
            if (iterator.hasNext()) {
                System.out.println("Files:");
                while (iterator.hasNext()) {
                    Path filePath = iterator.next();
                    System.out.println(filePath.getFileName().toString());
                }
            } else {
                System.out.println("No files!");
            }
        } catch (IOException e) {
            System.out.println("ERROR: Something went wrong while trying to view the files directory");
        }

    }

    /**
     * Prepares and displays an image via ImageViewer
     *
     * @param args the args the command was called with
     *
     * @author Euan Gilmour
     */
    private void viewImage(String[] args) {

        // Check for incorrect number of arguments
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\view <filename>");
            return;
        }

        // Get filename
        String fileName = args[1];

        // Verify that file exists
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");
        Path filePath = filesDirectory.resolve(fileName);
        if (!(Files.exists(filePath) && Files.isRegularFile(filePath))) {
            System.out.println("Error trying to view file '" + fileName + "': no such file exists.");
            return;
        }

        // Verify that file is a valid image file
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff"};
        if (Arrays.stream(imageExtensions).noneMatch(fileName::endsWith)) {
            System.out.println("Error trying to view file '" + fileName + "': file is not a valid image file");
            return;
        }

        // Set up ImageViewer and view image
        ImageViewer imageViewer = new ImageViewer();
        imageViewer.viewImage(filePath.toString());

    }

    /**
     * Prepares and plays a video via VideoPlayer
     *
     * @param args the args the command was called with
     *
     * @author Euan Gilmour
     */
    private void playVideo(String[] args) {

        // Check for incorrect number of arguments
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\play <filename>");
            return;
        }

        // Get filename
        String fileName = args[1];

        // Verify that file exists
        Path filesDirectory = Paths.get(System.getProperty("user.dir"), "drocsidFiles");
        Path filePath = filesDirectory.resolve(fileName);
        if (!(Files.exists(filePath) && Files.isRegularFile(filePath))) {
            System.out.println("Error trying to play file '" + fileName + "': no such file exists.");
            return;
        }

        // Verify that file is a valid video file
        String[] videoExtensions = {".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mpeg", ".mpg"};
        if (Arrays.stream(videoExtensions).noneMatch(fileName::endsWith)) {
            System.out.println("Error trying to play file '" + fileName + "': file is not a valid video file");
            return;
        }

        // Set up VideoPlayer and play video
        VideoPlayer videoPlayer = new VideoPlayer();
        videoPlayer.playVideo(filePath.toString());

    }

    /**
     * Prepares a new FileSender thread to send a file
     * and prepares and sends a FILE_SEND_SIGNAL Message
     *
     * @param args The args the command was called with.
     *
     * @author Euan Gilmour
     */
    private void sendFile(String[] args) {

        // Check for incorrect number of arguments
        if (args.length != 4) {
            System.out.println("Incorrect number of arguments");
            System.out.println("USAGE: \\sendFile <pathToFile> <recipientUsername> <portNo>");
            return;
        }

        // Unpack args
        String pathString = args[1];
        String recipient = args[2];
        int portNo;
        try {
            portNo = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Port must be a number.");
            System.out.println("USAGE: \\sendFile <pathToFile> <recipientUsername> <portNo>");
            return;
        }

        // Verify that file exists
        Path filePath = Paths.get(pathString);
        if (!(Files.exists(filePath) && Files.isRegularFile(filePath))) {
            System.out.println("ERROR: The given path is incorrect or file does not exist.");
            System.out.println("USAGE: \\sendFile <pathToFile> <recipientUsername> <portNo>");
            return;
        }

        // Verify that file is under the size limit
        int sizeLimit = 104857600; // 100 MB in bytes
        try {
            if (Files.size(filePath) > sizeLimit) {
                System.out.println("ERROR: The file is over the size limit of 100MB");
                return;
            }
        } catch (IOException e) {
            System.out.println("ERROR: something went wrong while assessing the size of the file.");
            return;
        }

        // All validation checks passed, spin up a new FileSender thread
        Thread fileSender = new Thread(new FileSender(filePath.toFile(), portNo, recipient));
        fileSender.start();

        // Prepare and send new FILE_SEND_SIGNAL Message to the server
        String fileName = filePath.getFileName().toString();
        String payload = fileName + ',' + recipient + ',' + portNo;
        Message fileSendSignalMessage = new Message(0, MessageType.FILE_SEND_SIGNAL, username, chatRoomId, System.currentTimeMillis(), payload);
        try {
            out.println(objectMapper.writeValueAsString(fileSendSignalMessage));
        } catch (JsonProcessingException e) {
            System.out.println("ERROR: Something went wrong while trying to send a FILE_SEND_SIGNAL message to the server");
        }

    }

    /**
     * Prepares and sends a text message to the server.
     *
     * @param userInput The input to be sent as a message.
     *
     * @author Robbie Booth
     */
    private void sendTextMessage(String userInput) {
        Message message_to_send = new Message(0, MessageType.TEXT, username, chatRoomId, System.currentTimeMillis(), userInput);//make message
        try {
            out.println(objectMapper.writeValueAsString(message_to_send));//send message to server
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}