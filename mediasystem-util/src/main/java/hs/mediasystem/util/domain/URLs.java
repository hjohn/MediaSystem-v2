package hs.mediasystem.util.domain;

import hs.mediasystem.util.exception.HttpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class URLs {

  /**
   * Reads all bytes supplied by the given URL.
   *
   * @param url a URL
   * @param requestProperties a {@link Map} of general request properties, see {@link HttpURLConnection#addRequestProperty(String, String)}, cannot be null
   * @return a byte array
   * @throws HttpException when the URL uses the HTTP protocol and a response code other than 200 was received
   * @throws IOException when an {@link IOException} occurs
   */
  public static final byte[] readAllBytes(URL url, Map<String, String> requestProperties) throws IOException {
    URLConnection connection = url.openConnection();

    if(connection instanceof HttpURLConnection httpURLConnection) {
      requestProperties.entrySet().stream().forEach(e -> httpURLConnection.addRequestProperty(e.getKey(), e.getValue()));

      if(httpURLConnection.getResponseCode() != 200) {
        throw new HttpException(url, httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage());
      }
    }

    int length = connection.getContentLength();

    if(length == -1) {
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

    byte[] data = new byte[length];

    try(InputStream is = connection.getInputStream()) {
      int readLength = is.readNBytes(data, 0, length);

      if(readLength != length) {
        throw new IOException("wrong length read, expected " + length + ", but got: " + readLength);
      }

      return data;
    }
  }

  public static final byte[] readAllBytes(URL url) throws IOException {
    return readAllBytes(url, Map.of());
  }

  public static final String readAllString(URL url) throws IOException {
    try(InputStream is = url.openStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
