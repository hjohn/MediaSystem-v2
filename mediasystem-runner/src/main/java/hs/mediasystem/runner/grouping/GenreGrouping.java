package hs.mediasystem.runner.grouping;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.scanner.api.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenreGrouping implements Grouping<Production> {
  private static final ResourceManager RM = new ResourceManager(GenreGrouping.class);
  private static final DataSource DATA_SOURCE = DataSource.instance(MediaType.of("GROUPING"), "GENRE");

  @Inject private MediaItem.Factory factory;

  @Override
  public List<MediaItem<MediaDescriptor>> group(List<MediaItem<Production>> items) {
    Map<String, List<MediaItem<? extends MediaDescriptor>>> map = new HashMap<>();

    for(MediaItem<? extends Production> item : items) {
      for(String genre : item.getData().getGenres()) {
        map.computeIfAbsent(genre, k -> new ArrayList<>()).add(item);
      }
    }

    List<MediaItem<MediaDescriptor>> topLevelItems = new ArrayList<>();

    for(Map.Entry<String, List<MediaItem<? extends MediaDescriptor>>> entry : map.entrySet()) {
      Details details = new Details(entry.getKey(), RM.getText(entry.getKey().toLowerCase(), "description"), null, null, null);

      GroupDescriptor groupDescriptor = new GroupDescriptor(new Identifier(DATA_SOURCE, entry.getKey()), details);

      MediaItem<MediaDescriptor> parent = factory.createParent(
        groupDescriptor,
        entry.getValue()
      );

      topLevelItems.add(parent);
    }

    return topLevelItems;
  }
}
