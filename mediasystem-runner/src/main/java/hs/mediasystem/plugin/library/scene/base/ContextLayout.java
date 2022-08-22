package hs.mediasystem.plugin.library.scene.base;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.plugin.library.scene.MediaItemFormatter;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.domain.Classification;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Person;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Serie;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.inject.Singleton;

@Singleton
public class ContextLayout {

  public BasePanel createGeneric(Object item) {
    if(item instanceof Work w) {
      return create(w);
    }
    if(item instanceof WorksGroup wg) {
      return create(wg);
    }

    return null;
  }

  public BasePanel create(Work work) {
    BasePanel panel = create(work.getDetails());

    if(work.getType().isComponent()) {
      work.getParent().map(Parent::title).ifPresent(panel.groupTitle::set);
    }

    panel.subtitle.set(work.getDetails().getClassification().genres().stream().collect(Collectors.joining(" / ")));
    work.getDetails().getReception().ifPresent(reception -> setReception(panel.rating, reception));

    // Add season and episode number:
    Optional<Sequence> seq = work.getDetails().getSequence().filter(s -> s.type().equals(Type.EPISODE));

    seq.map(Sequence::number).map(Object::toString).ifPresent(panel.episodeNumber::set);
    seq.flatMap(Sequence::seasonNumber).map(Object::toString).ifPresent(panel.season::set);

    // Add total seasons and episodes:
    work.getDetails().getSerie().flatMap(Serie::totalEpisodes).ifPresent(episodeCount -> {
      panel.totalEpisodes.set("" + episodeCount + work.getDetails().getSerie().flatMap(Serie::totalSeasons).map(seasonCount -> " (" + seasonCount + " seasons)").orElse(""));
    });

    return panel;
  }

  public BasePanel create(WorksGroup wg) {
    BasePanel panel = create(wg.getDetails());
    LocalDate now = LocalDate.now();

    if(wg.getId().getType().equals(MediaType.COLLECTION)) {
      wg.getChildren().stream().map(Work::getDetails).map(Details::getReleaseDate).flatMap(Optional::stream).filter(d -> d.isBefore(now)).min(Comparator.naturalOrder()).ifPresent(earliest -> {
        wg.getChildren().stream().map(Work::getDetails).map(Details::getReleaseDate).flatMap(Optional::stream).filter(d -> d.isBefore(now)).max(Comparator.naturalOrder()).ifPresent(latest -> {
          int minYear = earliest.getYear();
          int maxYear = latest.getYear();

          panel.releaseDate.setValue(minYear == maxYear ? "" + minYear : minYear + " - " + maxYear);
        });
      });

      Map<String, Long> genreCounts = wg.getChildren().stream()
        .map(Work::getDetails)
        .map(Details::getClassification)
        .map(Classification::genres)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

      panel.subtitle.set(genreCounts.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getValue, Collections.reverseOrder()))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" / "))
      );

      panel.totalEntries.set("" + wg.getChildren().size());
    }

    return panel;
  }

  public BasePanel create(Person person) {
    BasePanel panel = new BasePanel();

    panel.title.set(person.getName());

    person.getCover().ifPresent(panel.imageHandle::set);
    person.getBiography().ifPresent(panel.biography::set);
    person.getBirthPlace().ifPresent(panel.birthPlace::set);
    person.getBirthDate().ifPresent(bd -> panel.birthDate.set(MediaItemFormatter.formattedLocalDate(bd) + person.getDeathDate().map(MediaItemFormatter::formattedLocalDate).map(x -> " - " + x).orElse("")));

    return panel;
  }

  private BasePanel create(Details details) {
    BasePanel panel = new BasePanel();

    panel.title.set(details.getTitle());
    panel.releaseDate.set(MediaItemFormatter.formattedLocalDate(details.getReleaseDate().orElse(null)));
    panel.overview.set(details.getDescription().orElse(null));
    panel.imageHandle.set(details.getCover().or(details::getSampleImage).orElse(null));

    return panel;
  }

  private static void setReception(StringProperty rating, Reception reception) {
    if(reception != null && reception.voteCount() > 0) {
      rating.set(String.format("%.1f (%d votes)", reception.rating(), reception.voteCount()));
    }
  }

  private class BasePanel extends VBox {
    public final StringProperty groupTitle = new SimpleStringProperty();
    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty subtitle = new SimpleStringProperty();
    public final StringProperty releaseDate = new SimpleStringProperty();
    public final StringProperty birthPlace = new SimpleStringProperty();
    public final StringProperty birthDate = new SimpleStringProperty();
    public final StringProperty biography = new SimpleStringProperty();
    public final StringProperty season = new SimpleStringProperty();
    public final StringProperty episodeNumber = new SimpleStringProperty();
    public final StringProperty totalEntries = new SimpleStringProperty();
    public final StringProperty totalEpisodes = new SimpleStringProperty();
    public final StringProperty rating = new SimpleStringProperty();
    public final StringProperty overview = new SimpleStringProperty();
    public final ObjectProperty<ImageHandle> imageHandle = new SimpleObjectProperty<>();

    {
      AsyncImageProperty poster = new AsyncImageProperty(400, 600);

      getStyleClass().add("side-panel");

      poster.imageHandleProperty().bind(imageHandle);

      BiasedImageView imageView = new BiasedImageView() {{
        setMinRatio(0.9);
        getStyleClass().add("poster-image");
        imageProperty().bind(poster);
        setPreserveRatio(true);
        setSmooth(true);
        setAlignment(Pos.TOP_CENTER);
      }};

      imageView.managedProperty().bind(imageHandle.isNotNull());
      imageView.visibleProperty().bind(imageHandle.isNotNull());

      getChildren().addAll(
        Containers.vbox().nodes(
          Labels.create("groupTitle", groupTitle, Labels.HIDE_IF_EMPTY),
          Labels.create("title", title),
          Labels.create("subtitle", subtitle, Labels.HIDE_IF_EMPTY)
        ),
        Containers.hbox().style("hbox").ignoreWhenEmpty().nodes(
          Containers.vbox().ignoreWhen(season.isEmpty()).nodes(
            Labels.create("header", "SEASON"),
            Labels.create("season", season)
          ),
          Containers.vbox().ignoreWhen(episodeNumber.isEmpty()).nodes(
            Labels.create("header", "EPISODE"),
            Labels.create("episode-number", episodeNumber)
          )
        ),
        Containers.vbox().ignoreWhen(birthPlace.isEmpty()).nodes(
          Labels.create("header", "BIRTH PLACE"),
          Labels.create("birth-place", birthPlace)
        ),
        Containers.vbox().ignoreWhen(birthDate.isEmpty()).nodes(
          Labels.create("header", "BIRTH DATE"),
          Labels.create("birth-date", birthDate)
        ),
        Containers.hbox().style("hbox").ignoreWhenEmpty().nodes(
          Containers.vbox().ignoreWhen(releaseDate.isEmpty()).nodes(
            Labels.create("header", "RELEASE DATE"),
            Labels.create("release-date", releaseDate)
          ),
          Containers.vbox().ignoreWhen(totalEntries.isEmpty()).nodes(
            Labels.create("header", "TOTAL"),
            Labels.create("total-entries", totalEntries)
          ),
          Containers.vbox().ignoreWhen(rating.isEmpty()).nodes(
            Labels.create("header", "RATING"),
            Labels.create("rating", rating)
          )
        ),
        Containers.hbox().style("hbox").ignoreWhen(totalEpisodes.isEmpty()).nodes(
          Containers.vbox(
            Labels.create("header", "EPISODES"),
            Labels.create("total-episodes", totalEpisodes)
          )
        ),
        vgrow(Priority.ALWAYS, Containers.vbox().ignoreWhen(overview.isEmpty()).nodes(
          Labels.create("header", "OVERVIEW"),
          vgrow(Priority.ALWAYS, new AutoVerticalScrollPane(Labels.create("overview", overview), 8000, 40) {{
            setMinSize(1, 1);
            setPrefSize(10, 10);
          }})
        )),
        vgrow(Priority.ALWAYS, Containers.vbox().ignoreWhen(biography.isEmpty()).nodes(
          Labels.create("header", "BIOGRAPHY"),
          vgrow(Priority.ALWAYS, new AutoVerticalScrollPane(Labels.create("overview", biography), 8000, 40) {{
            setMinSize(1, 1);
            setPrefSize(10, 10);
          }})
        )),
        imageView
      );
    }
  }

  private static Node vgrow(Priority priority, Node node) {
    VBox.setVgrow(node, priority);
    return node;
  }
}
