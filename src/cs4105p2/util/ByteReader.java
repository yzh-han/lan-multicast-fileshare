package cs4105p2.util;
/**
 * ByteReader - two different mechanisms
 
   Saleem Bhatti
   18 Sep 2024, checked with java 21 on CS Lab Linux machines. 
   Oct 2023, Oct 2022, Oct 2021, Oct 2020, Sep 2019, Oct 2018
 
 */

import java.io.*;

public class ByteReader {

 /**
  * This gives you access to the individual bytes and can be used
  * for any kind of byte-oriented input stream, including a
  * network stream.
  */
  public static byte[] readBytes(InputStream i, int n)
  {
    byte[] buffer = null;
    byte[] b = null;
    int    r = 0;

    try {
      if (i.available() > 0) { // something to read?
        b = new byte[n]; // a maximum of n bytes will be read
        r = i.read(b); // r includes the end of line (EOL) byte if present
      }
    }
    catch (IOException e) {
      System.err.println("ByteReader.readBytes() - error: " + e.getMessage());
    }

    if (r > 0) {
      buffer = new byte[r]; // a buffer of just the right size
      for (--r; r >= 0; --r) { buffer[r] = b[r]; }
    }

    return buffer;
  } // readBytes


 /**
  *  This is the preferred mechanism for Java keyboard entry.
  */
  public static String readLine(InputStream i)
  {
    String line = null;

    try {
      if (i.available() > 0) { // something to read?
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(i));
        line = br.readLine(); // assumes EOL marker of "\n, "\r" or "\r\n"
      }
    }
    catch (IOException e) {
      System.err.println("ByteReader.readLine() - error: " + e.getMessage());
    }

    return line;

  } // readLine()

} // class
