package cs4105p2;
/*
  Application Configuration information
  CS4105 Practical P2 - Discover and Sahre

  Saleem Bhatti
  18 Sep 2024, checked with java 21 on CS Lab Linux machines. 
  Oct 2023, Oct 2022, Oct 2021, Oct 2020, Sep 2019, Oct 2018

*/

import java.io.FileInputStream;

/*
  This is an object that gets passed around, containing useful information.
*/

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
//https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Properties.html
import java.util.Properties;

import cs4105p2.util.LogFileWriter;

public class Configuration {
  public boolean isTest = false;
  // public boolean isTest = true;

  // Everything here is "public" to allow tweaking from user code.
  public Properties properties;
  public String propertiesFile = "config/configuration.properties";
  public LogFileWriter log;
  public String logFile = "logs/";
  public String downloadDir = "downloads/";

  /*
   * The assumption is that the workstation only has a single main interface.
   * The "first" detected network interface is used.
   * 
   * This is simplistic, but fine for the CS lab linux machines.
   * 
   * hostName, nifName, ipAddr4, and ipAddr6 are detected dynamically.
   */
  // public String hostName = "";
  // public String nifName = "";
  // public String ipAddr4 = "";
  public String ipAddr6 = "";
  public NetworkInterface nif = null;

  // These default values can be overriden in the properties file.

  // 'id -u' gives a numeric uid, u, which will be unique within the lab.
  // You can construct your "personal" multicast address, by "splitting"
  // `u` across the lower 32 bits. For example, if `u` is 414243,
  // mAddr6 = ff02::41:4243
  // and your "personal" port number, mPort_ = u.
  public String mAddr6 = "ff02::4105:4105"; // CS4105 whole class group
  public int mPort = 4105;
  public final String zeroAddr = "0"; // to indicate a "null" address

  public int tcpPort() {return mPort;}

  public int mTTL = 2; // plenty for the lab
  public boolean loopback = true; // ignore my own transmissions
  public boolean reuseAddr = false; // allow address use by other apps
  public int soTimeout = 1; // ms
  public int sleepTime = 5000; // ms
  
  public int fqdnQuerySoTime() {return soTimeout;}

  // // // //
  // application config -- default values 
  public String rootDir = "root_dir"; // sub-dir in current dir
  public String id; // System.getProperty("user.name") @ fqdn;
  public int maximumMessageSize = 500; // bytes
  public int maximumAdvertisementPeriod = 1000; // ms
  public int initialAdvertisementPeriod = 250; // ms


  public int nodeTTL(){return maximumAdvertisementPeriod + soTimeout;} // ms

  public int mfqdnTTL = 60000; // ms

  public Boolean checkOption(String value, String[] optionList) {
    boolean found = false;
    for (String option : optionList) {
      if (value.equals(option)) {
        found = true;
        break;
      }
    }
    return found;
  }

  public String[] true_false = { "true", "false" }; // Could have used enum.
  public String[] searchOptions = // Could have used enum.
      { "none", "path", "path-filename", "path-filename-substring" };
  public String search = "none"; // from searchOptions_
  public boolean download = false;

  // these should not be loaded from a config file, of course

  public Configuration(String file) {
    if (file != null) {
      propertiesFile = file;
    }

    try {

      /*
       * This is *not* a general way of reliably discovering all the local IPv4
       * and IPv6 addresses being used by a host on all of its interfaces, and
       * working out which is the "main" interface. However, it works for the CS
       * lab linux machines, which have:
       * 1. a single gigabit ethernet interface.
       * 2. a single IPv4 address for that interface.
       * 3. only link-local IPv6 (no global IPv6 prefix).
       */
      InetAddress ip4 = InetAddress.getLocalHost(); // assumes IPv4!
      nif = NetworkInterface.getByInetAddress(ip4); // assume the "main" interface
      // nifName = nif.getName();
      Enumeration<InetAddress> e_addr = nif.getInetAddresses();

      while (e_addr.hasMoreElements()) {
        final InetAddress a = e_addr.nextElement();
        // ---> test use
        if (a.getHostAddress().length() > 15) {
          ipAddr6 = a.getHostAddress();
        }
        // --- test use

        if (a.isLinkLocalAddress()) { // assume only this will be used
          // will include interface name, e.g. fe80:0:0:0:1067:14a1:4e8b:28ac%en0
          ipAddr6 = a.getHostAddress();
          break;
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    String h;

    try {
      // h = InetAddress.getLocalHost().getHostName();
      // h is fqdn
      h = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      System.out.println("Problem: " + e.getMessage());
      h = "FileTreeBrowser-host";
      System.out.println("Unknown host name: using " + h);
    }

    try {
      id = new String(System.getProperty("user.name") + "@" + h);
      logFile = logFile + new String(id + "-log.log");

      properties = new Properties();
      // InputStream p = getClass().getClassLoader().getResourceAsStream(propertiesFile);
      InputStream p = new FileInputStream(propertiesFile);

      if (p != null) {
        properties.load(p);
        String s;
        if ((s = properties.getProperty("logFile")) != null) {
          System.out.println(propertiesFile + " logFile: " + logFile + " -> " + s);
          logFile = new String(s);
        }

        if ((s = properties.getProperty("id")) != null) {
          System.out.println(propertiesFile + " id: " + id + " -> " + s);
          id = new String(s + "@" + h);
        }

        if ((s = properties.getProperty("rootDir")) != null) {
          System.out.println(propertiesFile + " rootDir: " + rootDir + " -> " + s);
          rootDir = new String(s);
        }

        if ((s = properties.getProperty("mAddr6")) != null) {
          System.out.println(propertiesFile + " mAddr6: " + mAddr6 + " -> " + s);
          mAddr6 = new String(s);
          // should check for valid mutlicast address range
        }

        if ((s = properties.getProperty("mPort")) != null) {
          System.out.println(propertiesFile + " mPort: " + mPort + " -> " + s);
          mPort = Integer.parseInt(s);
          // should check for valid port number range
        }

        if ((s = properties.getProperty("mTTL")) != null) {
          System.out.println(propertiesFile + " mTTL: " + mTTL + " -> " + s);
          mTTL = Integer.parseInt(s);
          // should check for valid TTL number range
        }

        if ((s = properties.getProperty("loopback")) != null) {
          if (!checkOption(s, true_false)) {
            System.out.println(propertiesFile + " bad value for 'loopback': '" + s + "' -> using 'false'");
            s = new String("false");
          }
          System.out.println(propertiesFile + " loopback: " + loopback + " -> " + s);
          loopback = Boolean.valueOf(s);
        }

        if ((s = properties.getProperty("reuseAddr")) != null) {
          if (!checkOption(s, true_false)) {
            System.out.println(propertiesFile + " bad value for 'reuseAddr': '" + s + "' -> using 'false'");
            s = new String("false");
          }
          System.out.println(propertiesFile + " reuseAddr: " + reuseAddr + " -> " + s);
          reuseAddr = Boolean.valueOf(s);
        }

        if ((s = properties.getProperty("soTimeout")) != null) {
          System.out.println(propertiesFile + " soTimeout: " + soTimeout + " -> " + s);
          soTimeout = Integer.parseInt(s);
          // should check for "sensible" timeout value
        }

        if ((s = properties.getProperty("sleepTime")) != null) {
          System.out.println(propertiesFile + " sleepTime: " + sleepTime + " -> " + s);
          sleepTime = Integer.parseInt(s);
          // should check for "sensible" sleep value
        }

        if ((s = properties.getProperty("maximumMessageSize")) != null) {
          System.out.println(propertiesFile + " maximumMessageSize: " + maximumMessageSize + " -> " + s);
          maximumMessageSize = Integer.parseInt(s);
          // should check for "sensible" message size value
        }

        if ((s = properties.getProperty("maximumAdvertisementPeriod")) != null) {
          System.out
              .println(propertiesFile + " maximumAdvertisementPeriod: " + maximumAdvertisementPeriod + " -> " + s);
          maximumAdvertisementPeriod = Integer.parseInt(s);
          // should check for "sensible" period value
        }

        if ((s = properties.getProperty("search")) != null) {
          if (!checkOption(s, searchOptions)) {
            System.out.println(propertiesFile + " bad value for 'search': '" + s + "' -> using 'none'");
            s = new String("none");
          }
          System.out.println(propertiesFile + " search: " + search + " -> " + s);
          search = new String(s);
        }

        if ((s = properties.getProperty("download")) != null) {
          if (!checkOption(s, true_false)) {
            System.out.println(propertiesFile + " bad value for 'download': '" + s + "' -> using 'false'");
            s = new String("false");
          }
          System.out.println(propertiesFile + " download: " + download + " -> " + s);
          download = Boolean.parseBoolean(s);
        }

        p.close();
      }

      log = new LogFileWriter(logFile);
      log.writeLog("-* logFile=" + logFile, true);
      log.writeLog("-* downloadDir=" + downloadDir, true);
      log.writeLog("-* id=" + id, true);
      log.writeLog("-* rootDir=" + rootDir, true);
      log.writeLog("-* mAddr6=" + mAddr6, true);
      log.writeLog("-* mPort=" + mPort, true);
      log.writeLog("-* tcpPort=" + tcpPort(), true);
      log.writeLog("-* mTTL=" + mTTL, true);
      log.writeLog("-* loopback=" + loopback, true);
      log.writeLog("-* reuseAddr=" + reuseAddr, true);
      log.writeLog("-* soTimeout=" + soTimeout, true);
      log.writeLog("-* fqdnQuerySoTime=" + fqdnQuerySoTime(), true);
      log.writeLog("-* sleepTime=" + sleepTime, true);
      log.writeLog("-* maximumMessageSize=" + maximumMessageSize, true);
      log.writeLog("-* maximumAdvertisementPeriod=" + maximumAdvertisementPeriod, true);
      log.writeLog("-* initialAdvertisementPeriod=" + initialAdvertisementPeriod, true);
      log.writeLog("-* nodeTTL=" + nodeTTL(), true);
      log.writeLog("-* mfqdnTTL=" + mfqdnTTL, true);
      log.writeLog("-* search=" + search, true);
      log.writeLog("-* download=" + download, true);
    }

    catch (UnknownHostException e) {
      System.out.println("Problem: " + e.getMessage());
    }

    catch (NumberFormatException e) {
      System.out.println("Problem: " + e.getMessage());
    }

    catch (IOException e) {
      System.out.println("Problem: " + e.getMessage());
    }

  }
}
