package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

/**
 * <search-error> := "search-error" ":" <response-id>
 * # A <response-id> would be used in a response messages to match requests to
 * # responses so that multiple requests can be outstanding to the same server.
 * <response-id> := <identifier> ":" <serial-number>
 * # <identifier> and <serial-number> as defined above.
 * # In any responses, a response-id uses the values of identifier and
 * # serial-number from the request. Examples below.
 */
public class SearchError extends SearchResult {
    // format check regex: "search-error" ":" <response-id>
    // e.g. search-error:saleem@my.host1.net:1001
    private static final Pattern pattern = Pattern.compile(
            "^search-error:(.+):(\\d+)$");

    public SearchError(String identifier, int serialNumber) {
        super(identifier, serialNumber, null);
    }

    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("search-error:%s", getResponseId());
    }

    @Override
    public String getType() {
        return "search-error";
    }

    public static Payload decode(String payload)
            throws MessageDecodeException {
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.matches()) {
            throw new MessageDecodeException("SearchError.decode(): " + payload);
        }

        String identifier = matcher.group(1);
        int serialNumber = Integer.parseInt(matcher.group(2));

        return new SearchError(identifier, serialNumber);
    }
}
