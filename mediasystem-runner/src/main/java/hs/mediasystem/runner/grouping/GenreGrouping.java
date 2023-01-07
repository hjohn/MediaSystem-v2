package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.ui.api.domain.Classification;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ImageHandleFactory;
import hs.mediasystem.util.image.ImageURI;

import java.time.LocalDate;
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
public class GenreGrouping implements Grouping<Work, Object> {
  private static final ResourceManager RM = new ResourceManager(GenreGrouping.class);
  private static final DataSource DATA_SOURCE = DataSource.instance("GENRE");
  private static final Comparator<Work> WEIGHTED_RATING_COMPARATOR = Comparator.comparing(GenreGrouping::score).reversed();

  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public List<Object> group(List<? extends Work> items) {
    Map<String, List<Work>> map = new HashMap<>();

    for(Work item : items) {
      for(String genre : item.getDetails().getClassification().genres()) {
        map.computeIfAbsent(genre, k -> new ArrayList<>()).add(item);
      }
    }

    List<Object> topLevelItems = new ArrayList<>();

    for(Map.Entry<String, List<Work>> entry : map.entrySet()) {
      Comparator<Work> majorGenreComparator = Comparator.comparing((Work r) -> {
        int index = r.getDetails().getClassification().genres().indexOf(entry.getKey());

        return index == -1 ? Integer.MAX_VALUE : index;
      });

      /*
       * This pics 4 covers that best represent this genre.  It does this by first
       * picking works which have this genre as their major genre (their first genre)
       * and then by favoring the highest rated and most recent ones (weighted).
       */

      AtomicReference<ImageHandle> backgroundImageHandleRef = new AtomicReference<>();
      String uris = entry.getValue().stream()
        .sorted(majorGenreComparator.thenComparing(WEIGHTED_RATING_COMPARATOR))
        .peek(mi -> {  // This is dirty
          if(backgroundImageHandleRef.get() == null) {
            mi.getDetails().getBackdrop().ifPresent(backgroundImageHandleRef::set);
          }
        })
        .map(Work::getDetails)
        .map(Details::getCover)
        .flatMap(Optional::stream)
        .filter(Objects::nonNull)
        .map(ImageHandle::getKey)
        .limit(4)
        .collect(Collectors.joining("|"));

      Details details = new Details(
        entry.getKey(),
        null,
        RM.getText(entry.getKey().toLowerCase(), "description"),
        null,
        uris.isEmpty() ? null : imageHandleFactory.fromURI(new ImageURI("multi::" + uris, null)),  // as cover
        null,
        null,
        backgroundImageHandleRef.get(),
        null,
        null,
        null,
        null,
        null,
        Classification.DEFAULT
      );

      List<Work> children = entry.getValue();

      WorksGroup parent = new WorksGroup(
        new WorkId(DATA_SOURCE, MediaType.FOLDER, entry.getKey()),
        details,
        children,
        false
      );

      topLevelItems.add(parent);
    }

    return topLevelItems;
  }

  private static double score(Work work) {
    Reception reception = work.getDetails().getReception().orElse(null);
    LocalDate date = work.getDetails().getReleaseDate().orElse(null);

    if(reception != null && date != null && !Boolean.TRUE.equals(work.getDetails().getClassification().pornographic())) {
      return reception.rating() + date.getYear() * 0.05;
    }

    return Double.NEGATIVE_INFINITY;
  }
}
