package hs.mediasystem.runner.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import hs.mediasystem.db.ImageRecord;
import hs.mediasystem.db.ImageStore;
import hs.mediasystem.util.PriorityRateLimiter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A {@link ResponseCache} that keeps track of request/responses in a database, and
 * returns responses from cache when possible.<p>
 *
 * The requests can be parameterized with two parameters:<p>
 *
 * "!time-out": How fresh the data must be to be returned from cache (in seconds)
 * "!rate-limit": Used to specify a maximum rate at which resources can be accessed (format is two semi-colon separated parameters for a {@link PriorityRateLimiter})
 * "!safe-url": A URL for logging requests stripped off sensitive data (passwords, API key's)
 */
@Singleton
public class DatabaseResponseCache extends ResponseCache {
  private static final Logger LOGGER = Logger.getLogger(DatabaseResponseCache.class.getName());
  private static final int ID = 0xCAC4EDA7;  // "CACHEDAT"
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final List<String> DEFAULT_TIME_OUT = List.of(Integer.toString(Integer.MAX_VALUE));
  private static final List<String> DEFAULT_NULL = Arrays.asList((String)null);
  private static final Map<String, PriorityRateLimiter> RATE_LIMITERS = new HashMap<>();

  @Inject private ImageStore store;

  @Override
  public CacheResponse get(URI uri, String method, Map<String, List<String>> requestHeaders) throws IOException {
    if(!method.equals("GET")) {
      return null;
    }

    ImageRecord image = store.findImageByURL(uri.toURL()).orElse(null);
    int timeOut = Integer.parseInt(requestHeaders.getOrDefault("!time-out", DEFAULT_TIME_OUT).get(0));
    String safeURL = Optional.ofNullable(requestHeaders.getOrDefault("!safe-url", DEFAULT_NULL).get(0)).orElse(uri.toURL().toString());

    if(image != null && image.getCreationTime().plusSeconds(timeOut).isAfter(LocalDateTime.now())) {
      // fresh enough, return it
      CacheResponse response = decodeCacheResponse(image.getImage(), safeURL);

      if(response != null) {
        return response;
      }

      image = null;  // Ensures that if decoding failed, it isn't tried again later.
    }

    // Either entry was missing, wasn't fresh enough, or it couldn't be decoded.  Fetch from original source:
    LOGGER.fine("Direct fetch (uncached): " + safeURL);

    PriorityRateLimiter rateLimiter = determineRateLimiter(requestHeaders.get("!rate-limit"));

    if(rateLimiter != null) {
      rateLimiter.acquire();
    }

    URLConnection conn = uri.toURL().openConnection();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();

    // Prevent infinite loop:
    conn.setUseCaches(false);

    Map<String, List<String>> modifiedHeaderFields = new HashMap<>(conn.getHeaderFields());

    // Serializing null is not allowed (which contains the request type), replace with empty string:
    modifiedHeaderFields.put("", modifiedHeaderFields.remove(null));

    OBJECT_MAPPER.writeValue(headerBuf, modifiedHeaderFields);

    baos.write(ByteBuffer.allocate(8).putInt(ID).putInt(headerBuf.size()).array());
    headerBuf.writeTo(baos);
    conn.getInputStream().transferTo(baos);

    byte[] buf = baos.toByteArray();

    if(!(conn instanceof HttpURLConnection) || ((HttpURLConnection)conn).getResponseCode() == 200) {
      // Store the result, if it was succesful:
      store.store(uri.toURL(), buf);
    }
    else if(image != null) {
      // If unsuccesful, for whatever reason, and there is an (expired) cached response, return that:
      LOGGER.warning("Direct fetch failed, falling back to cache: " + safeURL);

      CacheResponse response = decodeCacheResponse(image.getImage(), safeURL);

      if(response != null) {
        return response;
      }
    }

    return new CacheResponse() {
      @Override
      public Map<String, List<String>> getHeaders() throws IOException {
        return conn.getHeaderFields();
      }

      @Override
      public InputStream getBody() throws IOException {
        return new ByteArrayInputStream(buf, 8 + headerBuf.size(), buf.length - 8 - headerBuf.size());
      }
    };
  }

  private static synchronized PriorityRateLimiter determineRateLimiter(List<String> list) {
    if(list == null || list.isEmpty()) {
      return null;
    }

    String key = list.get(0);

    return RATE_LIMITERS.computeIfAbsent(key, k -> {
      String[] parts = k.split(";");

      return new PriorityRateLimiter(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    });
  }

  private static CacheResponse decodeCacheResponse(byte[] data, String safeURL) {
    try {
      ByteBuffer bb = ByteBuffer.wrap(data, 0, 8);

      if(bb.getInt() != ID) {
        LOGGER.warning("Cache entry was invalid (old entry, should not occur anymore after a while?) for: " + safeURL);

        return null;
      }

      int size = bb.getInt();

      @SuppressWarnings("unchecked")
      Map<String, List<String>> headers = OBJECT_MAPPER.readValue(new ByteArrayInputStream(data, 8, size), Map.class);

      LOGGER.fine("Retrieved from Cache: " + safeURL);

      headers.put(null, headers.remove(""));

      return new CacheResponse() {
        @Override
        public Map<String, List<String>> getHeaders() throws IOException {
          return headers;
        }

        @Override
        public InputStream getBody() throws IOException {
          return new ByteArrayInputStream(data, 8 + size, data.length - 8 - size);
        }
      };
    }
    catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception while decoding cache entry for: " + safeURL, e);

      return null;
    }
  }

  @Override
  public CacheRequest put(URI uri, URLConnection conn) throws IOException {
    return null;  // Storage is handled directly in #get
  }
}
