package cs4105p2.service;

import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

public class Nodes {
    private ConcurrentHashMap<String, Node> nodes;
        
    public Nodes() {
        nodes = new ConcurrentHashMap<String, Node>();
    }

    public Node getById(String id) {
        return nodes.get(id);
    }
        
    public void update(Node node) {
        nodes.put(node.getId(), node);
    }

    public void remove(String id) {
        nodes.remove(id);
    }

    public void cleanUp() {
        for(Node node : nodes.values()) {
            if (node.isExpired()) nodes.remove(node.getId());
        };
    }
    
    public void printNodes() {
        int index = 0;
        System.out.println("+++\t"
                            + "identifier\t\t\t\t"
                            + "port\t"
                            + "search\t"
                            + "dwnld\t"
                            + "lastseen\t"
                            + ":");

        for (Node node : nodes.values()) {
        index++;
        System.out.println(index + "\t"
            + node.getId() + "\t"
            + node.getServerPort() + "\t"
            + node.isSearchEnabled() + "\t"
            + node.isDownloadEnabled() + "\t"
            + node.getLastSeen() + "\t");
        }
    }

    public int resolvePort(String id) throws UnknownHostException {
        if (nodes.get(id) == null) {
            throw new UnknownHostException("Resolve port err, can't resolve identifier to get port.");
        }
        return nodes.get(id).getServerPort();        
    }

}
