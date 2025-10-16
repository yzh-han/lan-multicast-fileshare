package cs4105p2.protocol.payload;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

public class MqfdnsQuery extends Payload {
    private String fqdn;

    public MqfdnsQuery(String fqdn) {
        this.fqdn =fqdn;
    }

    @Override
    public String encode() {
        return String.format("mfqdns-query:%s" ,fqdn);
    }

    @Override
    public String getType() {
        return "mfqdns-query";
    }

    @Override
    public String getFqdn() {
        return this.fqdn;
    };

    public static Payload decode(String payload)
    throws MessageDecodeException
    {
        String[] fields = payload.split(":");
        if(fields.length != 2)
            throw new MessageDecodeException("MqfdnsQuery.decode(): " + payload); 
        String fqdn = fields[1];
        
        return new MqfdnsQuery( fqdn);
    }

    
    
}
