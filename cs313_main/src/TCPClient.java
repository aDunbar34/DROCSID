
import java.net.*;

public class TCPClient implements Runnable {
    private String hostName;
    private int portNumber;

    public TCPClient(String hostName, int portNumber) {
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    public static void main(String[] args) {
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        try (Socket clientSocket = new Socket(hostName, portNumber)) {
            //Create way of inputting that is capable of threading
            InputRunner inputRunner = new InputRunner(clientSocket);
            //Create way of outputting that is capable of threading
            OutputRunner outputRunner = new OutputRunner(clientSocket);
            //Create the threads
            Thread outputThread = new Thread(outputRunner);
            Thread inputThread = new Thread(inputRunner);
            //start the threads
            outputThread.start();
            inputThread.start();
            //continue whilst the threads are alive/keep program alive
            while(outputThread.isAlive() && inputThread.isAlive()) {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

    }
}
