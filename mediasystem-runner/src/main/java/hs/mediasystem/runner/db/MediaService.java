package hs.mediasystem.runner.db;

import hs.mediasystem.db.StreamCacheUpdateService;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.mediamanager.BasicStreamStore;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.mediamanager.LocalSerie;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaService {
  @Inject private BasicStreamStore streamStore;  // Only stores top level items, not children
  @Inject private DescriptorStore descriptorStore;
  @Inject private StreamCacheUpdateService updateService;  // TODO doesn't belong here
  @Inject private SerieHelper serieHelper;

  public Set<BasicStream> findStreams(Identifier identifier) {
    Identifier rootIdentifier = identifier.getRootIdentifier();

    if(rootIdentifier != null) {  // Special case, child identifiers are not in StreamStore, use helper
      return serieHelper.findChildStreams(rootIdentifier, identifier);
    }

    return streamStore.findStreams(identifier);
  }

  public MediaIdentification reidentify(StreamID streamId) {
    return updateService.reidentifyStream(streamId);
  }

  public <D extends MediaDescriptor> List<D> findAllByType(MediaType type, List<String> dataSourceNames) {
    return findAllByType(type, null, dataSourceNames);
  }

  public <D extends MediaDescriptor> List<D> findAllByType(MediaType type, String tag, List<String> dataSourceNames) {
    List<D> descriptors = new ArrayList<>();

    for(Map.Entry<BasicStream, Map<Identifier, Identification>> entry : streamStore.findIdentifiersByStreams(type, tag).entrySet()) {
      @SuppressWarnings("unchecked")
      D descriptor = (D)findDataSourcePreferredDescriptor(entry.getValue().keySet(), dataSourceNames);

      if(descriptor != null) {     // TODO should generate LOCAL entries on demand for Streams without any Descriptor
        descriptors.add(descriptor);  // TODO this can add duplicate descriptors if there are 2 streams that identify to same thing
      }
    }

    return descriptors;
  }

  public LocalSerie toLocalSerie(Serie serie) {
    return serieHelper.toLocalSerie(serie);
  }

  public Identification getIdentification(StreamID streamId, List<String> dataSourceNames) {
    Identification bestIdentification = null;
    int bestIndex = Integer.MAX_VALUE;

    Map<Identifier, Identification> identifications = streamStore.findIdentifications(streamId);

    if(identifications != null) {
      for(Map.Entry<Identifier, Identification> entry : identifications.entrySet()) {
        if(entry.getValue() != null) {
          int index = dataSourceNames.indexOf(entry.getKey().getDataSource().getName());

          if(index >= 0 && index < bestIndex) {
            bestIndex = index;
            bestIdentification = entry.getValue();
          }
        }
      }
    }

    return bestIdentification;
  }

  private MediaDescriptor findDataSourcePreferredDescriptor(Set<Identifier> set, List<String> dataSourceNames) {
    MediaDescriptor bestDescriptor = null;
    int bestIndex = Integer.MAX_VALUE;

    for(Identifier identifier : set) {
      MediaDescriptor descriptor = descriptorStore.get(identifier);

      if(descriptor != null) {
        int index = dataSourceNames.indexOf(identifier.getDataSource().getName());

        if(index >= 0 && index < bestIndex) {
          bestIndex = index;
          bestDescriptor = descriptor;
        }
      }
    }

    return bestDescriptor;
  }
}
