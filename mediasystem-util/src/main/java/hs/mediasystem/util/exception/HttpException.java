package hs.mediasystem.util.exception;

import java.io.IOException;
import java.net.URL;

public class HttpException extends IOException {
  private final int responseCode;

  public HttpException(URL url, int responseCode, String message) {
    super(url + " -> " + responseCode + ": " + message);

    this.responseCode = responseCode;
  }

  public int getResponseCode() {
    return responseCode;
  }
}