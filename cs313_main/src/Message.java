import java.io.Serializable;

/// This class represents a message sent by a user
/// to the server
public class Message implements Serializable {

  private final String content;
  private final String senderId;

  public Message(String content, String senderId) {
    this.content = content;
    this.senderId = senderId;
  }

  public String get_content() {
    return this.content;
  }

  public String getSenderId() {
    return this.senderId;
  }

}
