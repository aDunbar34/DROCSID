package customExceptions;

/**
 * Exception that would typically be thrown by a server when a message sent to it is invalid
 *
 * @author Robbie Booth
 */
public class InvalidMessageException extends Exception {
    public InvalidMessageException(String errorMessage) {
        super(errorMessage);
    }
}
