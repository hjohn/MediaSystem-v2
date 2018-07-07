package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.QueryService;
import hs.mediasystem.ext.basicmediatypes.QueryService.Result;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.StringURI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalMediaManager {
  private static final Logger LOGGER = Logger.getLogger(LocalMediaManager.class.getName());
  private static final Executor EXECUTOR = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("LocalMediaManager", true));

  @Inject private List<IdentificationService<?>> identificationServices;
  @Inject private List<QueryService<?>> queryServices;

  private final Map<DataSource, IdentificationService<?>> identificationServicesByDataSource = new HashMap<>();
  private final Map<DataSource, QueryService<?>> queryServicesByDataSource = new HashMap<>();
  private final List<LocalMediaListener> localMediaListeners = new ArrayList<>();

  // Various MediaStream indices
  private final Map<Type, Map<String, MediaStream<? extends MediaDescriptor>>> streamsByPrintIdentifierByType = new HashMap<>();  // By Type, then by StreamPrint.Identifier
  private final Map<Identifier, Set<MediaStream<?>>> groupsByIdentifier = new HashMap<>();
  private final Map<StringURI, Set<MediaStream<?>>> streamsByParentURI = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    LOGGER.info("Instantiated with " + identificationServices.size() + " identification services and " + queryServices.size() + " query services");

    for(IdentificationService<?> service : identificationServices) {
      if(identificationServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple identification services available for datasource: " + service.getDataSource());
      }
    }
    for(QueryService<?> service : queryServices) {
      if(queryServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  public <D extends MediaDescriptor> MediaStream<D> add(MediaStream<D> mediaStream) {
    MediaStream<D> replacedStream = addInternal(mediaStream);

    if(replacedStream != null) {
      localMediaListeners.stream().forEach(l -> l.mediaRemoved(replacedStream));
    }

    localMediaListeners.stream().forEach(l -> l.mediaAdded(mediaStream));

    return replacedStream;
  }

  public <D extends MediaDescriptor> boolean remove(MediaStream<D> mediaStream) {
    return removeMediaStream(mediaStream) != null;
  }

  public void addListener(LocalMediaListener listener) {
    localMediaListeners.add(listener);
  }

  public void removeListener(LocalMediaListener listener) {
    localMediaListeners.remove(listener);
  }

  private <D extends MediaDescriptor> MediaStream<D> addInternal(MediaStream<D> mediaStream) {
    synchronized(this) {
      MediaStream<D> replacedStream = removeMediaStream(mediaStream);

      addMediaStream(mediaStream);
      enrichMediaStream(mediaStream);

      return replacedStream;
    }
  }

  // TODO how to return another ID from a service?
//  public synchronized Collection<MediaStream<?>> findAllByType(Type type) {
//    return groupListsByType.get(type).values();
//  }

  public synchronized <T extends MediaDescriptor> Collection<MediaStream<T>> findAllByType(Type type) {
    @SuppressWarnings("unchecked")  // Save, as the type guarantees the kind of items in the map
    Map<String, MediaStream<T>> map = (Map<String, MediaStream<T>>)(Map<?, ?>)streamsByPrintIdentifierByType.get(type);

    return map == null ? Collections.emptyList() : map.values();
  }


  public synchronized Set<MediaStream<?>> findChildren(StringURI parentUri) {
    return streamsByParentURI.getOrDefault(parentUri, Collections.emptySet());
  }

  public synchronized Set<MediaStream<?>> find(ProductionIdentifier identifier) {
    Set<MediaStream<?>> set = groupsByIdentifier.get(identifier);

    return set == null ? Collections.emptySet() : new HashSet<>(set);
  }

  @SuppressWarnings("unchecked")
  private <D extends MediaDescriptor> void enrichMediaStream(MediaStream<D> mediaStream) {
    Type type = mediaStream.getType();

    Set<DataSource> identifiedDataSources = mediaStream.getMediaRecords().keySet().stream()
      .map(Identifier::getDataSource)
      .collect(Collectors.toSet());

    identificationServicesByDataSource.entrySet().stream()
      .filter(e -> e.getKey().getType().equals(type))  // Filters by type, this makes the cast to IdentificationService<T> safe.
      .filter(e -> !identifiedDataSources.contains(e.getKey()))
      .forEach(e -> EXECUTOR.execute(() -> identify(mediaStream, (IdentificationService<MediaStream<D>>)e.getValue())));

    mediaStream.getMediaRecords().values().stream()
      .filter(e -> e.getMediaDescriptor() == null)  // Only Records that have no entry
      .filter(e -> queryServicesByDataSource.containsKey(e.getDataSource()))  // Checks that a QueryService matching the DataSource exists
      .forEach(e -> EXECUTOR.execute(() -> query(mediaStream, e.getIdentification(), (QueryService<D>)queryServicesByDataSource.get(e.getDataSource()))));
  }

  private <D extends MediaDescriptor> void identify(MediaStream<D> mediaStream, IdentificationService<MediaStream<D>> service) {
    try {
      LOGGER.fine("Identifying using " + service.getClass().getName() + ": " + mediaStream);

      Identification identification = service.identify(mediaStream);

      if(identification != null) {
        synchronized(this) {
          mediaStream.mergeMediaRecord(new MediaRecord<D>(identification, null));
          groupsByIdentifier.computeIfAbsent(identification.getIdentifier(), k -> new HashSet<>()).add(mediaStream);
        }

        localMediaListeners.stream().forEach(l -> l.mediaUpdated(mediaStream));

        @SuppressWarnings("unchecked")
        QueryService<D> queryService = (QueryService<D>)queryServicesByDataSource.get(service.getDataSource());

        if(queryService != null) {
          query(mediaStream, identification, queryService);
        }
      }
      else {
        LOGGER.warning("Unable to identify: " + mediaStream + " with DataSource: " + service.getDataSource());
      }
    }
    catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception while identifying: " + mediaStream, e);
    }
  }

  private <D extends MediaDescriptor> void query(MediaStream<D> mediaStream, Identification identification, QueryService<D> service) {
    try {
      LOGGER.fine("Querying using " + service.getClass().getName() + ": " + mediaStream);

      Result<D> result = service.query(identification.getIdentifier());

      synchronized(this) {
        mediaStream.mergeMediaRecord(new MediaRecord<>(identification, result.getMediaDescriptor()));

        for(Identification newIdentification : result.getNewIdentifications()) {
          mediaStream.mergeMediaRecord(new MediaRecord<D>(newIdentification, null));
          groupsByIdentifier.computeIfAbsent(newIdentification.getIdentifier(), k -> new HashSet<>()).add(mediaStream);
        }
      }

      localMediaListeners.stream().forEach(l -> l.mediaUpdated(mediaStream));
    }
    catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception while querying: " + identification.getIdentifier(), e);
    }
  }

  private void addMediaStream(MediaStream<?> mediaStream) {
    streamsByPrintIdentifierByType.computeIfAbsent(mediaStream.getType(), k -> new HashMap<>()).put(mediaStream.getStreamPrint().getIdentifier(), mediaStream);
    mediaStream.getMediaRecords().keySet().stream().forEach(k -> groupsByIdentifier.computeIfAbsent(k, key -> new HashSet<>()).add(mediaStream));

    if(mediaStream.getParentUri() != null) {
      streamsByParentURI.computeIfAbsent(mediaStream.getParentUri(), k -> new HashSet<>()).add(mediaStream);
    }
  }

  private <D extends MediaDescriptor> MediaStream<D> removeMediaStream(MediaStream<D> mediaStream) {
    @SuppressWarnings("unchecked")
    MediaStream<D> removedStream = (MediaStream<D>)streamsByPrintIdentifierByType.computeIfAbsent(mediaStream.getType(), k -> new HashMap<>()).remove(mediaStream.getStreamPrint().getIdentifier());

    if(removedStream != null) {
      removedStream.getMediaRecords().keySet().stream().forEach(k -> groupsByIdentifier.computeIfAbsent(k, key -> new HashSet<>()).remove(removedStream));

      if(mediaStream.getParentUri() != null) {
        streamsByParentURI.computeIfAbsent(mediaStream.getParentUri(), k -> new HashSet<>()).remove(mediaStream);
      }
    }

    return removedStream;
  }

    // Where are we gonna put Path (the only thing special about DB entries is that they have an associated stream!) ? TODO

    // Interesting case: Twice the same movie on disk, but ofcourse path/size/hash are likely to differ... they will however identify as the same TMDB-ID
    // - merge them?  DB must be able to load and handle it as a merged result then...


    // Local Media Scanners need to add a local media with a **stable** Identifier.
    // In order to do this:
    // - Take path and size, check with a scanner-specific database part if those are known
    // - If known, grab the associated UUID/hash/id and use that as Identifier
    // - If not known, do quick hash of contents, check if hash + size is known
    // - If known (happens on renames or network path changes), update path/name, and grab associated id
    // - If not known, create new entry with new id
    //
    // This process is unique to the scanner to provide stable id's.  The DB does not care!

    // In order to add something to DB we need:
    // - An Identifier (which can be used to fetch cached related information)
    // - An IdData structure (for running other identifications)
    // - A, filled as best as possible, Entry structure -- it's also possible to
    //
    // Alternatively...
    // - Only an IdData structure, then run an IdentificationService on it (but which one?)
    // - IdentificationService provides Identifiers (even for local)  ---> won't work, IdData structure would have to contain IMDB ID / Path...
    // - Query service provides Entry (even for local)
    // Although this forces local media to be handled in the same fashion as, say TMDB, it does
    // seem a bit convuluted since all this information is probably known already when IdData is
    // created...
    //
    // Example:
    // 1) Scanner disects a filename and creates IdData (which would have to include full path and file size)
    // 2) In parallel several ID services


    // - MediaKey contains type as well.  Medias map contains all keys, but can refer to same values.
    // - When enriching, a DataEntry is returned.
    // - When a local media disappears, all entries with the same localmedia need to be deleted (slow, no reverse map available, yet)
    // - Lists need to be maintained on adds/deletes
    // - Fields from Media are filled by copying data from associated sub-media (LocalMedia, TMDBMedia, etc.) -- every source manipulates its own data object, never Media directly.



//  public synchronized void removeLocalMedia(StreamKey key) {
//
//  }

  // All Local Media of type X as Media's...
  // Top 100...
  // Recent releases...
  // Media from other server...


//  public synchronized Media find(StreamKey key) {
//    return medias.get(key);
//  }
}
