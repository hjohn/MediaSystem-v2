package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.AbstractPresentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.ObjectProperty;

import javax.inject.Inject;

public class EpisodesPresentation extends AbstractPresentation {
  private static final String SYSTEM = "MediaSystem:Episode";

  @Inject private LocalMediaManager localMediaManager;
  @Inject private StreamStateService streamStateService;
  @Inject private SettingsStore settingsStore;

  private final List<MediaItem<Episode>> internalEpisodeItems = new ArrayList<>();

  public final List<MediaItem<Episode>> episodeItems = Collections.unmodifiableList(internalEpisodeItems);
  public final ObjectProperty<MediaItem<Episode>> episodeItem = objectProperty();

  public EpisodesPresentation set(MediaItem<Serie> serieItem) {
    Serie serieDescriptor = serieItem.getData();

    Map<Integer, Map<Integer, Set<MediaStream<?>>>> serieIndex = createSerieIndex(serieItem);

    serieDescriptor.getSeasons().stream()
      .sorted((s1, s2) -> Integer.compare(s1.getNumber() == 0 ? Integer.MAX_VALUE : s1.getNumber(), s2.getNumber() == 0 ? Integer.MAX_VALUE : s2.getNumber()))
      .flatMap(s -> s.getEpisodes().stream())
      .map(e -> wrap(serieItem, e, serieIndex))
      .forEachOrdered(internalEpisodeItems::add);

    String settingKey = "last-selected:" + serieItem.getId();

    episodeItem.addListener((obs, old, current) -> settingsStore.storeSetting(SYSTEM, settingKey, current.getId()));

    String id = settingsStore.getSetting(SYSTEM, settingKey);

    outer:
    for(;;) {
      if(id != null) {
        for(int i = 0; i < episodeItems.size(); i++) {
          MediaItem<Episode> mediaItem = episodeItems.get(i);

          if(id.equals(mediaItem.getId())) {
            episodeItem.set(mediaItem);
            break outer;
          }
        }
      }

      episodeItem.set(episodeItems.get(0));
      break;
    }

    return this;
  }

  private Map<Integer, Map<Integer, Set<MediaStream<?>>>> createSerieIndex(MediaItem<?> serieItem) {
    Map<Integer, Map<Integer, Set<MediaStream<?>>>> streamsByEpisodeBySeason = new HashMap<>();

    if(serieItem.getStreams().isEmpty()) {
      return streamsByEpisodeBySeason;
    }

    Set<MediaStream<?>> episodeStreams = localMediaManager.findChildren(serieItem.getStreams().iterator().next().getUri());

    for(MediaStream<?> stream : episodeStreams) {
      String sequenceAttribute = (String)stream.getAttributes().get(Attribute.SEQUENCE);

      if(sequenceAttribute != null) {
        String[] parts = sequenceAttribute.split(",");

        if(parts.length == 2) {
          int seasonNumber = Integer.parseInt(parts[0]);
          String[] numbers = parts[1].split("-");

          for(int i = Integer.parseInt(numbers[0]); i <= Integer.parseInt(numbers[numbers.length - 1]); i++) {
            streamsByEpisodeBySeason.computeIfAbsent(seasonNumber, k -> new HashMap<>()).computeIfAbsent(i, k -> new HashSet<>()).add(stream);
          }
        }
        else {
          int episodeNumber = Integer.parseInt(parts[0]);

          streamsByEpisodeBySeason.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(episodeNumber, k -> new HashSet<>()).add(stream);
        }
      }
      else {
        streamsByEpisodeBySeason.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(0, k -> new HashSet<>()).add(stream);
      }
    }

    return streamsByEpisodeBySeason;
  }

  private MediaItem<Episode> wrap(MediaItem<Serie> serieItem, Episode data, Map<Integer, Map<Integer, Set<MediaStream<?>>>> streamsByEpisodeBySeason) {
    Set<MediaStream<?>> streams = Optional.ofNullable(streamsByEpisodeBySeason.get(data.getSeasonNumber())).map(m -> m.get(data.getNumber())).orElse(Collections.emptySet());

    return new MediaItem<>(
      data,
      serieItem,
      streams,
      countWatchedStreams(streams),
      streams.isEmpty() ? 0 : 1
    );
  }

  private int countWatchedStreams(Collection<MediaStream<?>> streams) {
    for(MediaStream<?> stream : streams) {
      if(streamStateService.isWatched(stream.getStreamPrint())) {
        return 1;
      }
    }

    return 0;
  }
}
