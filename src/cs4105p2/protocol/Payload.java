package cs4105p2.protocol;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.channels.NetworkChannel;

import cs4105p2.protocol.exception.MessageDecodeException;
import cs4105p2.protocol.payload.Advertisement;
import cs4105p2.protocol.payload.DownloadError;
import cs4105p2.protocol.payload.DownloadRequest;
import cs4105p2.protocol.payload.DownloadResult;
import cs4105p2.protocol.payload.MqfdnsAnswer;
import cs4105p2.protocol.payload.MqfdnsQuery;
import cs4105p2.protocol.payload.SearchError;
import cs4105p2.protocol.payload.SearchRequest;
import cs4105p2.protocol.payload.SearchResult;

/**
 *   <payload> := <advertisement-message> / <search-message> / <download-message> 
 */
public abstract class Payload {
    /** String should format to message format */
    public abstract String encode();

    public int getServerPort() {
        return 0;
    }

    public boolean isSearchEnabled() {
        return false;
    }

    public boolean isDownloadEnabled() {
        return false;
    }

    public String getSearchString() {
        return null;
    }

    public String getFileString(){
        return null;
    }

    public long getContentLength() {
        return 0;
    }

    public ResponseId getResponseId() {
        return null;
    }

    public String getFilePath() {
        return null;
    }

    public String getFqdn() {
        return null;
    };

    public int getTTL() {
        return 0;
    };

    public String getIpv6Address() {
        return null;
    };

    public abstract String getType();

    /** String should format to message format */
    @Override
    public String toString() {
        return encode();
    }

    static public String escape(String str) {
        return str.replace(":", "%3A");
    }

    static public String unescape(String str) {
        return str.replace("%3A", ":");
    }
    
    // resolve different type msgs
    public static Payload decode(String payload)
    throws MessageDecodeException {
        String type = payload.split(":")[0];
        switch (type) {
            // <advertisement-message> := "advertisement" ":" <server-port> : <services>
            // advertisement:10123:search=true,download=false
            case "advertisement":
                return Advertisement.decode(payload);

            // <search-request> := "search-request" ":" <search-string>
            // <search-string> = file-string
            // search-request:hello
            case "search-request":
                return SearchRequest.decode(payload);

            // <search-result> := "search-result" : <response-id> : file-string
            // <response-id> := <identifier> ":" <serial-number>
            // search-result:saleem@my.host1.net:1001:/dir1/dir2/hello_world.txt
            case "search-result":
                return SearchResult.decode(payload);

            // <search-error> := "search-error" ":" <response-id>
            // search-error:saleem@my.host1.net:1001
            case "search-error":
            
                return SearchError.decode(payload);

            case "mfqdns-query":
                return MqfdnsQuery.decode(payload);

            case "mfqdns-answer":
                return MqfdnsAnswer.decode(payload);

            case "download-request":
            
                return DownloadRequest.decode(payload);

            case "download-result":
                return DownloadResult.decode(payload);

            case "download-error":
                return DownloadError.decode(payload);
            default:
                throw new MessageDecodeException("Unknown payload type");
        }
    }
}
