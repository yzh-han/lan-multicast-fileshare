package cs4105p2.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import cs4105p2.Configuration;
import cs4105p2.multicast_ipv6_link_local.MulticastEndpoint;
import cs4105p2.protocol.Header;
import cs4105p2.protocol.Payload;
import cs4105p2.protocol.Message;
import cs4105p2.protocol.exception.MessageEncodeException;
import cs4105p2.protocol.payload.Advertisement;
import cs4105p2.protocol.payload.MqfdnsAnswer;
import cs4105p2.protocol.payload.MqfdnsQuery;
import cs4105p2.protocol.payload.SearchError;
import cs4105p2.protocol.payload.SearchRequest;
import cs4105p2.protocol.payload.SearchResult;

/**
 * Core service class that manages distributed file sharing functionality over IPv6.
 * Handles node discovery, file searching, and download operations using both multicast
 * and TCP communications.
 */
public class Service {
    static final int MAX_16BIT = 0xFFFF; // 16-bit maximum value (65,535)

    private Configuration c; // config info
    private MulticastEndpoint m; // multicast socket + config info

    //tcp socket
    private ServerSocket tcpServer;

    //this node
    private Node node;      // this node
    public String search;   // this node status from searchOptions_

    // Cache of nodes in group
    private Nodes nodeCache;

    // Cache of fqdn <-> ipv6
    private MfqdnsRRs fqdnCache;


    /**
     * Creates a new service instance with default search and download settings from configuration.
     * 
     * @param serverPort TCP port number for the service (1-65535)
     * @param c Configuration object containing service settings 
     */
    public Service(int serverPort, Configuration c) {
        this(serverPort, c, c.search, c.download);
    }

    /**
     * Creates a new service instance with specified search and download settings.
     * 
     * @param serverPort TCP port number for the service (1-65535)
     * @param c Configuration object containing service settings
     * @param search Search capability setting (none/path/...)
     * @param download Whether download capability is enabled
     * @throws IllegalArgumentException if serverPort or search options are invalid
     */
    public Service(int serverPort, Configuration c, String search, boolean download) {
        if (!c.checkOption(search, c.searchOptions)
            || serverPort < 1 || serverPort > MAX_16BIT) {
            throw new IllegalArgumentException("Service(): illegal argument");
        }
        this.c = c;
        this.m = new MulticastEndpoint(this.c);
        this.search = search;

        System.out.println("search --- " + search + !search.equals("none"));
        this.node = new Node(c.id, serverPort, !search.equals("none") , download);
        this.nodeCache = new Nodes();

        this.fqdnCache = new MfqdnsRRs();
    }



    // node service on this host status changes

    public Node getNode() {
        return this.node;
    }

    public boolean isDownloadEnabled() {
        return this.node.isDownloadEnabled();
    }

    public String getFQDN() {
        return this.node.getFQDN();
    }

    public void setSearch(String value) {
        if (!c.checkOption(value, c.searchOptions)) {
            throw new IllegalArgumentException("Service(): illegal argument");
        }

        this.search = value;
        this.node.setSearch(!value.equals("none"));
    }

    public void setDownload(boolean status) {
        this.node.setDownload(status);
    }
    
    // Multicast Group Management

    /**
     * Joins the IPv6 multicast group for node discovery and messaging.
     */
    public void join() {
        m.join();
    }

    /**
     * Leaves the IPv6 multicast group and cleans up resources.
     */
    public void leave() {
        m.leave();
    }

    // Messaging Operations

    /**
     * Broadcasts an advertisement message containing this node's capabilities.
     * Advertisement includes server port, search and download capabilities.
     */
    public void advertise() {
        String msg = "";
        try {
            msg = "" + new Message(new Header(node.getId()),
                    new Advertisement(this));

            txMessage(msg, "Ad");
        } catch (MessageEncodeException e) {
            System.out.println("Service.advertise(): " + e.getMessage());
        }
    }

    /**
     * Initiates a file search request across the network.
     *
     * @param fileString The search string to match against file paths
     * @return The serial number of the search request, or 0 if request failed
     */
    public int search(String fileString) {
        String msg = "";
        int serialNumber = 0;
        Header header;
        Payload payload;
        try {
            header = new Header(node.getId());
            payload = new SearchRequest(fileString);
            msg = "" + new Message(header, payload);
            txMessage(msg, "SReq");
            serialNumber = header.getSerialNumber();
        } catch (MessageEncodeException e) {
            System.out.println("Service.search(): " + e.getMessage());
        }

        return serialNumber;
    }

    /**
     * Sends a successful search response containing matching file path.
     *
     * @param identifier The requesting node's identifier
     * @param serialNumber The original search request's serial number
     * @param filePath The matching file's path
     */
    public void response(String identifier, int serialNumber, String filePath) {
        String msg = "";
        try {
            msg = "" + new Message(new Header(node.getId()),
                    new SearchResult(identifier,
                            serialNumber,
                            filePath));
            txMessage(msg, "SRes");
        } catch (MessageEncodeException e) {
            System.out.println("Service.response(): " + e.getMessage());
        }
    }

    /**
     * Sends a search error response when no matches are found.
     *
     * @param identifier The requesting node's identifier
     * @param serialNumber The original search request's serial number
     */
    public void response(String identifier, int serialNumber) {
        String msg = "";
        try {
            msg = "" + new Message(new Header(node.getId()),
                                    new SearchError(identifier, serialNumber));
            txMessage(msg, "SErr");
        } catch (MessageEncodeException e) {
            System.out.println("Service.response(): " + e.getMessage());
        }
    }

    public void mfqdnQuery(String fqdn) {
        String msg = "";
        try {
            msg = "" + new Message(new Header(node.getId()),
                                    new MqfdnsQuery(fqdn));
            txMessage(msg, "Query");
        } catch (MessageEncodeException e) {
            System.out.println("Service.query(): " + e.getMessage());
        }
    }

    public void mfqdnAnswer() {
        String msg = "";
        try {
            msg = "" + new Message(new Header(node.getId()),
                                    new MqfdnsAnswer(c.mfqdnTTL, c.ipAddr6));
            txMessage(msg, "Answer");
        } catch (MessageEncodeException e) {
            System.out.println("Service.query(): " + e.getMessage());
        }
    }

    private void txMessage(String msg, String logHint) {
        byte[] b = null;

        try {
            b = msg.toString().getBytes("US-ASCII");
            if (m.tx(MulticastEndpoint.PktType.ip6, b)) {
                    c.log.writeLog("tx" + logHint + "->\t" + msg, c.isTest);
                }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Service.txMessage(): " + e.getMessage());
        }
    }

    private void txMessage(String msg) {
        txMessage(msg, "");
    }


    // rx msg
    public String rxMessage() {
        String msg = "";
        try {
            byte[] b = new byte[c.maximumMessageSize];

            // recive m.rx(b)
            MulticastEndpoint.PktType p = m.rx(b);

            if (p == MulticastEndpoint.PktType.none
                    || p == MulticastEndpoint.PktType.ip4)
                return msg;

            String logRequest = "";
            if (p == MulticastEndpoint.PktType.ip6)
                msg = msg + new String(b, "US-ASCII").trim();
            logRequest = "->rx\t";

            logRequest = logRequest + msg;
            c.log.writeLog(logRequest, c.isTest);
        }

        catch (UnsupportedEncodingException e) {
            System.out.println("Service.rxMessage(): " + e.getMessage());
        }

        return msg;

    }

    // TCP Operations

    /**
     * Starts a TCP server for handling file download requests.
     * Binds to the configured IPv6 address and port.
     */
    public void startTcpServer() {
        try {
            tcpServer = new ServerSocket(getNode().getServerPort(), 1, InetAddress.getByName(c.ipAddr6));
            tcpServer.setSoTimeout(c.soTimeout);
            c.log.writeLog("Starting IPv6 tcp server on link-local address: " + c.ipAddr6, true);
            c.log.writeLog("Starting IPv6 tcp server on port: " + getNode().getServerPort(), true);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the TCP server and releases resources.
     */
    public void closeTcpServer() {
        if (tcpServer != null && !tcpServer.isClosed()) {
            try {
                tcpServer.close();
                c.log.writeLog("IPv6 tcp server close.", true);
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    /**
     * Transmits a single line of text over TCP connection.
     *
     * @param out PrintWriter for the TCP connection
     * @param string Text to transmit
     */
    public void tcpTxLine(PrintWriter out, String string) {
        String logRequest = "";
        out.println(string);
        out.flush();
        // log
        logRequest = "tcpTx->\t" + string;

        // write log
        c.log.writeLog(logRequest, c.isTest);
    }

    /**
     * Transmits a file over TCP connection.
     *
     * @param os Output stream for the TCP connection
     * @param file File to transmit
     */
    public void tcpTxFile(OutputStream os, File file) {
        try (
            FileInputStream fis = new FileInputStream(file);
        ) {
            byte[] buffer = new byte[c.maximumMessageSize];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String tcpRxLine(BufferedReader in) {
        String line = "";
        String logRequest ="";
        try {
            line = line + in.readLine();
            // log
            logRequest = "->tcpRx\t" + line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write log
        c.log.writeLog(logRequest, c.isTest);
        return line;
    }

    public void tcpRxFile(InputStream in, File downloadFilePath) {
        try (
            FileOutputStream out = new FileOutputStream(downloadFilePath);
        ) {
            // input buffer
            byte[] buffer = new byte[c.maximumMessageSize];
            int b = 0;

            // read form tcp, and write to file
            while ((b = in.read(buffer)) != -1) {
                out.write(buffer, 0, b);
                out.flush();
                System.out.print("+");
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerSocket getServer() {
        return tcpServer;
    }


    // Node Cache Management    
    /**
     * 
     * @param node
     */
    public synchronized Node resolveNodeById(String id) {
        return nodeCache.getById(id);
    }

    /**
     * Updates the node cache with information about a discovered node.
     *
     * @param node Node information to cache
     */
    public synchronized void updateNode(Node node) {
        nodeCache.update(node);
    }

    /**
     * Removes expired nodes from the cache.
     */
    public synchronized void cleanUpNodeCache() {
        nodeCache.cleanUp();
    }

    public int resolvePort(String id) throws UnknownHostException {
        return nodeCache.resolvePort(id);
    }

    public void printNodesCache() {
        nodeCache.printNodes();
    }

    // FQDN Resolution

    /**
     * Updates the FQDN to IPv6 address mapping cache.
     *
     * @param fqdn Fully qualified domain name
     * @param ttl Time-to-live in milliseconds
     * @param ipv6Address IPv6 address string
     * @param date Timestamp of the mapping
     */
    public synchronized void updateFqdnCache(String fqdn, int ttl, String ipv6Address, String date) {
        fqdnCache.update(fqdn, ttl, ipv6Address, date);
    }

    public synchronized void cleanUpNFqdnCache() {
        fqdnCache.cleanUp();
    }

    /**
     * Resolves an FQDN to its IPv6 address using the cache.
     *
     * @param fqdn Fully qualified domain name to resolve
     * @return Resolved InetAddress
     * @throws UnknownHostException if FQDN cannot be resolved
     */
    public InetAddress resolveFqdn(String fqdn) throws UnknownHostException{
        return fqdnCache.resolveFqdn(fqdn);
    }

    public void printFqdnCache() {
        fqdnCache.printRRs();
    }
}
