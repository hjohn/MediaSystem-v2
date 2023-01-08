package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Movie;
import hs.mediasystem.api.datasource.domain.stream.Work;
import hs.mediasystem.db.core.DescriptorService;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.work.Parent;

import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides information about local work without contacting external sources.
 * This service is very fast and should be used when creating large lists of
 * works.
 */
@Singleton
public class LocalWorkService {
  @Inject private LinkedWorksService linkedWorksService;
  @Inject private MediaStreamService mediaStreamService;
  @Inject private DescriptorService descriptorService;

  Optional<Work> findFirst(URI location) {
    return linkedWorksService.find(location).stream().map(this::toWork).findFirst();
  }

  Work toWork(LinkedWork linkedWork) {
    return new Work(
      linkedWork.work().descriptor(),
      findParent(linkedWork).orElse(null),
      linkedWork.matchedResources().stream().map(mediaStreamService::toMediaStream).toList()
    );
  }

  private Optional<Parent> findParent(LinkedWork linkedWork) {

    /*
     * A LinkedWork is always a local resource, so its parent information is based on that.
     * It can however be a Movie, in which case we should find the collection.
     */

    Resource resource = linkedWork.matchedResources().get(0).resource();

    return resource.parentLocation()
      .flatMap(this::findDescriptor)
      .or(() -> Optional.of(linkedWork.work().descriptor())
          .filter(Movie.class::isInstance)
          .map(Movie.class::cast)
          .flatMap(Movie::getCollectionId)
          .flatMap(descriptorService::findCached)
      )
      .map(this::createParent);
  }

  private Optional<WorkDescriptor> findDescriptor(URI location) {
    return linkedWorksService.find(location).stream()
      .findFirst()
      .map(LinkedWork::work)
      .map(hs.mediasystem.db.services.domain.Work::descriptor);
  }

  private Parent createParent(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Parent(descriptor.getId(), details.getTitle(), details.getBackdrop());
  }
}
