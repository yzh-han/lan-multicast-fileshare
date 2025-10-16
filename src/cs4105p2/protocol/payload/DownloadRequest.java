package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

public class DownloadRequest extends Payload {
    private static final Pattern pattern = Pattern.compile(
        "^download-request:(.+)$");

    private String fileString;

    public DownloadRequest(String fileString) {
        this.fileString = fileString;
    }

    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("download-request:%s", fileString);
    }

    /** */
    @Override
    public String getFileString() {
        return fileString;
    }

    @Override
    public String getType() {
        return "download-request";
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
            throw new MessageDecodeException("DownloadRequest.decode(): " + payload);
        }

        String fileString = matcher.group(1);

        return new DownloadRequest(fileString);
    }

}
