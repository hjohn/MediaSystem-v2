package hs.mediasystem.runner.grouping;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CollectionGrouping implements Grouping<Production> {
  @Inject private MediaItem.Factory factory;
  @Inject private VideoDatabase videoDatabase;

  @Override
  @SuppressWarnings("unchecked")
  public List<MediaItem<MediaDescriptor>> group(List<MediaItem<Production>> items) {
    Set<Identifier> collectionIdentifiers = new HashSet<>();
    List<MediaItem<MediaDescriptor>> topLevelItems = new ArrayList<>();

    for(MediaItem<? extends Production> mediaItem : items) {
      mediaItem.getData().getCollectionIdentifier().ifPresentOrElse(collectionIdentifiers::add, () -> topLevelItems.add((MediaItem<MediaDescriptor>)mediaItem));
    }

    for(Identifier collectionIdentifier : collectionIdentifiers) {
      ProductionCollection pc = videoDatabase.queryProductionCollection(collectionIdentifier);

      topLevelItems.add(factory.createParent(pc, createChildren(pc)));
    }

    return topLevelItems;
  }

  private List<MediaItem<? extends MediaDescriptor>> createChildren(ProductionCollection pc) {
    return pc.getItems().stream()
      .map(p -> factory.create(p, null))
      .collect(Collectors.toUnmodifiableList());
  }
}
