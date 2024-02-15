import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerConsumer extends Thread{
    LinkedBlockingQueue<Message> q;
    NonBlockingServer nonBlockingServer;

    private ObjectMapper objectMapper = new ObjectMapper();

    public ServerConsumer(LinkedBlockingQueue<Message> q, NonBlockingServer nonBlockingServer){
        super();
        this.q = q;
        this.nonBlockingServer = nonBlockingServer;
    }

    @Override
    public void run() {
        String x;
        while(true){
            try {
                //get message from key
                //appropriately treat message
                //return values to output socket location
                Message message = q.take();
                ClientData senderData = nonBlockingServer.getClientData(message.getSenderId());
                switch (message.getType()){
                    case TEXT -> {//add text to the room the user currently is in and send to other users in that room
                        String currentRoom = senderData.getCurrentRoom();//out of sync problem potentially here
                        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(currentRoom);
                        clientsInRoom.remove(senderData);//remove the client that sent it as they already will have the message
                        for (ClientData clientInRoom: clientsInRoom){//send the message to all the clients currently in that room
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(message);
                            nonBlockingServer.writeDataToClient(clientInRoom.getUserChannel(), messageAsByteJSON);
                        }
                    }
                    case SELECT_ROOM -> {//select rooms so that we send the message to the user(s) only if they are currently in that room, else we save to history and send message when history
                        senderData.setCurrentRoom(message.getTargetId());//TODO: potential bug here as i am getting confused with pass by reference and pass by copy
                    }
                    case ROOMS -> {//return all rooms available to user
                        byte[] roomsAsBytes = objectMapper.writeValueAsBytes(senderData.getUserRooms().toArray());

                        Message response = new Message(0, MessageType.ROOMS, senderData.getUsername(), null, System.currentTimeMillis(), roomsAsBytes);
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                    }
                    case INITIALISATION -> {//set up address that user has and return rooms
//                        nonBlockingServer.addConnectedClient(keyOfMessage, message.getSenderId());
                        //TODO error here
                    }
                    //eventually add a create room
                    default -> {

                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}