package server;

import client.ClientData;
import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.Message;
import messageCommunication.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Creates a server consumer thread that reads the nonblocking queue of messages that the server has received and does an appropriate action dependent on this.
 * @author Robbie Booth
 */
public class ServerConsumer extends Thread{
    LinkedBlockingQueue<Message> q;
    NonBlockingServerProducer nonBlockingServer;

    private ObjectMapper objectMapper = new ObjectMapper();

    public ServerConsumer(LinkedBlockingQueue<Message> q, NonBlockingServerProducer nonBlockingServer){
        super();
        this.q = q;
        this.nonBlockingServer = nonBlockingServer;
    }

    @Override
    public void run() {
        while(true){
            try {
                //get message from key
                //appropriately treat message
                //return values to output socket location
                Message message = q.take();//Thread will wait here
                ClientData senderData = nonBlockingServer.getClientData(message.getSenderId());
                switch (message.getType()){
                    case TEXT -> {//add text to the room the user currently is in and send to other users in that room
                        String currentRoom = senderData.getCurrentRoom();//out of sync problem potentially here
                        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(currentRoom);
                        clientsInRoom.remove(senderData);//remove the client that sent it as they already will have the message
                        for (ClientData clientInRoom: clientsInRoom){//send the message to all the clients currently in that room
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(message);
                            System.out.println("Sending message: " + message.getTextMessage() + " to "+clientInRoom.getUsername() +" in room: "+clientInRoom.getCurrentRoom());
                            nonBlockingServer.writeDataToClient(clientInRoom.getUserChannel(), messageAsByteJSON);
                        }
                    }

                    case ONLINE_STATUSES -> {
                        String currentRoom = senderData.getCurrentRoom(); //out of sync problem potentially here
                        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(currentRoom);
                        List<String> userList = new ArrayList<String>();
                        String onlineStatuses = "Users in Room: \n";

                        // Populate list of usernames
                        for (ClientData clientInRoom: clientsInRoom) {
                            userList.add(clientInRoom.getUsername());
                            onlineStatuses += "- " + clientInRoom.getUsername() + "\n";
                        }
                        System.out.println(userList);

                        Message clientMessage = new Message(0, MessageType.ONLINE_STATUSES , senderData.getUsername(), null, System.currentTimeMillis(), onlineStatuses );
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(clientMessage);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                    }

                    case SELECT_ROOM -> {//select rooms so that we send the message to the user(s) only if they are currently in that room, else we save to history and send message when history
                        System.out.println("Changed user: " + senderData.getUsername() +" room to: " + message.getTargetId() +" from: "+senderData.getCurrentRoom());
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
                    case FILE_SEND_SIGNAL -> handleFileSendSignal(message);
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


    /**
     * Parses the message for file transfer parameters and
     * sends new FILE_RECEIVE_SIGNAL to the correct client
     *
     * @param message the message
     */
    private void handleFileSendSignal(Message message) throws IOException {

        // Parse message payload
        String[] fileTransferDetails = message.getTextMessage().split(",");
        String fileName = fileTransferDetails[0];
        String recipientUsername = fileTransferDetails[1];
        String portNo = fileTransferDetails[2];

        ClientData senderData = nonBlockingServer.getClientData(message.getSenderId());

        // Search for recipient
        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(senderData.getCurrentRoom());
        for (ClientData client : clientsInRoom) {
            if (client.getUsername().equals(recipientUsername)) {

                System.out.println("Handling file transfer signals between sender <" + message.getSenderId() + "> and receiver <" + recipientUsername + ">");

                // Prepare and send new FILE_RECEIVE_SIGNAL
                String senderHost = ((InetSocketAddress) senderData.getUserChannel().getRemoteAddress()).getHostString();
                String payload = senderHost + ',' + portNo + ',' + fileName;
                Message fileReceiveSignal = new Message(0, MessageType.FILE_RECEIVE_SIGNAL, message.getSenderId(), senderData.getCurrentRoom(), System.currentTimeMillis(), payload);
                byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(fileReceiveSignal);
                nonBlockingServer.writeDataToClient(client.getUserChannel(), messageAsByteJSON);

            }
        }

    }
}