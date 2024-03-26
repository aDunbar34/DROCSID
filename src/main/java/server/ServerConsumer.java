package server;

import client.Client;
import client.ClientData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messageCommunication.Message;
import messageCommunication.MessageType;
import messageCommunication.UserMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.ArrayList;
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

                        //Add message to room history:
                        UserMessage userMessage = new UserMessage(senderData.getUsername(), message.getTimestamp(), message.getTextMessage());
                        try{
                            History.addMessageToHistory(userMessage, currentRoom);
                        }catch (IOException e){
                            e.printStackTrace();
                            System.out.println("IO Exception saving "+senderData.getUsername() +" message to room " + currentRoom+" history!");
                        }

                        //Message the clients
                        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(currentRoom);
                        clientsInRoom.remove(senderData);//remove the client that sent it as they already will have the message
                        for (ClientData clientInRoom: clientsInRoom){//send the message to all the clients currently in that room
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(message);
                            System.out.println("Sending message: " + message.getTextMessage() + " to "+clientInRoom.getUsername() +" in room: "+clientInRoom.getCurrentRoom());
                            nonBlockingServer.writeDataToClient(clientInRoom.getUserChannel(), messageAsByteJSON);
                        }
                    }

                    case FRIEND_REQUEST_LIST -> {
                        String friendRequestListDisplay = "";
                        Set<String> friend_request_list = senderData.getIncomingFriendRequests();

                        if (!friend_request_list.isEmpty()) {
                            friendRequestListDisplay = "Requests: \n";
                            synchronized (friend_request_list) {
                                for (String request: friend_request_list) {
                                    friendRequestListDisplay += "- Request from: " + request;
                                }
                            }
                        } else {
                            friendRequestListDisplay = "No Friend Requests";
                        }
                        Message clientMessage = new Message(0, MessageType.FRIEND_REQUEST_LIST , senderData.getUsername(), null, System.currentTimeMillis(), friendRequestListDisplay);
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(clientMessage);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);

                    }
                    case FRIENDS_LIST -> {
                        // Uncomment for Testing
//                         Set<String> templist = new HashSet<String>();
//                         templist.add("tester");
//                         senderData.setFriends(templist);

                        String onlineStatus = "offline";
                        String friendListDisplay = "";

                        Set<String> friend_list = senderData.getFriends();
                        if (!friend_list.isEmpty()) {
                            friendListDisplay = "Friends: \n";
                            synchronized (friend_list) {
                                for (String friend: friend_list) {
                                    if (userOnlineCheck(friend)) {
                                        onlineStatus = "Online";
                                    } else {
                                        onlineStatus = "Offline";
                                    }
                                    friendListDisplay += "- " + friend + " (" + onlineStatus + ")" + "\n";
                                }
                            }
                        } else {
                            friendListDisplay = "No friends yet";
                        }
                        Message clientMessage = new Message(0, MessageType.FRIENDS_LIST , senderData.getUsername(), null, System.currentTimeMillis(), friendListDisplay);
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(clientMessage);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                    }

                    case ONLINE_STATUSES -> {
                        String onlineStatuses = "";
                        if (senderData.getCurrentRoom() != null) {
                            String currentRoom = senderData.getCurrentRoom(); //out of sync problem potentially here
                            List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(currentRoom);
                            // Create the message to display as response
                            onlineStatuses = "Users in Room: \n";
                            synchronized (clientsInRoom) {
                                for (ClientData clientInRoom: clientsInRoom) {
                                    onlineStatuses += "- " + clientInRoom.getUsername() + "\n";
                                }
                            }

                        } else if (senderData.getCurrentRoom() == null) {
                            Collection<ClientData> clientsInServer = nonBlockingServer.getClientsInServer();

                            // Create the message to display as response
                            onlineStatuses = "Users in Server: \n";
                            synchronized (clientsInServer) {
                                for (ClientData clientInServer: clientsInServer) {
                                    onlineStatuses += "- " + clientInServer.getUsername() + "\n";
                                }
                            }
                        }
                        Message clientMessage = new Message(0, MessageType.ONLINE_STATUSES , senderData.getUsername(), null, System.currentTimeMillis(), onlineStatuses );
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(clientMessage);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                    }

                    case SELECT_ROOM -> {//select rooms so that we send the message to the user(s) only if they are currently in that room, else we save to history and send message when history
                        String room = null;
                        if(message.getTargetId() == null){
                            System.out.println("User: " + senderData.getUsername() +" leaving room: "+ senderData.getCurrentRoom());
                            senderData.setCurrentRoom(null);//TODO synchronize

                            Message response = new Message(0, MessageType.SELECT_ROOM, senderData.getUsername(), null , System.currentTimeMillis(), "success");
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);

                            break;
                        }
                        boolean userPartOfRoom = false;

                        Set<String> clientRoomsRef = nonBlockingServer.getClientRooms(senderData.getUsername());
                        synchronized (clientRoomsRef){
                            if(clientRoomsRef.contains(message.getTargetId())){
                                System.out.println("Changed user: " + senderData.getUsername() +" room to: " + message.getTargetId() +" from: "+senderData.getCurrentRoom());
                                senderData.setCurrentRoom(message.getTargetId());//TODO: potential bug here as i am getting confused with pass by reference and pass by copy
                                room = message.getTargetId();
                                userPartOfRoom = true;
                            }
                        }

                        if(!userPartOfRoom){
                            Message response = new Message(0, MessageType.SELECT_ROOM, senderData.getUsername(), null , System.currentTimeMillis(), "You are not part of room: "+message.getTargetId());
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                            break;
                        }

                        Message response = new Message(0, MessageType.SELECT_ROOM, senderData.getUsername(), room , System.currentTimeMillis(), "success");
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);

                        //Then Send history:
                        byte[] historyMessages = objectMapper.writeValueAsBytes(History.readRoomHistory(room));
                        Message historyResponse = new Message(0, MessageType.HISTORY, senderData.getUsername(), room, System.currentTimeMillis(), historyMessages);
                        messageAsByteJSON = objectMapper.writeValueAsBytes(historyResponse);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
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
                    case CREATE_ROOM -> {
                        String[] usernames = objectMapper.readValue(message.getPayload(), String[].class);
                        boolean roomAlreadyExists = true;
                        //Create room
                        Set<String> allRooms = nonBlockingServer.getAllRooms();
                        synchronized (allRooms){
                            if (!allRooms.contains(message.getTargetId())){
                                roomAlreadyExists = false;
                                //create room
                                allRooms.add(message.getTargetId());
                            }
                        }
                        if(roomAlreadyExists){
                            Message response = new Message(0, MessageType.CREATE_ROOM, senderData.getUsername(), null, System.currentTimeMillis(), "Room already exists!");
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                            break;
                        }

                        //Add the room to all users data and rooms data add all users. This also creates the room and user if they don't exist
                        List<String> usersInRoom = new ArrayList<>(List.of(usernames));
                        usersInRoom.add(message.getSenderId());
                        History.addUsersToRoom(message.getTargetId(), usersInRoom);

                        //add rooms to each user in memory
                        for(String username: usersInRoom){
                            ClientData clientData = nonBlockingServer.getClientData(username);
                            if(clientData != null){
                                synchronized (clientData) {
                                    clientData.addRoom(message.getTargetId());
                                }
                            }
                        }

                        Message response = new Message(0, MessageType.CREATE_ROOM, senderData.getUsername(), null, System.currentTimeMillis(), "Room: "+message.getTargetId()+" created!");
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                    }

                    case SEND_FRIEND_REQUEST -> {
                        String username = new String(message.getPayload());

                        boolean becomeFriends = false;

                        // Create user in history if they don't exist already and declare them as the target client
                        ClientData connectedReciever = nonBlockingServer.getClientData(username);
                        if(connectedReciever != null){
                            synchronized (connectedReciever) {
                                if(connectedReciever.getOutgoingFriendRequests().contains(senderData.getUsername())){//They have an outgoing to the person who sent one
                                    becomeFriends = true;
                                    connectedReciever.addFriend(senderData.getUsername());
                                }else{
                                    connectedReciever.addIncomingFriendRequest(senderData.getUsername());
                                }
                            }
                        }else{
                            History.createUser(username);
                            ClientData targetClient = History.readUser(username);
                            if(targetClient != null && targetClient.getOutgoingFriendRequests().contains(senderData.getUsername())){
                                becomeFriends = true;
                            }
                        }

                        synchronized (senderData){
                            if(becomeFriends){
                                senderData.addFriend(username);
                            }else{
                                senderData.addOutGoingRequest(username);
                            }
                        }

                        //Add to history
                        History.sendFriendRequest(senderData.getUsername(), username);

                        //Create message to clients:
                        String senderMessage = "Friend Request sent to: " + username;
                        String receiverMessage = "You've recieved a friend request from: " + senderData.getUsername();
                        if(becomeFriends){
                            senderMessage = "You have become friends with: "+username;
                            receiverMessage = "You have become friends with: "+senderData.getUsername();
                        }


                        // Send Confirmation to sender
                        Message response = new Message(0, MessageType.SEND_FRIEND_REQUEST, senderData.getUsername(), null, System.currentTimeMillis(), senderMessage);
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);

                        if(connectedReciever != null){
                            // Send you've received request to receiver
                            response = new Message(0, MessageType.SEND_FRIEND_REQUEST, senderData.getUsername(), null, System.currentTimeMillis(), receiverMessage);
                            messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(connectedReciever.getUserChannel(), messageAsByteJSON);
                        }

                    }

                    case ACCEPT_FRIEND -> {
                        String username = new String(message.getPayload());
                        ClientData targetClient = null;
                        Set<String> friend_list = senderData.getFriends();
                        Set<String> request_list = senderData.getIncomingFriendRequests();
                        ClientData clientInRoom;

                        ClientData clientInServer = nonBlockingServer.getClientData(username);

                        boolean requestExists = false;
                        if(clientInServer!= null){
                            targetClient = clientInServer;
                        }

                        synchronized (request_list) {
                            requestExists = request_list.contains(username);
                        }

                        if(!requestExists){
                            Message response = new Message(0, MessageType.ACCEPT_FRIEND, senderData.getUsername(), null, System.currentTimeMillis(), "Request for "+ username + " doesn't exist!");
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                            break;
                        }

                        if(targetClient != null){
                            synchronized (targetClient){
                                targetClient.addFriend(senderData.getUsername());
                            }
                        }
                        synchronized (senderData){
                            senderData.addFriend(username);
                        }

                        // Add to history
                        History.acceptFriendRequest(username, senderData.getUsername());

                        Message response = new Message(0, MessageType.ACCEPT_FRIEND, senderData.getUsername(), null, System.currentTimeMillis(), "You have become friends with: " + username);
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);

                        if(targetClient != null){//If user is online tell them you are now friends
                            response = new Message(0, MessageType.ACCEPT_FRIEND, senderData.getUsername(), null, System.currentTimeMillis(), "You have become friends with: " + senderData.getUsername());
                            messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(targetClient.getUserChannel(), messageAsByteJSON);
                        }
                    }

                    case ADD_MEMBERS -> {
                        String[] usernames = objectMapper.readValue(message.getPayload(), String[].class);
                        Set<String> allRooms = nonBlockingServer.getAllRooms();
                        boolean roomExists = false;
                        //Create room
                        synchronized (allRooms){
                            if (allRooms.contains(message.getTargetId())){
                                roomExists = true;
                            }
                        }
                        //Break as we cant do anything here as room doesn't exist
                        if(!roomExists){
                            Message response = new Message(0, MessageType.ADD_MEMBERS, senderData.getUsername(), null, System.currentTimeMillis(), "Room: "+message.getTargetId()+" doesn't exist!");
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                            break;
                        }

                        Set<String> clientRooms = nonBlockingServer.getClientRooms(message.getSenderId());
                        if(!clientRooms.contains(message.getTargetId())){
                            //client is not in room so cant add users
                            System.out.println(message.getSenderId() + " has requested to add room: "+message.getTargetId() +" but is not part of room!");
                            Message response = new Message(0, MessageType.ADD_MEMBERS, senderData.getUsername(), null, System.currentTimeMillis(), "You are not a member of room: "+message.getTargetId()+"!");
                            byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                            nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                            break;
                        }

                        //Add the room to all users data and rooms data add all users. This also creates the room and user if they don't exist
                        List<String> usersInRoom = new ArrayList<>(List.of(usernames));
                        usersInRoom.add(message.getSenderId());//add sender if already not contained
                        History.addUsersToRoom(message.getTargetId(), usersInRoom);


                        List<String> usersAddedToRoom = new ArrayList<>();
                        //add rooms to each user in memory
                        for(String username: usernames){
                            ClientData clientData = nonBlockingServer.getClientData(username);
                            if(clientData != null){
                                synchronized (clientData) {
                                    clientData.addRoom(message.getTargetId());
                                }

                            }
                            System.out.println("Adding room: "+message.getTargetId() + " to "+username+" rooms! - Requested by: "+message.getSenderId());
                            usersAddedToRoom.add(username);
                        }

                        Message response = new Message(0, MessageType.ADD_MEMBERS, senderData.getUsername(), null, System.currentTimeMillis(), "Added users: ["+String.join(", ", usersAddedToRoom)+"] to room: "+message.getTargetId()+"!");
                        byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(response);
                        nonBlockingServer.writeDataToClient(senderData.getUserChannel(), messageAsByteJSON);
                        //ADD rooms to each user in storage
                    }

                    case FILE_SEND_SIGNAL -> handleFileSendSignal(message);
                    case STREAM_SIGNAL -> handleStreamSignal(message);
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
     * Handles stream signal messages
     *
     * @param message the message
     */
    private void handleStreamSignal(Message message) throws IOException {

        // Search for recipient
        ClientData senderData = nonBlockingServer.getClientData(message.getSenderId());
        String recipientUsername = message.getTextMessage();
        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(senderData.getCurrentRoom());
        for (ClientData client : clientsInRoom) {
            if (client.getUsername().equals(recipientUsername)) {
                // Relay message when found
                nonBlockingServer.writeDataToClient(client.getUserChannel(), objectMapper.writeValueAsBytes(message));
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
        String filePath = fileTransferDetails[1];
        String recipientUsername = fileTransferDetails[2];
        String portNo = fileTransferDetails[3];

        ClientData senderData = nonBlockingServer.getClientData(message.getSenderId());

        // Search for recipient
        List<ClientData> clientsInRoom = nonBlockingServer.getClientsInRoom(senderData.getCurrentRoom());
        for (ClientData client : clientsInRoom) {
            if (client.getUsername().equals(recipientUsername)) {

                System.out.println("Handling file transfer signals between sender <" + message.getSenderId() + "> and receiver <" + recipientUsername + ">");

                // Prepare and send new FILE_LISTEN_SIGNAL
                String receiverHost = ((InetSocketAddress) client.getUserChannel().getRemoteAddress()).getHostString();
                String listenPayload = filePath + ',' + client.getUsername() + ',' + receiverHost + ',' + portNo;
                Message fileListenSignal = new Message(0, MessageType.FILE_LISTEN_SIGNAL, message.getSenderId(), senderData.getCurrentRoom(), System.currentTimeMillis(), listenPayload);
                byte[] listenMessageAsByteJSON = objectMapper.writeValueAsBytes(fileListenSignal);
                nonBlockingServer.writeDataToClient(senderData.getUserChannel(), listenMessageAsByteJSON);

                // Prepare and send new FILE_RECEIVE_SIGNAL
                String senderHost = ((InetSocketAddress) senderData.getUserChannel().getRemoteAddress()).getHostString();
                String payload = senderHost + ',' + portNo + ',' + fileName;
                Message fileReceiveSignal = new Message(0, MessageType.FILE_RECEIVE_SIGNAL, message.getSenderId(), senderData.getCurrentRoom(), System.currentTimeMillis(), payload);
                byte[] messageAsByteJSON = objectMapper.writeValueAsBytes(fileReceiveSignal);
                nonBlockingServer.writeDataToClient(client.getUserChannel(), messageAsByteJSON);
            }
        }
    }

    public boolean userOnlineCheck(String username) {
        Collection<ClientData> clientsInServer = nonBlockingServer.getClientsInServer();

        for (ClientData clientInServer: clientsInServer) {
            if (clientInServer.getUsername().equals(username)) {
                return true;
            }
        }

        return false;
    }
}