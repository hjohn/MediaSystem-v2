package hs.mediasystem.util;

import java.net.URL;

public class HttpException extends RuntimeIOException {
  private final int responseCode;

  public HttpException(URL url, int responseCode, String message) {
    super(url + " -> " + responseCode + ": " + message);

    this.responseCode = responseCode;
  }

  public int getResponseCode() {
    return responseCode;
  }
}