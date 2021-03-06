package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.ui.api.domain.Classification;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Singleton;

@Singleton
public class GenreGrouping implements Grouping<Work> {
  private static final ResourceManager RM = new ResourceManager(GenreGrouping.class);
  private static final DataSource DATA_SOURCE = DataSource.instance(MediaType.GROUPING, "GENRE");
  private static final Comparator<Work> RATING_COMPARATOR = Comparator.comparing((Work w) -> w.getDetails().getReception().map(Reception::getRating).orElse(0.0)).reversed();

  @Override
  public List<Object> group(List<Work> items) {
    Map<String, List<Work>> map = new HashMap<>();

    for(Work item : items) {
      for(String genre : item.getDetails().getClassification().getGenres()) {
        map.computeIfAbsent(genre, k -> new ArrayList<>()).add(item);
      }
    }

    List<Object> topLevelItems = new ArrayList<>();

    for(Map.Entry<String, List<Work>> entry : map.entrySet()) {
      Comparator<Work> majorGenreComparator = Comparator.comparing((Work r) -> {
        int index = r.getDetails().getClassification().getGenres().indexOf(entry.getKey());

        return index == -1 ? Integer.MAX_VALUE : index;
      });

      AtomicReference<ImageURI> backgroundURIRef = new AtomicReference<>();
      String uris = entry.getValue().stream()
        .sorted(majorGenreComparator.thenComparing(RATING_COMPARATOR))
        .peek(mi -> {  // This is dirty
          if(backgroundURIRef.get() == null) {
            mi.getDetails().getBackdrop().ifPresent(backgroundURIRef::set);
          }
        })
        .map(Work::getDetails)
        .map(Details::getImage)
        .flatMap(Optional::stream)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .limit(4)
        .collect(Collectors.joining("|"));

      Details details = new Details(
        entry.getKey(),
        null,
        RM.getText(entry.getKey().toLowerCase(), "description"),
        null,
        uris.isEmpty() ? null : new ImageURI("multi::" + uris, null),
        backgroundURIRef.get(),
        null,
        null,
        null,
        null,
        null,
        Classification.DEFAULT
      );

      List<Work> children = entry.getValue();

      WorksGroup parent = new WorksGroup(
        new WorkId(DATA_SOURCE, entry.getKey()),
        details,
        children
      );

      topLevelItems.add(parent);
    }

    return topLevelItems;
  }
}
