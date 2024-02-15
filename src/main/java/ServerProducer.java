import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerProducer extends Thread {
    LinkedBlockingQueue<Message> q;
    BufferedReader inputStream;
    ObjectMapper objectMapper = new ObjectMapper();

    public ServerProducer(LinkedBlockingQueue<Message> q, BufferedReader inputStream) throws IOException {
        super();
        this.q = q;
//        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        while(true){
            try {
                if(inputStream.readLine() != null){
                    String clientResponse = inputStream.readLine();
                    Message message = objectMapper.readValue(clientResponse, Message.class);
                    q.put(message);//put message on the queue
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
