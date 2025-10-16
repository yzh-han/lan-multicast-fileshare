package cs4105p2.service;

import java.util.Date;

import cs4105p2.util.Timestamp;

public class Node {
    private String id;
    private String uid;
    private String fqdn;
    private int serverPort;
    private boolean search; // from searchOptions_
    private boolean download;
    private Date lastSeen;
    private int TTL;


    public Node(String id, int serverPort, boolean search, boolean download){
        this.id = id;
        this.uid = id.split("@")[0];
        this.fqdn = id.split("@")[1];
        this.serverPort =serverPort;
        this.search = search;
        this.download = download;
        this.lastSeen = new Date(); // now
        this.TTL = 0;
    }

    public Node(String id, int serverPort, boolean search, boolean download, String timestamp, int TTL) {
        this(id, serverPort, search, download);
        this.lastSeen = Timestamp.parse(timestamp);
        this.TTL = TTL;
    }

    public boolean isExpired() {
        return new Date().getTime() - lastSeen.getTime() > TTL;
    }

    // 获取服务端口
    public String getId() {
        return id;
    }

    // 获取服务端口
    public String getUid() {
        return uid;
    }

    // 获取服务端口
    public String getFQDN() {
        return fqdn;
    }

    // // 获取服务端口
    // public InetAddress[] getIp6() {
    //     try {
    //         return Inet6Address.getAllByName(fqdn);
    //     } catch (UnknownHostException e) {
    //         e.printStackTrace();
    //     }
    // }

    // 获取服务端口
    public int getServerPort() {
        return serverPort;
    }

    public String getLastSeen() {
        return Timestamp.toTimestamp(lastSeen);
    }

    // 获取 search 服务状态
    public boolean isSearchEnabled() {
        return search;
    }

    // 获取 download 服务状态
    public boolean isDownloadEnabled() {
        return download;
    }

    // 设置 search 服务状态
    public void setSearch(boolean status) {
        this.search = status;
    }

    // 设置 download 服务状态
    public void setDownload(boolean status) {
        this.download = status;
    }
}
