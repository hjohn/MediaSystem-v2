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
      System.out.println("::: PARENT : " + url.toURI().resolve("."));
      String compiledCSS = Less.compile(url, new String(URLs.readAllBytes(url), StandardCharsets.UTF_8), false, new ReaderFactory() {
        @Override
        public Reader create(URL url) throws IOException {

          /*
           * URL here is not valid, but can be resolved in the context of hs.mediasystem.runner by taking the path
           * only.
           */

          System.out.println("::::::::::::::: " + url + " (path=" + url.getPath() +")");
          System.out.println("::::::::::::::: res = " + getClass().getClassLoader().getResource("/" + url.getPath()));

          //return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hs/mediasystem/runner/" + url.getPath()));
          return super.create(url);


//          return super.create(getClass().getClassLoader().getResource(url.getPath()));
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
