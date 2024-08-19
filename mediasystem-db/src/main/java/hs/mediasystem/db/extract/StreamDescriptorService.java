package hs.mediasystem.db.extract;

import hs.mediasystem.db.extract.StreamDescriptorFactory.RawDescriptor;
import hs.mediasystem.db.extract.StreamDescriptorFactory.RawSnapshot;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.media.Snapshot;
import hs.mediasystem.domain.media.StreamDescriptor;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.image.ImageURI;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.api.CheckedDatabase;

@Singleton
public class StreamDescriptorService {
  private static final Logger LOGGER = System.getLogger(StreamDescriptorService.class.getName());

  @Inject private UriDatabase uriDatabase;
  @Inject private StreamDescriptorStore store;
  @Inject private StreamDescriptorFactory factory;
  @Inject private CheckedDatabase database;

  public Optional<StreamDescriptor> get(URI location) throws SQLException {
    return CheckedOptional.from(uriDatabase.findContentId(location))
      .flatMapOpt(store::find)
      .toOptional();
  }

  public Optional<StreamDescriptor> create(URI location) {
    return CheckedOptional.from(uriDatabase.findContentId(location))
      .flatMapOpt(this::create)
      .toOptional();
  }

  private Optional<StreamDescriptor> create(ContentID contentId) {
    try {
      File file = uriDatabase.findUris(contentId.asInt()).stream()
        .map(URI::create)
        .map(Paths::get)
        .map(Path::toFile)
        .filter(File::exists)
        .findFirst()
        .orElseThrow(() -> new FileNotFoundException("URI not available or pointed to a missing resource for given content id: " + contentId));

      return Optional.of(createMetaData(contentId, file));
    }
    catch(Exception e) {
      LOGGER.log(Level.WARNING, "Error while storing stream metadata in database for content id " + contentId + ": " + Throwables.formatAsOneLine(e));

      return Optional.empty();
    }
  }

  private StreamDescriptor createMetaData(ContentID contentId, File file) throws Exception {
    LOGGER.log(Level.INFO, "Extracting metadata from: " + file);

    if(file.isDirectory()) {
      StreamDescriptor descriptor = new StreamDescriptor(Optional.empty(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

      store.store(contentId, descriptor);

      return descriptor;
    }

    RawDescriptor rawDescriptor = factory.create(file);
    StreamDescriptor descriptor = new StreamDescriptor(
      rawDescriptor.duration(),
      rawDescriptor.videoTracks(),
      rawDescriptor.audioTracks(),
      rawDescriptor.subtitleTracks(),
      rawDescriptor.snapshots().stream().map(raw -> new Snapshot(new ImageURI("localdb://" + contentId.asInt() + "/" + raw.index(), null), raw.frameNumber())).toList()
    );

    database.accept(tx -> {
      for(RawSnapshot snapshot : rawDescriptor.snapshots()) {
        store.storeImage(contentId, snapshot.index(), snapshot.imageData());
      }

      store.store(contentId, descriptor);
    });

    return descriptor;
  }
}
