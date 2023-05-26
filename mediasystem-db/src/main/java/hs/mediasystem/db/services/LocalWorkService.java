package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.datasource.domain.stream.Work;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.work.Context;

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

  private Optional<Context> findParent(LinkedWork linkedWork) {
    Resource resource = linkedWork.matchedResources().get(0).resource();

    return linkedWork.work().descriptor() instanceof Release release
      ? release.getContext().or(() -> resource.parentLocation().flatMap(this::findDescriptor).map(this::createParent))
      : Optional.empty();
  }

  private Optional<WorkDescriptor> findDescriptor(URI location) {
    return linkedWorksService.find(location).stream()
      .findFirst()
      .map(LinkedWork::work)
      .map(hs.mediasystem.db.services.domain.Work::descriptor);
  }

  private Context createParent(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Context(descriptor.getId(), details.getTitle(), details.getBackdrop());
  }
}
