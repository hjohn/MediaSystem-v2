package hs.mediasystem.db.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Resolution;
import hs.mediasystem.domain.work.Snapshot;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.mediamanager.StreamableStore;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SerieHelper {
  private static final DataSource LOCAL_RELEASE = DataSource.instance(MediaType.of("RELEASE"), "LOCAL");
  private static final MediaType EPISODE = MediaType.of("EPISODE");

  @Inject private StreamableStore streamStore;
  @Inject private StreamMetaDataStore metaDataProvider;

  public List<Episode> createExtras(Serie serie, StreamID streamId) {

    /*
     * For a serie, additional information is added for video files found in the same folder, but
     * which were unable to be matched to a Descriptor that is part of the Serie.  These are "Extras".
     */

    List<Episode> extras = new ArrayList<>();
    List<Streamable> children = streamStore.findChildren(streamId).stream()
        .sorted((a, b) -> a.getAttributes().<String>get(Attribute.TITLE).compareTo(b.getAttributes().get(Attribute.TITLE)))
        .collect(Collectors.toList());

    int ep = 1;

    for(Streamable child : children) {
      if(!streamStore.findIdentification(child.getId()).isPresent()) {
        // Create an Extra for it:
        extras.add(new Episode(
          createLocalId(serie, child),
          createMinimalDetails(child),
          null,
          null,
          -1,
          ep++,
          Collections.emptyList()
        ));
      }
    }

    return extras;
  }

  public Details createMinimalDetails(Streamable streamable) {
    Optional<StreamMetaData> metaData = metaDataProvider.find(streamable.getId());
    String subtitle = streamable.getAttributes().get(Attribute.SUBTITLE);

    ImageURI image;
    ImageURI backdrop = null;

    if(streamable.getType().equals(EPISODE)) {
      image = metaData.map(StreamMetaData::getSnapshots).filter(list -> list.size() > 1).map(list -> list.get(1)).map(Snapshot::getImageUri).orElse(null);
    }
    else {
      image = metaData.map(SerieHelper::createImage).orElse(null);
      backdrop = metaData.map(StreamMetaData::getSnapshots).filter(list -> list.size() > 2).map(list -> list.get(2)).map(Snapshot::getImageUri).orElse(null);
    }

    return new Details(
      streamable.getAttributes().get(Attribute.TITLE) + (subtitle == null ? "" : ": " + subtitle),
      null,
      null,
      image,
      backdrop
    );
  }

  private static ImageURI createImage(StreamMetaData metaData) {
    List<Snapshot> snapshots = metaData.getSnapshots();

    if(metaData.getVideoStreams().isEmpty() || snapshots.size() < 2) {
      return null;
    }

    Resolution resolution = metaData.getVideoStreams().get(0).getResolution();

    int h = 900;
    int w = h * resolution.getWidth() / resolution.getHeight() / 2;
    int x = (600 - w) / 2;

    return new ImageURI("multi:600,900;" + x + ",0," + w + ",450;" + x + ",450," + w + ",450:" + snapshots.get(0).getImageUri().getUri() + "," + snapshots.get(1).getImageUri().getUri());
  }

  private static LocalEpisodeIdentifier createLocalId(Serie serie, Streamable stream) {
    return new LocalEpisodeIdentifier(LOCAL_RELEASE, serie.getIdentifier().getId() + "/" + stream.getId().asInt(), serie.getIdentifier());
  }

  private static class LocalEpisodeIdentifier extends EpisodeIdentifier {
    private final Identifier rootIdentifier;

    public LocalEpisodeIdentifier(DataSource dataSource, String id, Identifier rootIdentifier) {
      super(dataSource, id);

      this.rootIdentifier = rootIdentifier;
    }

    @Override
    public Identifier getRootIdentifier() {
      return rootIdentifier;
    }
  }
}
