package cs4105p2.protocol.payload;

import cs4105p2.protocol.Payload;
import cs4105p2.protocol.exception.MessageDecodeException;

public class MqfdnsAnswer extends Payload {
    private int ttl;
    private String ipv6Address;

    public MqfdnsAnswer(int ttl, String ipv6Address) {
        this.ttl = ttl;
        this.ipv6Address = ipv6Address;
    }

    @Override
    public String encode() {
        return String.format("mfqdns-answer:%d:%s", getTTL(), escape(getIpv6Address()));
    }

    @Override
    public String getType() {
        return "mfqdns-answer";
    }

    @Override
    public int getTTL() {
        return ttl;
    };

    @Override
    public String getIpv6Address() {
        return ipv6Address;
    };

    public static Payload decode(String payload)
    throws MessageDecodeException
    {
        String[] fields = payload.split(":");
        if(fields.length != 3)
            throw new MessageDecodeException("MqfdnsAnswer.decode(): " +payload); 
        int ttl = Integer.parseInt(fields[1]);
        String ipv6Address = unescape(fields[2]);
        
        return new MqfdnsAnswer(ttl, ipv6Address);
    }
    
}
