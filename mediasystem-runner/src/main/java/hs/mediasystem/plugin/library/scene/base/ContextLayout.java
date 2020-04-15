package hs.mediasystem.plugin.library.scene.base;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.plugin.library.scene.MediaItemFormatter;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.domain.Classification;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Person;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Serie;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.ScaledImageView;

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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
public class ContextLayout {
  private static final MediaType COLLECTION = MediaType.of("COLLECTION");

  @Inject private ImageHandleFactory imageHandleFactory;

  public BasePanel create(Object item) {
    if(item instanceof Work) {
      return create((Work)item);
    }
    if(item instanceof Contribution) {
      return create(((Contribution)item).getPerson());
    }
    if(item instanceof Participation) {
      return create(((Participation)item).getWork());
    }
    if(item instanceof WorksGroup) {
      return create(((WorksGroup)item));
    }

    return null;
  }

  public BasePanel create(Work work) {
    BasePanel panel = create(work.getDetails());

    work.getParent().map(Parent::getName).ifPresent(panel.groupTitle::set);
    panel.subtitle.set(work.getDetails().getClassification().getGenres().stream().collect(Collectors.joining(" / ")));
    work.getDetails().getReception().ifPresent(reception -> setReception(panel.rating, reception));

    // Add season and episode number:
    Optional<Sequence> seq = work.getDetails().getSequence().filter(s -> s.getType().equals(Type.EPISODE));

    seq.map(Sequence::getNumber).map(Object::toString).ifPresent(panel.episodeNumber::set);
    seq.flatMap(Sequence::getSeasonNumber).map(Object::toString).ifPresent(panel.season::set);

    // Add total seasons and episodes:
    work.getDetails().getSerie().map(Serie::getTotalEpisodes).ifPresent(episodeCount -> {
      panel.totalEpisodes.set("" + episodeCount + work.getDetails().getSerie().flatMap(Serie::getTotalSeasons).map(seasonCount -> " (" + seasonCount + " seasons)").orElse(""));
    });

    return panel;
  }

  private BasePanel create(WorksGroup wg) {
    BasePanel panel = create(wg.getDetails());
    LocalDate now = LocalDate.now();

    if(wg.getId().getType().equals(COLLECTION)) {
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
        .map(Classification::getGenres)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

      panel.subtitle.set(genreCounts.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getValue, Collections.reverseOrder()))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" / "))
      );
    }

    return panel;
  }

  private BasePanel create(Details details) {
    BasePanel panel = new BasePanel();

    panel.title.set(details.getTitle());
    panel.releaseDate.set(MediaItemFormatter.formattedLocalDate(details.getReleaseDate().orElse(null)));
    panel.overview.set(details.getDescription().orElse(null));
    panel.imageURI.set(details.getImage().orElse(null));

    return panel;
  }

  public BasePanel create(Person person) {
    BasePanel panel = new BasePanel();

    panel.title.set(person.getName());

    person.getImage().ifPresent(panel.imageURI::set);
    person.getBiography().ifPresent(panel.biography::set);
    person.getBirthPlace().ifPresent(panel.birthPlace::set);
    person.getBirthDate().ifPresent(bd -> panel.birthDate.set(MediaItemFormatter.formattedLocalDate(bd) + person.getDeathDate().map(MediaItemFormatter::formattedLocalDate).map(x -> " - " + x).orElse("")));

    return panel;
  }

  private static void setReception(StringProperty rating, Reception reception) {
    if(reception != null && reception.getVoteCount() > 0) {
      rating.set(String.format("%.1f (%d votes)", reception.getRating(), reception.getVoteCount()));
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
    public final ObjectProperty<ImageURI> imageURI = new SimpleObjectProperty<>();

    {
      AsyncImageProperty poster = new AsyncImageProperty(400, 600);

      getStyleClass().add("side-panel");

      poster.imageHandleProperty().bind(Val.wrap(imageURI).map(imageHandleFactory::fromURI));

      ScaledImageView imageView = new ScaledImageView() {{
        getStyleClass().add("poster-image");
        imageProperty().bind(poster);
        setPreserveRatio(true);
        setSmooth(true);
        setAlignment(Pos.TOP_CENTER);
      }};

      getChildren().addAll(
        Labels.create("groupTitle", groupTitle, Labels.HIDE_IF_EMPTY),
        Labels.create("title", title),
        Labels.create("subtitle", subtitle, Labels.HIDE_IF_EMPTY),
        Containers.hbox(
          "hbox",
          Containers.vbox(
            season.isEmpty().not(),
            Labels.create("header", "SEASON"),
            Labels.create("season", season)
          ),
          Containers.vbox(
            episodeNumber.isEmpty().not(),
            Labels.create("header", "EPISODE NUMBER"),
            Labels.create("episode-number", episodeNumber)
          )
        ),
        Containers.vbox(
          birthPlace.isEmpty().not(),
          Labels.create("header", "BIRTH PLACE"),
          Labels.create("birth-place", birthPlace)
        ),
        Containers.vbox(
          birthDate.isEmpty().not(),
          Labels.create("header", "BIRTH DATE"),
          Labels.create("birth-date", birthDate)
        ),
        Containers.hbox(
          "hbox",
          Containers.vbox(
            totalEntries.isEmpty().not(),
            Labels.create("header", "TOTAL"),
            Labels.create("total-entries", totalEntries)
          ),
          Containers.vbox(
            releaseDate.isEmpty().not(),
            Labels.create("header", "RELEASE DATE"),
            Labels.create("release-date", releaseDate)
          ),
          Containers.vbox(
            rating.isEmpty().not(),
            Labels.create("header", "RATING"),
            Labels.create("rating", rating)
          )
        ),
        Containers.hbox(
          "hbox",
          Containers.vbox(
            totalEpisodes.isEmpty().not(),
            Labels.create("header", "EPISODES"),
            Labels.create("total-episodes", totalEpisodes)
          )
        ),
        vgrow(Priority.ALWAYS, Containers.vbox(
          overview.isEmpty().not(),
          Labels.create("header", "OVERVIEW"),
          vgrow(Priority.ALWAYS, new AutoVerticalScrollPane(Labels.create("overview", overview), 8000, 40) {{
            setMinSize(1, 1);
            setPrefSize(10, 10);
          }})
        )),
        vgrow(Priority.ALWAYS, Containers.vbox(
          biography.isEmpty().not(),
          Labels.create("header", "BIOGRAPHY"),
          vgrow(Priority.ALWAYS, new AutoVerticalScrollPane(Labels.create("overview", biography), 8000, 40) {{
            setMinSize(1, 1);
            setPrefSize(10, 10);
          }})
        )),
        imageView
      );

      VBox.setVgrow(imageView, Priority.ALWAYS);
    }
  }

  private static Node vgrow(Priority priority, Node node) {
    VBox.setVgrow(node, priority);
    return node;
  }
}
