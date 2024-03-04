package client;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class RoomStorage {
    private final BlockingQueue<List<String>> roomQueue = new LinkedBlockingQueue<>();

    // Producer adds a list of rooms to the queue
    public void addRooms(List<String> rooms) {
        try {
            roomQueue.put(rooms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }//TODO rooms may get blocked and not added if server never returns them/crashes

    // Consumer retrieves a list of rooms from the queue with a timeout
    public List<String> getRooms(long timeout, TimeUnit unit) {
        try {
            return roomQueue.poll(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
