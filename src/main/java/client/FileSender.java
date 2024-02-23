package client;

import java.io.File;

/**
 * Listens for a connection from a peer and
 * sends a file when established
 */
public class FileSender implements Runnable {

    private File file;
    private int portNo;

    public FileSender(File file, int portNo) {
        this.file = file;
        this.portNo = portNo;
    }

    @Override
    public void run() {}

}
