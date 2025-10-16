package cs4105p2.protocol.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

/**
 * <search-message> := <search-request> / <search-response>
 * <search-request> := "search-request" ":" <search-string>
 * # All searches should be matched using a case-insensitive, sub-string match.
 * # The sub-string matches any part of the full logical path-name of the file.
 * 
 * <search-string> = file-string
 * # file-string is any group of characters that can be used for a
 * # the substring match of a full logical path-name for a file.
 * 
 * # Example of a complete search request, with header:
 * #
 * # :saleem@my.host1.net:1001:20240912-170101.001:search-request:hello:
 */
public class SearchRequest extends Payload {
    // format check regex: "search-request" ":" <search-string>
    // e.g. search-request:hello
    private static final Pattern pattern = Pattern.compile(
            "^search-request:(.+)$");
    private String searchString;

    public SearchRequest(String searchString) {
        this.searchString = searchString;
    }

    /** String should format to message format */
    @Override
    public String encode() {
        return String.format("search-request:%s", searchString);
    }

    /** */
    @Override
    public String getSearchString() {
        return searchString;
    }

    @Override
    public String getType() {
        return "search-request";
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
            throw new MessageDecodeException("SearchRequest.decode(): " + payload);
        }

        String searchString = matcher.group(1);

        return new SearchRequest(searchString);
    }

}
