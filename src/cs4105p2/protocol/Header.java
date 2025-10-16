package cs4105p2.protocol;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.exception.MessageEncodeException;
import cs4105p2.util.Timestamp;

/**
 * General message format.
 * <message> := ":" <header> ":" <payload> ":"
 * 
 * <header> := <identifier> ":" <serial-number> ":" <timestamp>
 * <identifier> := uid "@" fqdn
 * 
 * # uid as from Java's System.getProperty.("user.name")
 * # fqdn as from Java's InetAddress.getLocalHost().getCanonicalHostName();
 * # Examples: saleem@my.host1.net | my-id@my.host2.net
 * 
 * <serial-number> := *DIGIT
 * # A non-zero, unsigned decimal number to help the sender identify / demux
 * # responses to messages: this value is used by a server in responses.
 * # Examples: 42 101 528491
 */

public class Header {
    private static final int MAX_16BIT = 0xFFFF; // 16-bit maximum value (65,535)
    private static int initialSerialNumber = new Random().nextInt(MAX_16BIT)  + 1;
    private static int nextSerialNumber() {
        do{
            initialSerialNumber = (initialSerialNumber + 1) & MAX_16BIT;
        } while (initialSerialNumber == 0);
        return initialSerialNumber; // Apply wrapping using bitwise AND
    }
    
    // <header> := <identifier> ":" <serial-number> ":" <timestamp>
    // e.g. saleem@my.host1.net:528491:20231011-174242.042
    private static final Pattern pattern = Pattern.compile(
        "^(.+):(\\d+):(.+)$");
        // <timestamp> := <year> <month> <day> "-" <hours> <minutes> <seconds> "." <milliseconds>
    // Examples: 20240912-174242.042
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
    {sdf.setLenient(false);}
    private String identifier;
    private int serialNumber;
    private String timestamp;

    public Header(String identifier) 
    throws MessageEncodeException {
        // serialNumber is a non-zero, unsigned, 16-bit decimal number,
        if (
            identifier == null
        ) throw new MessageEncodeException("Encode err: Header");
        this.identifier = identifier;
        this.serialNumber = nextSerialNumber();
        this.timestamp = sdf.format(new Date()); // now
    }

    public Header(String identifier, int serialNumber, String timestamp)
    throws MessageEncodeException {
        if (identifier == null
            || serialNumber < 1
            || serialNumber> MAX_16BIT
            || !Timestamp.isValid(timestamp)
        ) {throw new MessageEncodeException("Encode err: Header");}

        this.identifier = identifier;
        this.serialNumber = serialNumber;
        this.timestamp = timestamp;
    }

    public Header(String identifier, String serialNumber, String timestamp)
    throws MessageEncodeException {
        this(identifier, Integer.parseInt(serialNumber), timestamp);
    }

    public String encode(){
        return String.format("%s:%d:%s", identifier, serialNumber, timestamp);
    }

    /** String format to message format */
    @Override
    public String toString() {
        return encode();
    }

    /**
     * static method
     * 
     * to be modified
     */
    public static Header decode(String header) throws Exception {
        Matcher matcher = pattern.matcher(header);
        if(!matcher.matches()) {
            throw new MessageEncodeException("header encoder err");
        }

        return new Header(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    /** */
    public String getIdentifier() {
        return identifier;
    }

    /** */
    public int getSerialNumber() {
        return serialNumber;
    }

    /** */
    public String getTimestamp() {
        return timestamp;
    }

    public String getFqdn() {
        return identifier.split("@")[1];
    }

    public String getUid() {
        return identifier.split("@")[0];
    }
    
}
