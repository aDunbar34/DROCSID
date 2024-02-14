import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MessageBroadcaster implements Runnable {

    private final BlockingQueue<Message> messageQueue;
    private final List<ClientHandler> clients;

    public MessageBroadcaster(BlockingQueue<Message> messageQueue, List<ClientHandler> clients) {
        this.messageQueue = messageQueue;
        this.clients = clients;
    }

    public void run() {
        while (true) {
            try {
                Message message = messageQueue.take();

                synchronized (clients) {
                    for (ClientHandler client : clients) {
                        if (!(client.getClientId().equals(message.getSenderId()))) {
                            client.sendMessage(message);
                        }
                    }
                    clients.notify();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
