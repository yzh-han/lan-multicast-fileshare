package cs4105p2.protocol;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import cs4105p2.protocol.exception.MessageDecodeException;


public class Message {
    // format check regex: <message> := ":" <header> ":" <payload> ":"
    private static final Pattern messagePattern = Pattern.compile(
        "^:" 
        + "(?<header>[^:]+:[^:]+:[^:]+)"
        + ":"
        + "(?<payload>.+)"
        + ":$" );
    private Header header;
    private Payload payload;

    public Message(Header header, Payload payload) {
        this.header = header;
        this.payload = payload;
    }

    // return a complete message，includ header and payload
    @Override
    public String toString() {
        if ( header.encode() == null || payload.encode() == null) {
            return null;
        }

        return ":" + header.encode() + ":" + payload.encode() + ":";
    }

    public Header getHeader() {
        return header;
    }

    public Payload getPayload() {
        return payload;
    }

    




    public static Message decode(String message) throws Exception {
        Matcher matcher = messagePattern.matcher(message);
        
        if (!matcher.matches()) {
            throw new MessageDecodeException("Decode err: ProtocolMessage");
        }

        Header header = Header.decode(matcher.group("header"));
        Payload payload = Payload.decode(matcher.group("payload"));
        return new Message(header, payload);
    }


}


