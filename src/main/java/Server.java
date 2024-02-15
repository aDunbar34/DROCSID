import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private LinkedBlockingQueue<Message> queue;

    public Server() {
        queue = new LinkedBlockingQueue<>();
        NonBlockingServer nonBlockingServer = new NonBlockingServer(queue);
        ServerConsumer[] serverConsumers = new ServerConsumer[4];
        Thread nonBlockingServerThread = new Thread(nonBlockingServer);

        nonBlockingServerThread.start();
        for(int i = 0; i < 4; i++){
            serverConsumers[i] = new ServerConsumer(queue, nonBlockingServer);
            serverConsumers[i].start();
        }
        while (nonBlockingServerThread.isAlive()){

        }
    }

    public static void main(String[] args) {
        LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
        NonBlockingServer nonBlockingServer = new NonBlockingServer(queue);
        ServerConsumer[] serverConsumers = new ServerConsumer[4];
        Thread nonBlockingServerThread = new Thread(nonBlockingServer);

        nonBlockingServerThread.start();
        for(int i = 0; i < 4; i++){
            serverConsumers[i] = new ServerConsumer(queue, nonBlockingServer);
            serverConsumers[i].start();
        }
        while (nonBlockingServerThread.isAlive()){

        }
    }
}