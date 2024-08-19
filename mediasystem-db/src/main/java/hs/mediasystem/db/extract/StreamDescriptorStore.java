package hs.mediasystem.db.extract;

import hs.mediasystem.db.jackson.RecordSerializer;
import hs.mediasystem.domain.media.StreamDescriptor;
import hs.mediasystem.domain.stream.ContentID;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.api.CheckedDatabase;
import org.int4.db.core.reflect.Reflector;
import org.int4.db.core.reflect.TypeConverter;

@Singleton
public class StreamDescriptorStore {
  private record StreamDescriptorRecord(ContentID contentId, StreamDescriptor descriptor, Instant createTime, Instant updateTime) {}

  private static final Lookup LOOKUP = MethodHandles.lookup();
  private static final RecordSerializer<StreamDescriptor> RECORD_SERIALIZER = new RecordSerializer<>(StreamDescriptor.class);
  private static final TypeConverter<StreamDescriptor, byte[]> DESCRIPTOR_CONVERTER = TypeConverter.of(byte[].class, RECORD_SERIALIZER::serialize, RECORD_SERIALIZER::unserialize);
  private static final TypeConverter<ContentID, Integer> CONTENT_ID_CONVERTER = TypeConverter.of(Integer.class, ContentID::asInt, ContentID::new);
  private static final Reflector<StreamDescriptorRecord> ALL = Reflector.of(LOOKUP, StreamDescriptorRecord.class)
    .addTypeConverter(ContentID.class, CONTENT_ID_CONVERTER)
    .addTypeConverter(StreamDescriptor.class, DESCRIPTOR_CONVERTER);

  @Inject private CheckedDatabase database;

  Optional<StreamDescriptor> find(ContentID contentId) throws SQLException {
    return database.query(tx ->
      tx."SELECT \{ALL} FROM stream_descriptors WHERE content_id = \{contentId.asInt()}"
        .map(ALL)
        .getOptional()
        .map(StreamDescriptorRecord::descriptor)
    );
  }

  void store(ContentID contentId, StreamDescriptor descriptor) throws SQLException {
    Instant time = Instant.now();

    database.accept(tx ->
      tx."""
        INSERT INTO stream_descriptors (\{ALL}) VALUES (\{ALL.values(new StreamDescriptorRecord(contentId, descriptor, time, time))})
          ON CONFLICT (content_id)
          DO UPDATE SET descriptor = EXCLUDED.descriptor, update_time = EXCLUDED.update_time
      """.execute()
    );
  }

  public byte[] readSnapshot(ContentID contentId, int index) throws SQLException {
    return database.query(tx ->
      tx."SELECT image FROM stream_descriptor_snapshots WHERE content_id = \{contentId.asInt()} AND index = \{index}"
        .asBytes().getOptional()
        .orElse(null)
    );
  }

  boolean existsSnapshot(ContentID contentId, int index) throws SQLException {
    return database.query(tx ->
      tx."SELECT COUNT(*) FROM stream_descriptor_snapshots WHERE content_id = \{contentId.asInt()} AND index = \{index}"
        .asInt().get() > 0
    );
  }

  void storeImage(ContentID contentId, int index, byte[] image) throws SQLException {
    database.accept(tx ->
      tx."INSERT INTO stream_descriptor_snapshots (content_id, index, image) VALUES (\{contentId.asInt()}, \{index}, \{image})".execute()
    );
  }
}
