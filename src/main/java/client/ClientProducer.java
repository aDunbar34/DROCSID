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
                case "\\play" -> playVideo(commandArgs);
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
                throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }

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