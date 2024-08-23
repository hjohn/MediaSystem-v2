package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.db.core.LinkedWorksService;
import hs.mediasystem.db.core.domain.LinkedWork;
import hs.mediasystem.db.services.collection.CollectionLocationManager;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.domain.work.CollectionDefinition;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.util.image.ImageURI;

import java.time.LocalDate;
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
  private static final Comparator<LinkedWork> WEIGHTED_RATING_COMPARATOR = Comparator.comparing(CollectionService::score).reversed();

  @Inject private CollectionLocationManager manager;
  @Inject private LinkedWorksService linkedWorksService;

  public List<Collection> findCollections() {
    List<Collection> collections = new ArrayList<>();

    for(CollectionDefinition collectionDefinition : manager.getCollectionDefinitions()) {
      MediaType mediaType = MediaType.valueOf(collectionDefinition.type().toUpperCase());
      List<LinkedWork> works = linkedWorksService.findAllByType(mediaType, collectionDefinition.tag());
      Collections.sort(works, WEIGHTED_RATING_COMPARATOR);

      String uris = works.stream()
        .map(LinkedWork::workDescriptor)
        .map(WorkDescriptor::getDetails)
        .map(Details::getCover)
        .flatMap(Optional::stream)
        .map(Object::toString)
        .limit(3)
        .collect(Collectors.joining("|"));

      Optional<ImageURI> backgroundImage = works.stream()
        .map(LinkedWork::workDescriptor)
        .map(WorkDescriptor::getDetails)
        .map(Details::getBackdrop)
        .flatMap(Optional::stream)
        .findFirst();

      collections.add(new Collection(
        collectionDefinition.title(),
        Optional.ofNullable(uris.isEmpty() ? null : new ImageURI("multi:landscape:" + uris, null)),
        backgroundImage,
        collectionDefinition
      ));
    }

    return collections;
  }

  private static double score(LinkedWork linkedWork) {
    if(linkedWork.workDescriptor() instanceof Production production) {
      Reception reception = production.getReception();
      LocalDate date = production.getDetails().getDate().orElse(null);

      if(reception != null && date != null && !Boolean.TRUE.equals(production.getClassification().pornographic())) {
        return reception.rating() + date.getYear() * 0.25;
      }
    }

    return Double.NEGATIVE_INFINITY;
  }
}
