package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import hs.mediasystem.util.CryptoUtil;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.RateLimiter;
import hs.mediasystem.util.RuntimeIOException;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.URLs;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.inject.Singleton;

@Singleton
public class TheMovieDatabase {
  private static final Logger LOGGER = Logger.getLogger(TheMovieDatabase.class.getName());
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * TMDB allows a maximum of 30 queries in a period of 10 seconds, this rate limiter allows 15 queries per 10 seconds.
   */
  private static final RateLimiter GLOBAL_RATE_LIMITER = new RateLimiter(15, 10);

  private final String apiKey = CryptoUtil.decrypt("8AF22323DB8C0F235B38F578B7E09A61DB6F971EED59DE131E4EF70003CE84B483A778EBD28200A031F035F4209B61A4", "-MediaSystem-"); // Yes, I know you can still get the key.
  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode configuration;

  public JsonNode query(String query, String... parameters) {
    if(parameters.length % 2 != 0) {
      throw new IllegalArgumentException("Uneven number of vararg 'parameters': must provide pairs of name/value");
    }

    try {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < parameters.length; i += 2) {
        sb.append("&");
        sb.append(parameters[i]);
        sb.append("=");
        sb.append(URLEncoder.encode(parameters[i + 1], "UTF-8"));
      }

      URL url = new URL("http://api.themoviedb.org/" + query + "?api_key=" + apiKey + sb.toString());

      LOGGER.info("Querying TMDB: " + url.toString().replaceAll("api_key=[0-9A-Za-z]+", "api_key=***"));

      return objectMapper.readTree(getURL(url));
    }
    catch(RuntimeIOException | IOException e) {
      throw new RuntimeException("While executing query: " + query + "; parameters=" + Arrays.toString(parameters), e);
    }
  }

  public static LocalDate parseDateOrNull(String text) {
    try {
      return text == null ? null : DATE_TIME_FORMATTER.parse(text, new TemporalQuery<LocalDate>() {
        @Override
        public LocalDate queryFrom(TemporalAccessor temporal) {
          return LocalDate.from(temporal);
        }
      });
    }
    catch(DateTimeParseException e) {
      return null;
    }
  }

  public ImageURI createImageURI(String path, String size) {
    if(path == null || size == null) {
      return null;
    }

    try {
      return new ImageURI(getConfiguration().get("images").get("base_url").textValue() + size + path);
    }
    catch(IllegalArgumentException e) {
      LOGGER.warning("Bad TMDB URL: \"" + getConfiguration().get("images").get("base_url").textValue() + size + path + "\"; exception: " + Throwables.formatAsOneLine(e));
      return null;
    }
  }

  private JsonNode getConfiguration() {
    if(configuration == null) {
      synchronized(this) {
        if(configuration == null) {
          configuration = query("3/configuration");
        }
      }
    }

    return configuration;
  }

  private static byte[] getURL(URL url) {
    GLOBAL_RATE_LIMITER.acquire();

    return URLs.readAllBytes(url);
  }
}
