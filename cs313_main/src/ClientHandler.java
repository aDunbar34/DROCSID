import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final String clientId;
    private final BlockingQueue<Message> messageQueue;
    private ObjectInputStream clientReader;
    private ObjectOutputStream clientWriter;

    public ClientHandler(Socket clientSocket, String clientId, BlockingQueue<Message> messageQueue) {
        this.clientSocket = clientSocket;
        this.clientId = clientId;
        this.messageQueue = messageQueue;
        try {
            clientReader = new ObjectInputStream(this.clientSocket.getInputStream());
            clientWriter = new ObjectOutputStream(this.clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            ObjectInputStream clientReader = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Message message = (Message) clientReader.readObject();

                messageQueue.put(message);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(Message message) {
        try {
            clientWriter.writeObject(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getClientId() {
        return clientId;
    }
}
