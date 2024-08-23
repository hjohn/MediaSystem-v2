package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.db.jackson.RecordSerializer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.api.CheckedDatabase;
import org.int4.db.core.reflect.Reflector;
import org.int4.db.core.reflect.TypeConverter;

/**
 * Stores identifications by local URI.
 *
 * <p>Note: no attempt is made to de-duplicate identifications, there is a simple
 * direction relationship between a file and an identification.
 */
@Singleton
public class IdentificationStore {
  private record IdentificationRecord(URI location, Identification identification, Instant createTime, Instant updateTime) {}

  private static final Lookup LOOKUP = MethodHandles.lookup();
  private static final RecordSerializer<Identification> RECORD_SERIALIZER = new RecordSerializer<>(Identification.class);
  private static final TypeConverter<Identification, byte[]> IDENTIFICATION_CONVERTER = TypeConverter.of(byte[].class, RECORD_SERIALIZER::serialize, RECORD_SERIALIZER::unserialize);
  private static final TypeConverter<URI, String> URI_CONVERTER = TypeConverter.of(String.class, URI::toString, URI::create);
  private static final Reflector<IdentificationRecord> ALL = Reflector.of(LOOKUP, IdentificationRecord.class)
    .addTypeConverter(URI.class, URI_CONVERTER)
    .addTypeConverter(Identification.class, IDENTIFICATION_CONVERTER);

  @Inject private CheckedDatabase database;

  Optional<Identification> find(URI location) throws SQLException {
    return database.query(tx ->
      tx."SELECT \{ALL} FROM identifications WHERE location = \{location.toString()}"
        .map(ALL)
        .getOptional()
        .map(IdentificationRecord::identification)
    );
  }

  void store(URI location, Identification identification) throws SQLException {
    Instant time = Instant.now();

    database.accept(tx ->
      tx."""
        INSERT INTO identifications (\{ALL}) VALUES (\{ALL.values(new IdentificationRecord(location, identification, time, time))})
          ON CONFLICT (location)
          DO UPDATE SET identification = EXCLUDED.identification, update_time = EXCLUDED.update_time
      """.execute()
    );
  }
}
