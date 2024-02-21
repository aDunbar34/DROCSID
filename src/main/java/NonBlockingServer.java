import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class NonBlockingServer implements Runnable {

    private Map<String, ClientData> connectedClients;

    LinkedBlockingQueue<Message> queueOfMessageToBeRead;


    public NonBlockingServer(LinkedBlockingQueue<Message> queue)  {
        queueOfMessageToBeRead = queue;
        connectedClients = new HashMap<String, ClientData>();

    }

    @Override
    public void run() {
        try{
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8080));
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port 8080");

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(serverSocketChannel, selector);
                    } else if(key.isValid() && key.isReadable()){//Checks if key is still active and is readable
                        try{
                            queueOfMessageToBeRead.put(getMessageFromKey(key));
                        }catch (InvalidMessageException e){
                            e.printStackTrace();
                        }catch (SocketException e){
                            closeConnection(key);
                        }

                    }
                }
            }
        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClientDisconnectException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @return The connected clients data or null if not connected
    * */
    public synchronized ClientData getClientData(String username){
        return connectedClients.get(username);
    }

    /**
     *
     * @param room the room to be searched
     * @return A list of connected clients in the room given
     * */
    public synchronized List<ClientData> getClientsInRoom(String room){
        List<ClientData> clientsInRoom = new ArrayList<>();
        for (ClientData clientData: connectedClients.values()){
            if(clientData.getCurrentRoom().equals(room)){
                clientsInRoom.add(clientData);
            }
        }
        return clientsInRoom;
    }

    private void closeConnection(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        for (ClientData clientData:connectedClients.values()
             ) {
            if(clientData.getUserChannel().equals(clientChannel)){
                connectedClients.remove(clientData.getUsername());
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

    public void readDataFromClient(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by client
            key.cancel();
            clientChannel.close();
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
        } else if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data);
            System.out.println("Received from client " + clientChannel.getRemoteAddress() + ": " + message);

            // Example response to the client
            String responseMessage = "Hello, client!";
            writeDataToClient(clientChannel, responseMessage.getBytes());
        }
    }


    /**
     * Takes the message and returns it if on the channel
     * @return the message object given in the channel
     * */
    public Message getMessageFromKey(SelectionKey key) throws InvalidMessageException, ClientDisconnectException, IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
//        System.out.println(clientChannel.isConnected());
//        System.out.println(clientChannel.isOpen());
//        System.out.println(clientChannel.isRegistered());
//        if(!clientChannel.){
//            throw new ClientDisconnectException("Client Disconnected");
//        }
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by client
            key.cancel();
            clientChannel.close();
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
            throw new ClientDisconnectException("Client has been disconnected");
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
                throw new InvalidMessageException("Message being parsed is invalid");
            }
            if(messageParsed.getType().equals(MessageType.INITIALISATION)){//on initialisation you add to the connected clients
                addConnectedClient(clientChannel, messageParsed.getSenderId());
            }
            return messageParsed;
        }
        throw new InvalidMessageException("Message is empty");
    }

    public void addConnectedClient(SocketChannel clientChannel, String username){
        //TODO make system to read rooms that client is in from saved
        //TODO make check that client current room isn't null else where as it will be null here
        if(connectedClients.containsKey(username)){
            return;
        }
        ClientData clientData = new ClientData(username, clientChannel, new HashSet<>());
        connectedClients.put(username, clientData);
    }

    public void writeDataToClient(SocketChannel clientChannel, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        clientChannel.write(buffer);
        System.out.println("Messaged Client");
    }


}