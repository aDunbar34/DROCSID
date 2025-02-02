package server;

import client.ClientData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import customExceptions.ClientDisconnectException;
import customExceptions.InvalidMessageException;
import messageCommunication.Message;
import messageCommunication.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A non-blocking producer that reads the servers incoming messages and adds the message to the queue for the consumers
 * or if new client connected, it initiates that connection.
 * @author Robbie Booth
 */
public class NonBlockingServerProducer implements Runnable {

    private Map<String, ClientData> connectedClients;
    private final Object connectedClientLock = new Object();

    private HashSet<String> allRooms;

    LinkedBlockingQueue<Message> queueOfMessageToBeRead;
    private final int portNumber;


    public NonBlockingServerProducer(LinkedBlockingQueue<Message> queue, int portNumber)  {
        queueOfMessageToBeRead = queue;
        connectedClients = new HashMap<String, ClientData>();
        this.portNumber = portNumber;
        Collection<String> rooms = History.readAllRoomNames();
        System.out.println("Loaded rooms from history: "+String.join(", ", rooms));
        this.allRooms = new HashSet<>(rooms);
    }

    @Override
    public void run() {
        try{
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(portNumber));//Creates a server socket where the ip address is the location and the port number
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port" + portNumber);

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(serverSocketChannel, selector);//on new connection it starts up it
                    } else if(key.isValid() && key.isReadable()){//Checks if key is still active and is readable
                        try{
                            queueOfMessageToBeRead.put(getMessageFromKey(key));//gets the message and adds it to the queue for consumers
                        }catch (InvalidMessageException e){
                            System.out.println("Message is empty!");
                        }catch (SocketException e){
                            closeConnection(key);//on the connection exit of a client it closes that connection
                        }

                    }
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClientDisconnectException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public HashSet<String> getAllRooms() {
        return allRooms;
    }

    /**
    * @return The connected clients data or null if not connected
     * @author Robbie Booth
    * */
    public synchronized ClientData getClientData(String username){
        return connectedClients.get(username);
    }

    public synchronized Set<String> getClientRooms(String username){
        ClientData client = connectedClients.get(username);
        if(client.equals(null)){
            return null;
        }
        return client.getUserRooms();
    }

    /**
     *
     * @param room the room to be searched
     * @return A list of connected clients in the room given
     * @author Robbie Booth
     * */
    public List<ClientData> getClientsInRoom(String room){
        List<ClientData> clientsInRoom = new ArrayList<>();
        for (ClientData clientData: connectedClients.values()){
            if(clientData.getCurrentRoom() != null && clientData.getCurrentRoom().equals(room)){
                clientsInRoom.add(clientData);
            }
        }
        return clientsInRoom;
    }

    /**
     *
     * Returns a list of all clients in the server
     * @return A list of connected clients in the server
     * @author Adam Dunbar
     *
     * */

    public synchronized Collection<ClientData> getClientsInServer() {
        return  connectedClients.values();
    }

    /**
     * Closes the connection of the key parameter and removes the client associates from the {@link #connectedClients}.
     *
     * @param key the selection key of the socket channel to be removed
     * @throws IOException
     * @author Robbie Booth
     */
    private void closeConnection(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        String usernameToRemove = null;
        synchronized (connectedClientLock) {
            for (Iterator<ClientData> iterator = connectedClients.values().iterator(); iterator.hasNext();) {
                ClientData clientData = iterator.next();
                if (clientData.getUserChannel().equals(clientChannel)) {
                    usernameToRemove = clientData.getUsername();
                }
            }
            if(usernameToRemove != null){
                connectedClients.remove(usernameToRemove);
                System.out.println(usernameToRemove +" disconnected!");
            }
        }
        clientChannel.close();
    }

    private void acceptConnection(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
    }

    /**
     * Takes the message and returns it if on the channel
     * @return the message object given in the channel
     * @author Robbie Booth
     * */
    public Message getMessageFromKey(SelectionKey key) throws InvalidMessageException, ClientDisconnectException, IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by client
//            key.cancel();
//            clientChannel.close();
//            System.out.println("client.Client disconnected: " + clientChannel.getRemoteAddress());
//            throw new ClientDisconnectException("client.Client has been disconnected");
            closeConnection(key);
        } else if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data);
            System.out.println("Received from client " + clientChannel.getRemoteAddress() + ": " + message);
            ObjectMapper objectMapper = new ObjectMapper();
            Message messageParsed;
            try{
                messageParsed = objectMapper.readValue(message, Message.class);
                System.out.println(messageParsed.getSenderId());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new InvalidMessageException("messageCommunication.Message being parsed is invalid");
            }
            if(messageParsed.getType().equals(MessageType.INITIALISATION)){//on initialisation you add to the connected clients
                addConnectedClient(clientChannel, messageParsed.getSenderId());
            }
            return messageParsed;
        }
        throw new InvalidMessageException("messageCommunication.Message is empty");
    }

    /**
     * Adds the connected client to {@link #connectedClients} if not already there
     * @param clientChannel socket channel of the client
     * @param username the unique username of the client
     * @author Robbie Booth
     */
    public void addConnectedClient(SocketChannel clientChannel, String username){
        //TODO make system to read rooms that client is in from saved
        //TODO make check that client current room isn't null else where as it will be null here
        //TODO make error if client is already connected and kill connection with the client trying to impersonate
        synchronized (connectedClientLock) {
            if (connectedClients.containsKey(username)) {
                return;
            }
            ClientData clientData;
            try{
                clientData = History.readUser(username);
                if(clientData == null){
                    clientData = new ClientData(username, clientChannel, new HashSet<>());
                    History.createUser(username); //Create user if it doesn't exist. Else this does nothing
                }else{
                    //set user channel as it will be null
                    clientData.setUserChannel(clientChannel);
                }
            } catch (IOException e) {
                e.printStackTrace();
                clientData = new ClientData(username, clientChannel, new HashSet<>());
            }

            connectedClients.put(username, clientData);
        }
    }

    /**
     * Messages the client channel given with the data given
     * @param clientChannel client channel to be messaged
     * @param data data to be sent to client channel
     * @throws IOException
     * @author Robbie Booth
     */
    public void writeDataToClient(SocketChannel clientChannel, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        clientChannel.write(buffer);
        System.out.println("Messaged Client");
    }

    //File operations:



}
