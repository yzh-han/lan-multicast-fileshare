package cs4105p2;
/**
 * Browses a file tree with a simple text based output and navigation.
 *
 * @author   <a href="https://saleem.host.cs.st-andrews.ac.uk/">Saleem Bhatti</a>
 * @version  1.5, 18 September 2024
 * 18 Sep 2024, checked with java 21 on CS Lab Linux machines. 
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException; // used once network code is added
import java.text.SimpleDateFormat;
import java.util.List;

import cs4105p2.util.ByteReader;
import cs4105p2.util.FileSearcher;
import cs4105p2.util.Timestamp;
import cs4105p2.protocol.Header;
import cs4105p2.protocol.Message;
import cs4105p2.protocol.Payload;
import cs4105p2.protocol.payload.DownloadError;
import cs4105p2.protocol.payload.DownloadRequest;
import cs4105p2.protocol.payload.DownloadResult;
import cs4105p2.service.Node;
import cs4105p2.service.Service;

public final class FileTreeBrowser {
  // user commands
  final static String quit = new String(":quit");
  final static String help = new String(":help");
  final static String services = new String(":services");
  final static String up = new String("..");
  final static String list = new String(".");
  final static String nodes = new String(":nodes");
  final static String search = new String(":search");
  final static String download = new String(":download");

  final static String propertiesFile = "config/configuration.properties";
  static Configuration configuration;
  static String rootPath = "";

  File thisDir; // this directory
  String thisDirName; // name of this directory
  SimpleDateFormat sdf;

  static private volatile boolean isQuitting = false;
  static private volatile int searchSeriaNumber = 0;
  static private volatile int searchResultCount = 0;
  static private volatile boolean isSendingFqdnAnswer = false;
  static private volatile int advertisementPeriod;

  

  static Service service;

  static Thread advertise;
  static Thread rx;
  static Thread tcpRx;
  /**
   * Create a path relative to the logical root directory.
   * 
   * @param pathName : a full/canonical path for a file/directory.
   */
  static String logicalPathName(String pathName) {
    String p = pathName.replace(rootPath, "");
    if (p == "")
      p = "/";
    return p;
  }

  /**
   * @param args : no args required
   */
  public static void main(String[] args) {

    configuration = new Configuration(propertiesFile);
    rootPath = getPathName(new File(configuration.rootDir));
    advertisementPeriod = configuration.initialAdvertisementPeriod;

    InputStream keyboard = System.in;
    String userCmd = new String(list);

    // 1. initialize FileTreeBrowser
    FileTreeBrowser ftb = new FileTreeBrowser(configuration.rootDir);
    ftb.printList();      // pring browser

    // 2. initialize node service on this host
    //  1)  start node service on this host
    service = new Service(configuration.mPort, configuration);

    //  2)  initialize Control Plane service
    service.join();                     //  2a) join muticast group
    startAdvertise(service);            //  2b) start node advertise
    startRx(service);                   //  2c) start rx muticast msgs

    //  3a) TcpServer
      service.startTcpServer();         // 3a1) start TcpServer
      startTcpRx(service.getServer());  // 3a2) accepting connection

    // 4. waiting command
    while (!isQuitting()) {

      System.out.print("\n[filename | '" + list + "' | '" + up + "' | '" + services + "' | '" + nodes + "' | '" + search
          + "' | '" + download + "' | '" + quit + "' | '" + help + "'] ");

      // what does the user want to do?
      while ((userCmd = ByteReader.readLine(keyboard)) == null) {
        try {
          Thread.sleep(configuration.sleepTime);
        } catch (InterruptedException e) {
        } // Thread.sleep() - do nothing
      }

      // blank
      if (userCmd.isBlank()) {
        continue;
      }

      // quit
      if (userCmd.equalsIgnoreCase(quit)) {
        quit();
      }

      // help message
      else if (userCmd.equalsIgnoreCase(help)) {
        displayHelp();
      }

      // service info
      else if (userCmd.equalsIgnoreCase(services)) {
        displayServices();
      }

      // list files ".""
      else if (userCmd.equalsIgnoreCase(list)) {
        ftb.printList();
      }

      // move up directory tree ".."
      else if (userCmd.equalsIgnoreCase(up)) {
        // move up to parent directory ...
        // but not above the point where we started!
        if (ftb.thisDirName.equals(rootPath)) {
          System.out.println("At root : cannot move up.\n");
        } else {
          String parent = ftb.thisDir.getParent();
          System.out.println("<<< " + logicalPathName(parent) + "\n");
          // ftb = new FileTreeBrowser(parent);
          ftb.updateFTB(parent);
        }
      }

      // :nodes - list discovered servers
      else if (userCmd.equalsIgnoreCase(nodes)) {
        // clean up node cache
        service.cleanUpNodeCache();
        // print node cache
        service.printNodesCache();
      }

      // :search - 
      else if (userCmd.equalsIgnoreCase(search)) {
        search();
      }

      // :download -
      else if (userCmd.equalsIgnoreCase(download)) {
        download();
      }

      else { // do something with pathname

        File f = ftb.searchList(userCmd);

        if (f == null) {
          System.out.println("Unknown command or filename: '" + userCmd + "'");
        }

        // act upon entered filename
        else {

          String pathName = getPathName(f);

          if (f.isFile()) { // print some file details
            System.out.println("file: " + logicalPathName(pathName));
            System.out.println("size: " + f.length());
          }

          else if (f.isDirectory()) { // move into to the directory
            System.out.println(">>> " + logicalPathName(pathName));
            // ftb = new FileTreeBrowser(pathName);
            ftb.updateFTB(pathName);
          }

        } // (f == null)

      } // do something

    } // while(!quit)

  } // main()

  /**
   * Create a new FileTreeBrowser.
   *
   * @param pathName the pathname (directory) at which to start.
   */
  public FileTreeBrowser(String pathName) {
    if (pathName == "") {
      pathName = configuration.rootDir;
    } else // "." -- this directory, re-list only
    if (pathName.equals(list)) {
      pathName = thisDirName;
    }
    thisDir = new File(pathName);
    thisDirName = getPathName(thisDir);
  }

  public void updateFTB(String pathName) {
    if (pathName == "") {
      pathName = configuration.rootDir;
    } else // "." -- this directory, re-list only
    if (pathName.equals(list)) {
      pathName = thisDirName;
    }
    thisDir = new File(pathName);
    thisDirName = getPathName(thisDir);
  }

  /**
   * Print help message.
   */
  static void displayHelp() {

    String[] lines = {
        "--* Welcome to the simple FileTreeBrowser. *--",
        "* The display consists of:",
        "\t- The name of the current directory",
        "\t- The list of files (the numbers for the files are of no",
        "\t  significance, but may help you with debugging).",
        "* Files that are directories have trailing '" + File.separator + "'.",
        "* Use text entry to navigate the directory tree.",
        "\t.\t\tTo refresh the view of the current directory.",
        "\t..\t\tTo move up a directory level.",
        "\tfilename\tTo list file details (if it is a file) or to",
        "\t\t\tmove into that directory (if it is a directory name).",
        "\t:services\tTo list the services offered.",
        "\t:nodes\t\tTo list the other nodes discovered.",
        "\t:download\tTo download a file.",
        "\t:quit\t\tTo quit the program.",
        "\t:help\t\tTo print this message."
    };

    for (int i = 0; i < lines.length; ++i)
      System.out.println(lines[i]);

    return;
  }

  /**
   * Print config information.
   */
  static void displayServices() {

    String services = ":";
    services += "id=" + configuration.id + ":";
    services += "timestamp=" + Timestamp.now() + ":";
    services += "search=" + configuration.search + ",";
    services += "download=" + configuration.download;
    services += ":";

    System.out.println(services);
  }


  /**
   * Search
   * @param fileString
   */
  static void search() { // TBC
    System.out.print("Please enter search-string: ");

    // 1. waiting search-fileString input
    String fileString;
    while ((fileString = ByteReader.readLine(System.in)) == null) {
      try {
        Thread.sleep(configuration.sleepTime);
      } catch (InterruptedException e) {}
    }

    //2. sent search-request msg, and get serialNumber
    int serialNumber;
    //  2a) send search-request, and if fail to send, return
    if (service == null
        || (serialNumber = service.search(fileString)) == 0) //sent search-request
      return;

    //3. start process <search-result> and <search-error> msgs
    searchSeriaNumber = serialNumber;   // when != 0 start process search-response

    System.out.println("--* Press \"Enter\" to stop searching. *--");
    System.out.println("+++\t" + "id\t\t\t\t\t" + "fileString");

    // 4. waite soTimeout, to recive and print response
    try {
      Thread.sleep(configuration.soTimeout);
    } catch (InterruptedException e) {}

    // 5. re-set searchSeriaNumber and count, to stop processing search-response msgs
    searchSeriaNumber = 0;
    searchResultCount = 0;
  }

  static void download() { 
    String identifier;
    String filePath;
    int port;

    // 1. wait intput to get identifier
    System.out.print("Please enter fqdn: ");
    while ((identifier = ByteReader.readLine(System.in)) == null) {
      try {Thread.sleep(configuration.sleepTime);}
      catch (InterruptedException e) {}
    }
    
    // 2. clean cache
    service.cleanUpNFqdnCache();
    service.cleanUpNodeCache();
    
    // 3. get ipv6 and port, from 1) cache, 2) mquery
    //  3a) get porot for cache
    try {
      port = service.resolvePort(identifier);
    } catch (UnknownHostException e) {
      System.out.println(e.getMessage() + " Please re-try.");
      return;
    }

    //  3b) get ipv6, first from cache, second from mfqdnquery
    String fqdn = null;
    InetAddress ipv6 = null;
    fqdn = identifier.split("@")[1];

    for (int i = 2; i >= 0; i--) {
      //  3b1) try to resolve from cache
      try{
        ipv6 = service.resolveFqdn(fqdn);
        break;
      } 
      // 3b2) if can't resolve
      catch(UnknownHostException e) {
        // first time, query try twice
        if (i != 0) {
          service.mfqdnQuery(fqdn);
          // sleep soTimeout, to wait response
          // fqdn so time out = configuration.soTimeout
          try {Thread.sleep(configuration.fqdnQuerySoTime());} 
          catch (InterruptedException e1) {}
        }
        // second time return err msg
        else {
          System.out.println("Ipv6 resolve err. " + e.getMessage() + ". Please re-try.");
          return;
        }
      }
      catch(Exception e) {System.out.println("Invalid command. Please re-try");}
    }

    // 4. wait input to get download file full logical path-name
    System.out.print("Please enter full logical path-name: ");
    while ((filePath = ByteReader.readLine(System.in)) == null) {
      try {
        Thread.sleep(configuration.sleepTime);
      } catch (InterruptedException e) {}
    }

    // 5. establish tcp connect, and download
    try(
      // 5a) build connect
      Socket socket = new Socket(ipv6, port);
      InputStream inputStream = socket.getInputStream();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      OutputStream outputStream = socket.getOutputStream();
      PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
    ) {
      // 5a) build download-request msg
      String downloadRequest = new Message( new Header(configuration.id), 
                                            new DownloadRequest(filePath)
                                          ).toString();
      // 5b) sent download-request msg
      service.tcpTxLine(printWriter, downloadRequest);

      // 5c) recieve download-response, result/error
      String response = service.tcpRxLine(bufferedReader);

      // 5d) recive file (result) or print fail(error)
      //  5d1) decode msg
      Message msg = Message.decode(response);
      //  5d1) get msg type (result/error)
      switch (msg.getPayload().getType()) {
        // 5d1a) result -> successfull, start to download
        case "download-result" -> {
          // set download path (download/[filename])
          int fileDepth = filePath.split("/").length;
          String fileName = filePath.split("/")[fileDepth -1 ];
          File file = new File(configuration.downloadDir + fileName);

          // start download
          long fileLength = msg.getPayload().getContentLength();
          System.out.println("\n---> download start...");
          
          System.out.println("file size(Byte): "+ fileLength);
          for(long i = fileLength; i > 0 ; i = i - configuration.maximumMessageSize){
            System.out.print("-");
          }
          System.out.println();
          
          // download method
          System.out.println("progress: ");
          service.tcpRxFile(inputStream, file); // download method

          // complete download
          System.out.println("---+ download completes.");
        }
        // 5d1b) error -> fail
        case "download-error" -> {
          System.out.println("download-error, please re-try");
        }
      };
    } 
    catch (IOException e) {
      System.out.println("Can't connect to -> " + identifier + " Please re-try");
    } 
    catch (Exception e) {
      System.out.println("Something wrong: " + e.getMessage() + " Please re-try");
    }
  }



  /**
   * List the names of all the files in this directory.
   */
  public void printList() {

    File[] fileList = thisDir.listFiles();

    System.out.println("\n+++  id: " + configuration.id);
    System.out.println("+++ dir: " + logicalPathName(getPathName(thisDir)));
    System.out.println(getPathName(thisDir));

    System.out.println("+++\tfilename:");
    for (int i = 0; i < fileList.length; ++i) {

      File f = fileList[i];
      String name = f.getName();
      if (f.isDirectory()) // add a trailing separator to dir names
        name = name + File.separator;
      System.out.println(i + "\t" + name);
    }
    System.out.println("+++");
  }

  String getParent() {
    return thisDir.getParent();
  }

  /**
   * Search for a name in the list of files in this directory.
   *
   * @param name the name of the file to search for.
   */
  public File searchList(String name) {

    File found = null;

    File[] fileList = thisDir.listFiles();
    for (int i = 0; i < fileList.length; ++i) {
      if (name.equals(fileList[i].getName())) {
        found = fileList[i];
        break;
      }
    }
    return found;
  }

  /**
   * Get full pathname.
   *
   * @param f the File for which the pathname is required.
   */
  static public String getPathName(File f) {

    String pathName = null;

    try {
      pathName = f.getCanonicalPath();
    } catch (IOException e) {
      System.out.println("+++ FileTreeBrowser.pathname(): " + e.getMessage());
    }

    return pathName;
  }

  static public boolean isQuitting() {
    return isQuitting;
  }

  static public void quit() {
    isQuitting = true;
    closeAdvertise();
    closeRx();
    closeTcpRx();

    try {
      Thread.sleep(configuration.sleepTime);
    } catch (InterruptedException e) {}

    service.leave();            // leave group
    service.closeTcpServer();   // close server
  }


  /**
   * Starts a thread that broadcasts advertisement messages with exponential backoff.
   * The thread runs continuously until interrupted, implementing an adaptive broadcast interval:
   * - Initial period starts at 1000ms
   * - Period doubles after each advertisement until reaching maximumAdvertisementPeriod
   * - Period resets to 1000ms when discovering a new node
   * 
   * @param service   Node service on this host
   */
  static void startAdvertise(Service service) {
    advertise = new Thread(() -> {
      configuration.log.writeLog("advertise start", configuration.isTest);
      while (!Thread.currentThread().isInterrupted()) {
        service.advertise();
        try {
          Thread.sleep(advertisementPeriod);
          if (2 * advertisementPeriod <= configuration.maximumAdvertisementPeriod) {
            advertisementPeriod =  2 * advertisementPeriod;
          } else {
            advertisementPeriod = configuration.maximumAdvertisementPeriod;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      configuration.log.writeLog("advertise close", true);
    });

    advertise.start();
    configuration.log.writeLog("muticast advertise start", true);
  }

  /**
   * Interrupts and stops the advertisement broadcasting thread.
   */
  static void closeAdvertise() {
    advertise.interrupt();
  }


  /**
   * Starts a thread that handles incoming multicast messages.
   * The thread continuously listens and processes different types of protocol msgs:
   * - Advertisement messages for node discovery
   * - Search requests/results/errors for file search functionality
   * - MFQDNS queries/answers for FQDN resolution
   * 
   * @param service The service instance handling message processing
   */
  static void startRx(Service service) {
    rx = new Thread(() -> {
      configuration.log.writeLog("muticast rx start", true);
      while (!Thread.currentThread().isInterrupted()) {
        try {
          // 1. recieve msg;
          String rMsg = service.rxMessage();
          // 2. decode
          Message msg = Message.decode(rMsg);
          Header header = msg.getHeader();
          Payload payload = msg.getPayload();

          String id;
          String fileString;
          // 3. process accoding different type
          switch (payload.getType()) {
            // A - advertisement: update nodecache
            case "advertisement" -> {
              // if discover a new node, re-set advertisementPeriod
              if(service.resolveNodeById(header.getIdentifier()) == null) {
                advertisementPeriod = configuration.initialAdvertisementPeriod;
              }
              service.updateNode(new Node(header.getIdentifier(),
                                          payload.getServerPort(),
                                          payload.isSearchEnabled(),
                                          payload.isDownloadEnabled(),
                                          header.getTimestamp(),
                                          configuration.nodeTTL()));
            }
            
            // B1 - search-request:
            case "search-request" -> {
              // B1.1) if this host can't be searched, do nothing, no process
              if (!service.getNode().isSearchEnabled()) continue;
              
              id = header.getIdentifier();
              int serialNumber = header.getSerialNumber();
              String searchStr = payload.getSearchString();

              // B1.2) search files to a list
              List<File> files = FileSearcher.search(rootPath, searchStr);

              // B1.3) if no matches, send <search-error>
              if (files.size() == 0) {
                service.response(id, serialNumber);
              }
              // B1.4) else, each match send a <search-result> msg
              else {
                for (File file: files) {
                  // send file string relative to the logical root
                  String fileStr = logicalPathName(file.toString());
                  service.response(id, serialNumber, fileStr);
                }
              }
            }
            
            // B2 - search-resul: when is searching, and response-id match, get a result
            case "search-result" -> {
              // - only process when during searching, and response-id match requst
              if (searchSeriaNumber == 0          // ==0 means is not searching
                  // match response-id
                  || !configuration.id.equals(payload.getResponseId().getIdentifier())
                  || searchSeriaNumber != payload.getResponseId().getSerialNumber())
                continue;
              
              // B2.1) count + 1
              searchResultCount ++;
              id = header.getIdentifier();
              fileString = payload.getFilePath();
              
              // B2.2) print search result
              System.out.println(searchResultCount + "\t" 
                                 + id +"\t" 
                                 + fileString);
            }
            
            // B3 - when is searching, and response-id match, get a result (no matching)
            case "search-error" -> {
              // - only process when during searching, and response-id match requst
              if (searchSeriaNumber == 0
                  || !configuration.id.equals(payload.getResponseId().getIdentifier())
                  || searchSeriaNumber != payload.getResponseId().getSerialNumber())
                continue;
  
              id = header.getIdentifier();
              fileString = "no results found.";

              // B2.2) print search result
              searchResultCount ++;
              System.out.println(searchResultCount + "\t" 
                                + id +"\t" 
                                + fileString);;
            }

            // C1 - mfqdns-query, send annswer msg.
            case "mfqdns-query" -> {
              if (isSendingFqdnAnswer                      // has receive query msg
                  || !configuration.id.contains(payload.getFqdn())  // not query me
              ) continue;

              // C1.1) set isSendingFqdnAnswer = true, to skip process other query
              isSendingFqdnAnswer = true;
              // C1.2) wait 1/2 so time and sent answer msg, and then re-set isSendingFqdnAnswer
              new Thread(() -> {
                // sleep 1/2 fqdnQuerySoTime
                try {Thread.sleep(configuration.fqdnQuerySoTime()/2);} 
                catch (InterruptedException e) {}
                service.mfqdnAnswer();
                isSendingFqdnAnswer = false;
              }).start();;
            }

            // C2 - mfqdns-answer, update fqdncache
            case "mfqdns-answer" -> {
              service.updateFqdnCache(header.getFqdn(),
                                      payload.getTTL(),
                                      payload.getIpv6Address(),
                                      header.getTimestamp());
            }
   
            default -> {
              System.out.println("FileTreeBrowser.rx: other type msg");
            }
          }
        } catch (Exception e) {}
      }
      configuration.log.writeLog("muticast rx close", true);
    });

    rx.start();
  }

  /**
   * Interrupts and stops the message receiving thread.
   */
  static void closeRx() {
    rx.interrupt();
  }

  // TCP

  /**
   * Starts a TCP server thread that listens for incoming download requests.
  * For each incoming connection, creates a new thread to handle the download request.
  * 
  * @param serverSocket The server socket listening for TCP connections
  */
  static void startTcpRx(ServerSocket serverSocket) {
    tcpRx = new Thread(() -> {
    configuration.log.writeLog("tcp server start accepting", true);
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Socket socket = serverSocket.accept();
          // response to download request
          new Thread(() -> tcpResponseDownloadRequest(socket)).start();;
        } catch (IOException e) {}
      }
      configuration.log.writeLog("tcp server stop accepting.", true);
    });

    tcpRx.start();
    
  }

  /**
   * Interrupts and stops the TCP server thread.
   */
  static void closeTcpRx() {
    tcpRx.interrupt();
  }

  /**
   * Handles an individual file download request over a TCP connection.
   * Process:
   * 1. Receives and decodes download request message
   * 2. Validates request and checks file existence
   * 3. Sends appropriate response (success with file or error)
   * 4. Transfers file data if available
   * 5. Closes connection
   *
   * @param socket The TCP socket connection for the download request
   */
  static void tcpResponseDownloadRequest(Socket socket) {
    try (
      InputStreamReader isr = new InputStreamReader(socket.getInputStream()); 
      BufferedReader bufferedReader = new BufferedReader(isr);
      PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
    ){
      // 1. recive a one line string (downloda request)
      String rxLine = service.tcpRxLine(bufferedReader);

      // 2. decode to Message
      Message msg = Message.decode(rxLine);
      Payload payload = msg.getPayload();

      // only process download-request
      if (!payload.getType().equals("download-request") ) return;

      // 3. initialize download response, and requset file path 
      String downloadResponseMsg;
      File file = new File(rootPath + payload.getFileString());

      // 3-1 when host is downloadenablebd && file exsits, response result & file
      if (file.isFile() && service.getNode().isDownloadEnabled()) {
        downloadResponseMsg = new Message(new Header(configuration.id),
                                          new DownloadResult(file.length())
                                          ).toString();
        
        service.tcpTxLine(printWriter, downloadResponseMsg);
        service.tcpTxFile(socket.getOutputStream(), file);
      }
      // 3-2 when file not exsits, response errer
      else {
        downloadResponseMsg = new Message(new Header(configuration.id),
                                          new DownloadError()
                                          ).toString();
        service.tcpTxLine(printWriter, downloadResponseMsg);
      }
    } 
    catch (Exception e) {System.out.println();} 
    finally {
      try {socket.close();} 
      catch (IOException e) {}
    }
  }
}
