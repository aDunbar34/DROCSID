import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class InputRunner implements Runnable {
  private Socket socket = null;

  public InputRunner(Socket socket) {
    this.socket = socket;
  }

  public void run() {
    try {
      System.out.println("Enter a message:");
      BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      // Reads string from client and sends it back to the client String inputLine;
      String message;
      while ((message = stdIn.readLine()) != null) {
        if (message != null) {
          out.println(message);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
