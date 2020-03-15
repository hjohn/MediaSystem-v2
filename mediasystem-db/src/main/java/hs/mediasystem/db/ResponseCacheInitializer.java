package hs.mediasystem.db;

import java.net.ResponseCache;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class ResponseCacheInitializer {
  @Inject private DatabaseResponseCache databaseResponseCache;

  @PostConstruct
  private void postConstruct() {
    ResponseCache.setDefault(databaseResponseCache);
  }
}
