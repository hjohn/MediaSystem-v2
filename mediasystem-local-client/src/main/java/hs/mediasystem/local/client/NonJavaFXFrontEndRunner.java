package hs.mediasystem.local.client;

public class NonJavaFXFrontEndRunner {
  public static void main(String[] args) {
    FrontEndRunner.main(args);  // Workaround for javafx.graphics named module check when bundled as fat jar
  }
}
