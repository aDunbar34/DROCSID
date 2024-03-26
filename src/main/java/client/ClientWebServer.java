package client;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 * A web server that hosts the web app for streaming
 *
 * @author Euan Gilmour
 */
public class ClientWebServer {

    private final Server server;

    public ClientWebServer() {
        this.server = new Server(8080);
    }

    public void startServer() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("./webResources");
        resourceHandler.setWelcomeFiles(new String[]{"app.html"});

        server.setHandler(resourceHandler);

        try {
            server.start();
            System.out.println("Web server successfully started.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
