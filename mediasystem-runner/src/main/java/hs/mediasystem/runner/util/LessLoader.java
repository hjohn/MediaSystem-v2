package hs.mediasystem.runner.util;

import com.inet.lib.less.Less;
import com.inet.lib.less.LessException;
import com.inet.lib.less.ReaderFactory;
import com.sun.jna.Platform;

import hs.mediasystem.util.domain.URLs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LessLoader {
  private final URL baseUrl;
  private final String root;
  private final Class<?> cls;

  public static String compile(Class<?> cls, String name) {
    return new LessLoader(cls).compile(name);
  }

  public LessLoader(Class<?> cls) {
    this.cls = cls;

    try {
      String packageName = cls.getPackageName();

      // create a parent directory level for each part of the package name.  Two levels would be "../..".
      packageName = packageName.replaceAll("\\.", "/").replaceAll("[^/]+", "..");

      this.baseUrl = cls.getResource("").toURI().resolve(packageName).toURL();
      this.root = baseUrl.toExternalForm().replaceAll("/$", "");
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String compile(String name) {
    return compileUrl(cls.getResource(name));
  }

  private String compileUrl(URL url) {
    try {
      String lessData = URLs.readAllString(url).replace("${root}", root);

      String compiledCSS = Less.compile(baseUrl, lessData, false, new ReaderFactory() {
        @Override
        public Reader create(URL importUrl) throws IOException {
          URL url = toSystemSpecificURL(importUrl);

          if(!exists(url)) {
            url = importUrl;
          }

          return new StringReader(URLs.readAllString(url).replace("${root}", root));
        }
      });

      return "data:text/css;charset=UTF-8;base64,"
        + Base64.getEncoder().encodeToString(compiledCSS.getBytes(StandardCharsets.UTF_8));
    }
    catch(LessException e) {
      throw new IllegalStateException("Exception while parsing: " + url, e);
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static boolean exists(URL url) {
    try(InputStream is = url.openStream()) {
      return true;
    }
    catch(IOException e) {
      return false;
    }
  }

  private static URL toSystemSpecificURL(URL url) throws MalformedURLException {
    String path = url.toExternalForm();

    int slash = path.lastIndexOf("/");
    String file = path.substring(slash + 1);
    int dot = file.lastIndexOf(".");
    String name = file.substring(0, dot);
    String extension = file.substring(dot + 1);

    String type = Platform.isLinux() ? "linux"
      : Platform.isWindows() ? "win"
      : Platform.isMac() ? "mac"
      : "other";

    return URI.create(path.substring(0, slash) + "/" + name + "-" + type + "." + extension).toURL();
  }
}
