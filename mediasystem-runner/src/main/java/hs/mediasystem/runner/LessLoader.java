package hs.mediasystem.runner;

import com.inet.lib.less.Less;
import com.inet.lib.less.ReaderFactory;

import hs.mediasystem.util.URLs;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LessLoader {

  public static URL compile(URL url) {
    try {
      System.out.println("::: LOADING : " + url);
      String compiledCSS = Less.compile(url.toURI().resolve(".").toURL(), new String(URLs.readAllBytes(url), StandardCharsets.UTF_8), false, new ReaderFactory() {
        @Override
        public Reader create(URL url) throws IOException {
          System.out.println("::::::::::::::: " + url);
          // TODO Auto-generated method stub
          return super.create(url);
        }
      });

      Path tempFile = Files.createTempFile(null, null);
      Files.write(tempFile, compiledCSS.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

      tempFile.toFile().deleteOnExit();

      return tempFile.toUri().toURL();
    }
    catch(IOException | URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
