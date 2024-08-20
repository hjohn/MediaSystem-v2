package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import hs.mediasystem.util.CryptoUtil;
import hs.mediasystem.util.exception.HttpException;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.image.ImageURI;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Opt;

@Singleton
public class TheMovieDatabase {
  private static final Logger LOGGER = Logger.getLogger(TheMovieDatabase.class.getName());

  private final String apiKey = CryptoUtil.decrypt("8AF22323DB8C0F235B38F578B7E09A61DB6F971EED59DE131E4EF70003CE84B483A778EBD28200A031F035F4209B61A4", "-MediaSystem-"); // Yes, I know you can still get the key.
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Lock lock = new ReentrantLock(true);

  @Inject @Opt @Named("ext.tmdb.host") private String host = "http://api.themoviedb.org/";

  private JsonNode configuration;

  /**
   * Queries the movie database and returns an optional {@link JsonNode}. If the query
   * resulted in a not found response, an empty optional is returned. In all other cases
   * an exception is thrown.
   *
   * @param query a query, cannot be {@code null}
   * @param key a logical key which identifies the result for use in HTTP caching, cannot be {@code null}
   * @param parameters parameters to append to the URL, cannot be {@code null}
   * @return an optional {@link JsonNode}, never {@code null}
   * @throws HttpException when there was a status code that was not in the 200 range and not 404
   * @throws IOException when there was a general I/O error
   */
  public Optional<JsonNode> query(String query, String key, List<String> parameters) throws IOException {
    if(parameters.size() % 2 != 0) {
      throw new IllegalArgumentException("Uneven number of vararg 'parameters': must provide pairs of name/value");
    }

    try {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < parameters.size(); i += 2) {
        sb.append("&");
        sb.append(parameters.get(i));
        sb.append("=");
        sb.append(URLEncoder.encode(parameters.get(i + 1), "UTF-8"));
      }

      return getURL(new URI(host + query + "?api_key=" + apiKey + sb.toString()).toURL(), key);
    }
    catch(IOException | URISyntaxException e) {
      throw new IOException("While executing query: " + query + "; key=" + key + "; parameters=" + parameters, e);
    }
  }

  // variant of query that shouldn't fail with 404
  public JsonNode get(String query, String key, List<String> parameters) throws IOException {
    return query(query, key, parameters).orElseThrow(() -> new IOException("TMDB unexpectedly returned 404 not found while executing query: " + query));
  }

  public Optional<JsonNode> query(String query, String key) throws IOException {
    return query(query, key, List.of());
  }

  public static LocalDate parseDateOrNull(String text) {
    try {
      return text == null || text.isEmpty() ? null : LocalDate.parse(text);
    }
    catch(DateTimeParseException e) {
      LOGGER.warning("Non-empty unparsable date encountered: '" + text + "'");
      return null;
    }
  }

  /**
   * Create image URI's for a TMDB resource. If the TMDB configuration could not be loaded,
   * or the path or size parameters are {@code null}, then {@code null} is returned.
   *
   * @param path a TMDB image path, can be {@code null}
   * @param size a preferred image size, can be {@code null}
   * @param key a cache key, can be {@code null}
   * @return an {@link ImageURI}, or {@code null} if there was a problem creating it
   */
  public ImageURI createImageURI(String path, String size, String key) {
    if(path == null || size == null) {
      return null;
    }

    try {
      return new ImageURI(getConfiguration().get("images").get("base_url").textValue() + size + path, key);
    }
    catch(Exception e) {
      LOGGER.warning("Unable to create TMDB Image URL for: " + path + " (" + size + "); exception: " + Throwables.formatAsOneLine(e));

      return null;
    }
  }

  private JsonNode getConfiguration() throws IOException {
    lock.lock();

    try {
      if(configuration == null) {
        configuration = query("3/configuration", null).orElseThrow(() -> new IOException("Could not load TMDB configuration (404)"));
      }

      return configuration;
    }
    finally {
      lock.unlock();
    }
  }

  private Optional<JsonNode> getURL(URL url, String key) throws IOException {
    try {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();

      connection.addRequestProperty("!time-out", Integer.toString(4 * 60 * 60));  // Requests cached for upto 4 hours
      connection.addRequestProperty("!rate-limit", "TMDB;10;5");  // TMDB allows a maximum of 30 queries in a period of 10 seconds, this rate limiter allows 10 queries per 5 seconds.
      connection.addRequestProperty("!safe-url", url.toString().replaceAll("api_key=[0-9A-Za-z]+", "api_key=***"));   // Strips api_key from URL for safe logging
      connection.addRequestProperty("!key", key);

      int responseCode = connection.getResponseCode();

      if(responseCode == 404) {
        return Optional.empty();
      }

      if(responseCode < 200 || responseCode >= 300) {
        throw new HttpException(url, responseCode, connection.getResponseMessage());
      }

      try(InputStream is = connection.getInputStream()) {
        return Optional.of(objectMapper.readTree(is));
      }
    }
    catch(HttpException e) {
      throw e;
    }
    catch(IOException e) {
      throw new IOException("Exception while reading: " + url, e);
    }
  }
}
