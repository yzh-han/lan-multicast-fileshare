package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

public class DownloadResult extends Payload {
    private static final Pattern pattern = Pattern.compile(
    "^download-result:(.+)$");

    private long contentLength;

    public DownloadResult(long contentLength) {
        this.contentLength = contentLength;
    }

    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("download-result:%d", contentLength);
    }

    /** */
    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public String getType() {
        return "download-result";
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
            throw new MessageDecodeException("DownloadResult.decode(): " + payload);
        }

        String contentLength = matcher.group(1);

        return new DownloadResult(Integer.parseInt(contentLength));
    }
}
