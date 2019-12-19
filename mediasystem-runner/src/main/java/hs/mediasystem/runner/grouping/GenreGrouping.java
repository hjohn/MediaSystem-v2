package hs.mediasystem.runner.grouping;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenreGrouping implements Grouping<Production> {
  private static final ResourceManager RM = new ResourceManager(GenreGrouping.class);
  private static final DataSource DATA_SOURCE = DataSource.instance(MediaType.of("GROUPING"), "GENRE");
  private static final Comparator<MediaItem<Production>> RATING_COMPARATOR = Comparator.comparing((MediaItem<Production> mi) -> Optional.of(mi.getData()).map(Release::getReception).map(Reception::getRating).orElse(0.0)).reversed();

  @Inject private MediaItem.Factory factory;

  @Override
  public List<MediaItem<MediaDescriptor>> group(List<MediaItem<Production>> items) {
    Map<String, List<MediaItem<Production>>> map = new HashMap<>();

    for(MediaItem<Production> item : items) {
      for(String genre : item.getData().getGenres()) {
        map.computeIfAbsent(genre, k -> new ArrayList<>()).add(item);
      }
    }

    List<MediaItem<MediaDescriptor>> topLevelItems = new ArrayList<>();

    for(Map.Entry<String, List<MediaItem<Production>>> entry : map.entrySet()) {
      Comparator<MediaItem<Production>> majorGenreComparator = Comparator.comparing((MediaItem<Production> mi) -> {
        int index = mi.getData().getGenres().indexOf(entry.getKey());

        return index == -1 ? Integer.MAX_VALUE : index;
      });

      AtomicReference<ImageURI> backgroundURIRef = new AtomicReference<>();
      String uris = entry.getValue().stream()
        .sorted(majorGenreComparator.thenComparing(RATING_COMPARATOR))
        .peek(mi -> {  // This is dirty
          if(backgroundURIRef.get() == null) {
            mi.getDetails().getBackdrop().ifPresent(backgroundURIRef::set);
          }
        })
        .map(MediaItem::getDetails)
        .map(Details::getImage)
        .flatMap(Optional::stream)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .limit(4)
        .collect(Collectors.joining(","));

      Details details = new Details(entry.getKey(), RM.getText(entry.getKey().toLowerCase(), "description"), null, uris.isEmpty() ? null : new ImageURI("multi::" + uris), backgroundURIRef.get());

      @SuppressWarnings("unchecked")
      List<MediaItem<? extends MediaDescriptor>> children = (List<MediaItem<? extends MediaDescriptor>>)(List<?>)entry.getValue();
      GroupDescriptor groupDescriptor = new GroupDescriptor(new Identifier(DATA_SOURCE, entry.getKey()), details);

      MediaItem<MediaDescriptor> parent = factory.createParent(groupDescriptor, children);

      topLevelItems.add(parent);
    }

    return topLevelItems;
  }
}
