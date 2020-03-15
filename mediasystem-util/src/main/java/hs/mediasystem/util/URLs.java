package hs.mediasystem.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLs {

  /**
   * Reads all bytes supplied by the given URL.  Any IO problems will be wrapped in
   * a RuntimeIOException.
   *
   * @param url a URL
   * @return a byte array
   * @throws HttpException when the URL uses the HTTP protocol and a response code other than 200 was received
   * @throws RuntimeIOException when an {@link IOException} occurs
   */
  public static final byte[] readAllBytes(URL url) {
    try {
      URLConnection connection = url.openConnection();

      if(connection instanceof HttpURLConnection) {
        HttpURLConnection httpURLConnection = (HttpURLConnection)connection;

        if(httpURLConnection.getResponseCode() != 200) {
          throw new HttpException(url, httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage());
        }
      }

      try(InputStream is = connection.getInputStream();
          ByteArrayOutputStream bais = new ByteArrayOutputStream()) {
        byte[] byteChunk = new byte[4096];
        int n;

        while((n = is.read(byteChunk)) > 0) {
          bais.write(byteChunk, 0, n);
        }

        return bais.toByteArray();
      }
    }
    catch(IOException e) {
      throw new RuntimeIOException("While reading URL: " + url, e);
    }
  }
}
