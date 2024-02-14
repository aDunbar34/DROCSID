import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

    private final ServerSocket serverSocket;
    private final BlockingQueue<Message> messageQueue;
    private final List<ClientHandler> clients;
    private final ExecutorService executorService;


    private final int NUMBER_OF_THREADS = 10;

    public Server(int portNo) {
        try {
            this.serverSocket = new ServerSocket(portNo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.messageQueue = new LinkedBlockingQueue<>();
        this.clients = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }

    public void start() {
        System.out.println("Server starting...");
        int clientCount = 0

        try {
            Thread messageBroadcaster = new Thread(new MessageBroadcaster(messageQueue, clients));
            messageBroadcaster.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                String clientId = "" + clientCount;
                clientCount++;

                ClientHandler client = new ClientHandler(clientSocket, clientId, messageQueue);

                synchronized (clients) {
                    clients.add(client);
                    clients.notify();
                }

                executorService.submit(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}