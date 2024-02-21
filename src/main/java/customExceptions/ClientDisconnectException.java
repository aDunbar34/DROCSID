package customExceptions;

public class ClientDisconnectException extends Exception {
    public ClientDisconnectException(String errorMessage) {
        super(errorMessage);
    }
}