package customExceptions;

/**
 * Exception that would get thrown by the server for when a client disconnected
 *
 * @author Robbie Booth
 */
public class ClientDisconnectException extends Exception {
    public ClientDisconnectException(String errorMessage) {
        super(errorMessage);
    }
}