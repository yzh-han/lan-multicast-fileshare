package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;
import cs4105p2.service.Service;;

/**
 * <advertisement-message> := "advertisement" ":" <server-port> : <services>.
 * e.g.
 * :saleem@my.host1.net:528491:20231013-174242.042:advertisement:10123:search=true,download=false:
 * 
 * <server-port> := *DIGIT
 * # a non-zero, unsigned, 16-bit decimal number, on which this server
 * # listens for incoming connections, an ephemeral port as allocated
 * # by the OS when ServerSocket() is created.
 * 
 * <services> := <service-name> "=" <service-status> "," <service-name> "="
 * <service-status>
 * # The offered services and their capabilities.
 * # Example:
 * #
 * # search=true,download=false
 * 
 * <service-name> := "search" / "download"
 * 
 * <service-status> := "true" / "false"
 */
public class Advertisement extends Payload {
    static final int MAX_16BIT = 0xFFFF; // 16-bit maximum value (65,535)
    private static final Pattern pattern = Pattern.compile(
        "^advertisement:"
        + "(\\d{1,5}):(search=(true|false),download=(true|false))$");
    // Service service;
    int serverPort;
    boolean searchEnabled;
    boolean downloadEnabled;

    public Advertisement(Service service) {
        // super(service);
        this.serverPort = service.getNode().getServerPort();
        this.searchEnabled = service.getNode().isSearchEnabled();
        this.downloadEnabled = service.getNode().isDownloadEnabled();
    }

    public Advertisement(int serverPort,
                                boolean searchEnabled,
                                boolean downloadEnabled) 
    {
        this.serverPort = serverPort;
        this.searchEnabled = searchEnabled;
        this.downloadEnabled = downloadEnabled;
    }


    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("advertisement:%d:%s",
                             serverPort,
                             generateServiceMessage());
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    @Override    
    public boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public String generateServiceMessage() {
        return String.format("search=%s,download=%s",
                isSearchEnabled() ? "true" : "false",
                isDownloadEnabled() ? "true" : "false");

    }

    public static Payload decode(String payload)
    throws MessageDecodeException{
        Matcher matcher = pattern.matcher(payload);
        if(!matcher.matches()) {
            throw new MessageDecodeException("Decode err: Advertisement");
        }

        int serverPort = Integer.parseInt(matcher.group(1));
        boolean searchEnabled = Boolean.parseBoolean(matcher.group(3));
        boolean downloadEnabled = Boolean.parseBoolean(matcher.group(4));

        if (serverPort < 1 || serverPort > MAX_16BIT) {
            throw new MessageDecodeException("Message Decode err: server port err");
        }

        try {
            return new Advertisement(serverPort, searchEnabled, downloadEnabled);
        } catch(Exception e) {
            throw new MessageDecodeException("Adertisement.decode(): " + payload);
        }
    }

    @Override
    public String getType() {
        return "advertisement";
    }
}
