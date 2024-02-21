package stage1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class PeerOutput implements Runnable {
    private Socket socket = null;

    public PeerOutput(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("DROCSID user> " + inputLine);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
