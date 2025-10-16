package cs4105p2.protocol.exception;

public class MessageEncodeException extends Exception {
    public MessageEncodeException(){}

    public MessageEncodeException(String message){
        super(message);
    }
}
