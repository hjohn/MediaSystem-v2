package hs.mediasystem.runner.db;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Release;
import hs.mediasystem.runner.collection.CollectionDefinition;
import hs.mediasystem.runner.collection.CollectionLocationManager;
import hs.mediasystem.scanner.api.MediaType;
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
  private static final Comparator<MediaDescriptor> RATING_COMPARATOR = Comparator.comparing((MediaDescriptor d) -> d instanceof Release ? Optional.ofNullable(((Release)d).getReception()).map(Reception::getRating).orElse(0.0) : 0.0).reversed();

  @Inject private CollectionLocationManager manager;
  @Inject private MediaService mediaService;

  public List<Collection> findCollections() {
    List<Collection> collections = new ArrayList<>();

    for(CollectionDefinition collectionDefinition : manager.getCollectionDefinitions()) {
      MediaType mediaType = MediaType.of(collectionDefinition.getType().toUpperCase());
      List<MediaDescriptor> descriptors = mediaService.findAllByType(mediaType, collectionDefinition.getTag(), List.of("TMDB", "LOCAL"));
      Collections.sort(descriptors, RATING_COMPARATOR);

      String uris = descriptors.stream()
        .map(MediaDescriptor::getDetails)
        .map(Details::getImage)
        .flatMap(Optional::stream)
        .map(Object::toString)
        .limit(3)
        .collect(Collectors.joining(","));

      Optional<ImageURI> backgroundImage = descriptors.stream()
        .map(MediaDescriptor::getDetails)
        .map(Details::getBackdrop)
        .flatMap(Optional::stream)
        .findFirst();

      collections.add(new Collection(new Details(
        collectionDefinition.getTitle(),
        null,
        null,
        uris.isEmpty() ? null : new ImageURI("multi:landscape:" + uris),
        backgroundImage.orElse(null)
      ), collectionDefinition));
    }

    return collections;
  }
}
