package hs.mediasystem.db.services;

import hs.mediasystem.db.services.collection.CollectionLocationManager;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.domain.work.CollectionDefinition;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CollectionService {
  private static final Comparator<Work> RATING_COMPARATOR =
      Comparator.comparing((Work r) -> r.getDescriptor() instanceof Release ? Optional.ofNullable(((Release)r.getDescriptor()).getReception()).map(Reception::getRating).orElse(0.0) : 0.0).reversed();

  @Inject private CollectionLocationManager manager;
  @Inject private WorksService worksService;

  public List<Collection> findCollections() {
    List<Collection> collections = new ArrayList<>();

    for(CollectionDefinition collectionDefinition : manager.getCollectionDefinitions()) {
      MediaType mediaType = MediaType.valueOf(collectionDefinition.getType().toUpperCase());
      List<Work> works = worksService.findAllByType(mediaType, collectionDefinition.getTag());
      Collections.sort(works, RATING_COMPARATOR);

      String uris = works.stream()
        .map(Work::getDescriptor)
        .map(MediaDescriptor::getDetails)
        .map(Details::getImage)
        .flatMap(Optional::stream)
        .map(Object::toString)
        .limit(3)
        .collect(Collectors.joining(","));

      Optional<ImageURI> backgroundImage = works.stream()
        .map(Work::getDescriptor)
        .map(MediaDescriptor::getDetails)
        .map(Details::getBackdrop)
        .flatMap(Optional::stream)
        .findFirst();

      collections.add(new Collection(
        collectionDefinition.getTitle(),
        uris.isEmpty() ? null : new ImageURI("multi:landscape:" + uris),
        backgroundImage.orElse(null),
        collectionDefinition
      ));
    }

    return collections;
  }
}
