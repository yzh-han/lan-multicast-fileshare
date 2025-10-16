package cs4105p2.protocol;

/**
 * A <response-id> would be used in a response messages to match requests to
 * responses so that multiple requests can be outstanding to the same server.
 * <response-id> := <identifier> ":" <serial-number>
 * e.g. my-id@my.host2.net:528491
 */
public class ResponseId {
    private String identifier;
    private int serialNumber;

    public ResponseId(String identifier, int serialNumber) {
        // check: serial-number should be a non-zero, unsigned decimal number
        if (serialNumber < 1) {
            throw new IllegalArgumentException(
                    "serialNumber should be a non-zero, unsigned decimal number");
        }
        this.identifier = identifier;
        this.serialNumber = serialNumber;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    /** String should format to message format */
    @Override
    public String toString() {
        return String.format("%s:%d", getIdentifier(), getSerialNumber());
    }
}
