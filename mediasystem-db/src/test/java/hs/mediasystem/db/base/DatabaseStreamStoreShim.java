package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import javax.inject.Inject;

public class DatabaseStreamStoreShim {
  private final DatabaseStreamStore store;

  @Inject
  public DatabaseStreamStoreShim(DatabaseStreamStore store) {
    this.store = store;
  }

  public void put(Streamable streamable) {
    store.put(streamable);
  }
}
