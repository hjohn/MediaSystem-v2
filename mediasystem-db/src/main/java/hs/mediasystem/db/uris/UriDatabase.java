package hs.mediasystem.db.uris;

import hs.mediasystem.domain.stream.ContentID;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.api.Database;
import org.int4.db.core.api.Transaction;
import org.int4.db.core.reflect.Extractor;
import org.int4.db.core.reflect.Reflector;

@Singleton
public class UriDatabase {
  private static final Reflector<UriRecord> ALL = Reflector.of(UriRecord.class).withNames("id", "content_id", "uri");
  private static final Extractor<UriRecord> EXCEPT_ID = ALL.excluding("id");

  @Inject private Database database;

  public <T, U> Map<T, U> findAll(Function<UriRecord, T> keyMapper, Function<UriRecord, U> valueMapper) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Map<T, U> map = new HashMap<>();

      tx."SELECT \{ALL} FROM uris"
        .map(ALL)
        .consume(r -> map.put(keyMapper.apply(r), valueMapper.apply(r)));

      return map;
    }
  }

  public void store(String uri, int contentId) {
    try(Transaction tx = database.beginTransaction()) {
      tx."""
        INSERT INTO uris (\{EXCEPT_ID}) VALUES (\{contentId}, \{uri})
          ON CONFLICT (uri) DO UPDATE SET content_id = \{contentId}
      """.execute();
      tx.commit();
    }
  }

  public List<String> findUris(int contentId) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx."SELECT uri FROM uris WHERE content_id = \{contentId}"
        .asString()
        .toList();
    }
  }

  public Optional<ContentID> findContentId(URI location) {
    return database.query(tx ->
      tx."SELECT content_id FROM uris WHERE uri = \{location.toString()}"
        .asInt()
        .getOptional()
        .map(ContentID::new)
    );
  }
}
