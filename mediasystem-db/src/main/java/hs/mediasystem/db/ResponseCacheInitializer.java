package hs.mediasystem.db;

import hs.mediasystem.db.base.DatabaseResponseCache;

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
