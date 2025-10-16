package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

public class DownloadError extends Payload{
    private static final Pattern pattern = Pattern.compile(
    "^download-error$");


    public DownloadError() {
    }

    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("download-error");
    }


    @Override
    public String getType() {
        return "download-error";
    }

    /**
     * 
     * @param payload
     * @return
     * @throws MessageDecodeException
     */
    public static Payload decode(String payload)
        throws MessageDecodeException {
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.matches()) {
            throw new MessageDecodeException("DownloadError.decode(): " + payload);
        }


        return new DownloadError();
    }
}
