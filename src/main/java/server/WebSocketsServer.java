package server;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.time.Duration;

/**
 * A websocket server for use in signalling
 * for WebRTC connections
 *
 * @author Euan Gilmour
 */
public class WebSocketsServer {

    private final Server server;

    public WebSocketsServer() {
        server = new Server(8081);
        ServletContextHandler handler = new ServletContextHandler(server, "/socket");
        server.setHandler(handler);

        JettyWebSocketServletContainerInitializer.configure(handler, (servletContext, container) -> {
            container.addMapping("/", WebSocketServerEndpoint.class);
        });
    }

    public void startServer() {
        try {
            server.start();
            System.out.println("Successfully started WebSockets Server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
