package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.datasource.domain.stream.Work;
import hs.mediasystem.db.core.LinkedWorksService;
import hs.mediasystem.db.core.MediaStreamService;
import hs.mediasystem.db.core.domain.LinkedWork;
import hs.mediasystem.db.core.domain.Resource;
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
class LocalWorkService {
  @Inject private LinkedWorksService linkedWorksService;
  @Inject private MediaStreamService mediaStreamService;

  @Inject
  LocalWorkService() {}

  Optional<Work> findFirst(URI location) {
    return linkedWorksService.find(location).stream().map(this::toWork).findFirst();
  }

  Work toWork(LinkedWork linkedWork) {
    return new Work(
      linkedWork.workDescriptor(),
      findParent(linkedWork).orElse(null),
      linkedWork.resources().stream().map(mediaStreamService::toMediaStream).toList()
    );
  }

  private Optional<Context> findParent(LinkedWork linkedWork) {
    Resource resource = linkedWork.resources().getFirst();

    return linkedWork.workDescriptor() instanceof Release release
      ? release.getContext().or(() -> resource.streamable().parentLocation().flatMap(this::findDescriptor).map(this::createParent))
      : Optional.empty();
  }

  private Optional<WorkDescriptor> findDescriptor(URI location) {
    return linkedWorksService.find(location).stream()
      .findFirst()
      .map(LinkedWork::workDescriptor);
  }

  private Context createParent(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Context(descriptor.getId(), details.getTitle(), details.getCover(), details.getBackdrop());
  }
}
