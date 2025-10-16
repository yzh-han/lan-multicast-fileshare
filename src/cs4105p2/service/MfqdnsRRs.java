package cs4105p2.service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

public class MfqdnsRRs {
    ConcurrentHashMap<String, MfqdnsRR> records;

    public MfqdnsRRs() {
        records = new ConcurrentHashMap<>();
    }

    public void update(MfqdnsRR record) {
        records.put(record.getFqdn(), record);
    }

    // mfqdnsAnswer: 
    public void update(String fqdn, int ttl, String ipv6Address, String date) {
        MfqdnsRR record = new MfqdnsRR(fqdn, ttl, ipv6Address, date);
        records.put(record.getFqdn(), record);
    }

    public void remove(String fqdn) {
        records.remove(fqdn);
    }

    public InetAddress resolveFqdn(String fqdn) throws UnknownHostException {
        MfqdnsRR record = records.get(fqdn);
        if (record == null) {
            throw new UnknownHostException("MfqdnsRRs.resolveFqdn(): record = null");
        }
        InetAddress ipv6 = InetAddress.getByName(record.getIpv6Address());
        if (!(ipv6 instanceof Inet6Address)) {
            throw new UnknownHostException("MfqdnsRRs.resolveFqdn():" + record.getIpv6Address());
        }
        return ipv6;
    }

    public void cleanUp() {
        for(MfqdnsRR record : records.values()) {
            if (record.isExpired()) records.remove(record.getFqdn());
        };
    }

    public void printRRs() {
        int index = 0;
        System.out.println("+++\t"
                            + "fqdn\t\t\t\t"
                            + "ipv6\t"
                            + ":");

        for (MfqdnsRR record : records.values()) {
        index++;
        System.out.println(index + "\t"
            + record.getFqdn() + "\t"
            + record.getIpv6Address() + "\t");
        }
    }

}
