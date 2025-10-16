package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.ResponseId;
import cs4105p2.protocol.exception.MessageDecodeException;

/**
 * <search-message> := <search-request> / <search-response>
 * <search-response> := <search-result> / <search-error>
 * <search-result> := "search-result" : <response-id> : file-string
 * <response-id> := <identifier> ":" <serial-number>
 * e.g /dir1/dir2/hello_world.txt
 */
public class SearchResult extends Payload {
    // format check regex: 
    // <search-result> := "search-result" : <response-id> : file-string
    // e.g. search-result:saleem@my.host1.net:1001:/dir1/dir2/hello_world.txt
    private static final Pattern pattern = Pattern.compile(
            "^search-result:(.+):(\\d+):(.+)$");
    private ResponseId ResponseId;
    private String filePath;

    public SearchResult(String identifier, int serialNumber, String filePath) {
        ResponseId = new ResponseId(identifier, serialNumber);
        this.filePath = filePath;
    }

    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("search-result:%s:%s", getResponseId(), filePath);
    }

    @Override
    public ResponseId getResponseId() {
        return ResponseId;
    }
    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getType() {
        return "search-result";
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
            throw new MessageDecodeException("SearchResult.decode(): " + payload);
        }

        String identifier = matcher.group(1);
        int serialNumber = Integer.parseInt(matcher.group(2));
        String filePath = matcher.group(3);

        return new SearchResult(identifier, serialNumber, filePath);
    }
}
