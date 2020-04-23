package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.FolderPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FoldersCollectionType implements CollectionType {
  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", Comparator.comparing((Work work) -> work.getType().toString()).reversed()
      .thenComparing(WorkBinder.BY_NAME)
      .thenComparing(WorkBinder.BY_SUBTITLE)
    ),
    new SortOrder<>("watched-date", WorkBinder.BY_LAST_WATCHED_DATE.reversed())
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("watched-recently", r -> r.getState().getLastConsumptionTime().filter(d -> d.isAfter(Instant.now().minus(365 * 2, ChronoUnit.DAYS))).isPresent()
    )
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("unwatched", r -> !r.getState().isConsumed().getValue())
  );

  @Inject private FolderPresentation.Factory factory;
  @Inject private WorksClient worksClient;

  @Override
  public String getId() {
    return MediaType.FOLDER.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      worksClient.findRootsByTag(tag),
      "Folders" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS),
      null
    );
  }
}
