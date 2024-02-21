package customExceptions;

/**
 * Exception thrown by the server when a user is not found
 * @author Robbie Booth
 */
public class UserNotFoundException extends Exception {
    public UserNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
