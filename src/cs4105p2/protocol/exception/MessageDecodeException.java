package cs4105p2.protocol.exception;

public class MessageDecodeException extends Exception {
    public MessageDecodeException(){}

    public MessageDecodeException(String message){
        super(message);
    }
}
