package cs4105p2.service;

import cs4105p2.util.Timestamp;
import java.util.Date;

public class MfqdnsRR {
    private String fqdn;
    private int ttl;
    private String ipv6Address;
    private Date date;

    public MfqdnsRR(String fqdn, String ttl, String ipv6Address, String date) {
        this(fqdn, Integer.parseInt(ttl), ipv6Address, date);
    }

    public MfqdnsRR(String fqdn, int ttl, String ipv6Address, String date) {
        this.fqdn = fqdn;
        this.ttl = ttl;
        this.ipv6Address = ipv6Address;
        this.date = Timestamp.parse(date);
    }

    public String getFqdn() {
        return fqdn;
    };

    public int getTTL() {
        return ttl;
    };

    public String getIpv6Address() {
        return ipv6Address;
    };

    public boolean isExpired() {
        return new Date().getTime() - this.date.getTime() > ttl; 
    }
}
