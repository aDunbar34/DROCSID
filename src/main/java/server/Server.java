package server;

import messageCommunication.Message;

import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Non Blocking Server that creates {@link #numberOfProducerThreads} number of producer threads
 * and {@link #numberOfConsumerThreads} number of consumer threads <br>
 * The server that is created is on the computers IP address on the port number given
 *
 * @author Robbie Booth
 */
public class Server implements Runnable {
    private LinkedBlockingQueue<Message> queue;

    private final int portNumber;
    private static final int numberOfConsumerThreads = 4;
    private static final int numberOfProducerThreads = 1;//This value is just 1 right now and not getting used because of producer multiconsuming problem

    public Server(int portNumber) {
        this.portNumber = portNumber;
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        NonBlockingServerProducer nonBlockingServer = new NonBlockingServerProducer(queue, portNumber);
        ServerConsumer[] serverConsumers = new ServerConsumer[numberOfConsumerThreads];
        Thread nonBlockingServerThread = new Thread(nonBlockingServer);

        nonBlockingServerThread.start();
        for(int i = 0; i < numberOfConsumerThreads; i++){
            serverConsumers[i] = new ServerConsumer(queue, nonBlockingServer);
            serverConsumers[i].start();
        }
        while (nonBlockingServerThread.isAlive()){

        }
        for(ServerConsumer serverConsumer: serverConsumers){
            serverConsumer.interrupt();//Since threads wait at queue indefinitely, we have to interrupt them to kill them
        }
    }

    /**
     * Creates a {@link Server} on the computer IP address with the port number given
     * @param args Port number of server to be made
     *
     * @author Robbie Booth
     */
    public static void main(String[] args) {
        int portNumber = Integer.parseInt(args[0]);
        Server server = new Server(portNumber);
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        WebSocketsServer webSocketsServer = new WebSocketsServer();
        webSocketsServer.startServer();
        Thread serverThread = new Thread(server);
        serverThread.run();
        while(serverThread.isAlive()){

        }
    }
}