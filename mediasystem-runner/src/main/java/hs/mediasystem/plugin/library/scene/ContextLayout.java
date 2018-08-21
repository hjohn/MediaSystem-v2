package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.ScaledImageView;

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

@Singleton
public class ContextLayout {
  @Inject private ImageHandleFactory imageHandleFactory;

  public BasePanel create(MediaItem<?> mediaItem) {
    Object data = mediaItem.getData();

    if(data instanceof Episode) {
      return create((Episode)data, mediaItem.getParent().productionTitle.get());
    }
    if(mediaItem.getProduction() != null) {
      return create(mediaItem.getProduction());
    }
    if(mediaItem.getPerson() != null) {
      return create(mediaItem.getPerson());
    }

    return null;
  }

  public BasePanel create(Serie serie, int seasonNumber) {
    BasePanel panel = new BasePanel();

    Production production = serie.findSeason(seasonNumber);

    panel.title.set(serie.getName());
    panel.season.set(seasonNumber == 0 ? "Specials" : "" + seasonNumber);
    panel.overview.set(production.getDescription());
    panel.imageURI.set(production.getImage());
    setReception(panel.rating, production.getReception());

    return panel;
  }

  public BasePanel create(Episode episode, String groupTitle) {
    BasePanel panel = new BasePanel();

    panel.groupTitle.set(groupTitle);
    panel.season.set("" + episode.getSeasonNumber());
    panel.episodeNumber.set("" + episode.getNumber());
    panel.title.set(episode.getName());
    panel.releaseDate.set(MediaItemFormatter.formattedLocalDate(episode.getDate()));
    panel.overview.set(episode.getDescription());
    panel.imageURI.set(episode.getImage());
    setReception(panel.rating, episode.getReception());

    return panel;
  }

  public BasePanel create(Production production) {
    BasePanel panel = new BasePanel();

    panel.title.set(production.getName());
    panel.subtitle.set(production.getGenres().stream().collect(Collectors.joining(" / ")));
    panel.releaseDate.set(MediaItemFormatter.formattedLocalDate(production.getDate()));
    panel.overview.set(production.getDescription());
    panel.imageURI.set(production.getImage());
    setReception(panel.rating, production.getReception());

    return panel;
  }

  public BasePanel create(Person person) {
    BasePanel panel = new BasePanel();

    panel.title.set(person.getName());
    panel.imageURI.set(person.getImage());

    return panel;
  }

  public BasePanel create(PersonalProfile personalProfile) {
    BasePanel panel = create(personalProfile.getPerson());

    panel.birthPlace.set(personalProfile.getBirthPlace());
    panel.birthDate.set(MediaItemFormatter.formattedLocalDate(personalProfile.getBirthDate()) + (personalProfile.getDeathDate() == null ? "" : " - " + MediaItemFormatter.formattedLocalDate(personalProfile.getDeathDate())));
    panel.biography.set(personalProfile.getBiography());

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
    public final StringProperty rating = new SimpleStringProperty();
    public final StringProperty overview = new SimpleStringProperty();
    public final ObjectProperty<ImageURI> imageURI = new SimpleObjectProperty<>();

    {
      AsyncImageProperty poster = new AsyncImageProperty();

      getStyleClass().add("side-panel");

      poster.imageHandleProperty().bind(Binds.monadic(imageURI).map(imageHandleFactory::fromURI));

      ScaledImageView imageView = new ScaledImageView() {{
        getStyleClass().add("poster-image");
        imageProperty().bind(poster);
        setPreserveRatio(true);
        setSmooth(true);
        setAlignment(Pos.TOP_CENTER);
      }};

      getChildren().addAll(
        Labels.create("groupTitle", groupTitle, groupTitle.isEmpty().not()),
        Labels.create("title", title),
        Labels.create("subtitle", subtitle, subtitle.isEmpty().not()),
        Containers.hbox(
          "hbox",
          Containers.vbox(
            Labels.create("SEASON", "header", season.isEmpty().not()),
            Labels.create("season", season, season.isEmpty().not())
          ),
          Containers.vbox(
            Labels.create("EPISODE NUMBER", "header", episodeNumber.isEmpty().not()),
            Labels.create("episode-number", episodeNumber, episodeNumber.isEmpty().not())
          )
        ),
        Containers.vbox(
          Labels.create("BIRTH PLACE", "header", birthPlace.isEmpty().not()),
          Labels.create("birth-place", birthPlace, birthPlace.isEmpty().not())
        ),
        Containers.vbox(
          Labels.create("BIRTH DATE", "header", birthDate.isEmpty().not()),
          Labels.create("birth-date", birthDate, birthDate.isEmpty().not())
        ),
        Containers.hbox(
          "hbox",
          Containers.vbox(
            Labels.create("RELEASE DATE", "header", releaseDate.isEmpty().not()),
            Labels.create("release-date", releaseDate, releaseDate.isEmpty().not())
          ),
          Containers.vbox(
            Labels.create("RATING", "header", rating.isEmpty().not()),
            Labels.create("rating", rating, rating.isEmpty().not())
          )
        ),
        vgrow(Priority.ALWAYS, Containers.vbox(
          overview.isEmpty().not(),
          Labels.create("OVERVIEW", "header"),
          vgrow(Priority.ALWAYS, new AutoVerticalScrollPane(Labels.create("overview", overview), 8000, 8) {{
            setMinSize(1, 1);
            setPrefSize(10, 10);
          }})
        )),
        vgrow(Priority.ALWAYS, Containers.vbox(
          biography.isEmpty().not(),
          Labels.create("BIOGRAPHY", "header"),
          vgrow(Priority.ALWAYS, new AutoVerticalScrollPane(Labels.create("overview", biography), 8000, 8) {{
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
