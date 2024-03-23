package server;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;


import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The endpoint of the WebSockets server for WebRTC Signalling
 *
 * @author Euan Gilmour
 */
@WebSocket
public class WebSocketServerEndpoint {

    private ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WEBSOCKETS: New connection from " + session.getRemoteAddress().toString());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        System.out.println("WEBSOCKETS: Connection closed from " + session.getRemoteAddress().toString());
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        System.out.println("WEBSOCKETS: ERROR");
        cause.printStackTrace();
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            JsonNode jsonMessage = objectMapper.readTree(message);

            switch(jsonMessage.get("type").asText()) {
                case "INTRODUCTION" -> {
                    String name = jsonMessage.get("name").asText();
                    sessions.put(name, session);
                    System.out.println("WEBSOCKETS: Session " + session.getRemoteAddress().toString() + " introduced as '" + name + "'");
                }
                case "HEARTBEAT" -> {
                    String name = jsonMessage.get("name").asText();
                    ObjectNode response = objectMapper.createObjectNode();
                    response.put("type", "HEARTBEAT");
                    response.put("name", name);
                    response.put("connected", sessions.containsKey(name));
                    session.getRemote().sendString(response.toString());
                    System.out.println("WEBSOCKETS: Heartbeat message received from session " + session.getRemoteAddress().toString() + " about user " + name);
                }
            }
        } catch (JsonProcessingException e) {
            System.out.println("WEBSOCKETS: Invalid JSON sent from session " + session.getRemoteAddress().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
