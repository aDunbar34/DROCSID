import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class OutputRunner implements Runnable {
  private Socket socket = null;

  public OutputRunner(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        if (inputLine != null) {
          System.out.println("DORCSID user> " + inputLine);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);

    }

  }
}
