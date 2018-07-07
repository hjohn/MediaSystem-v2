package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.Labels;
import hs.mediasystem.util.javafx.ScaledImageView;

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

  public Node create(MediaItem<?> mediaItem) {
    Object data = mediaItem.getData();

    if(data instanceof Episode) {
      return create((Episode)data);
    }
    if(mediaItem.getProduction() != null) {
      return create(mediaItem.getProduction());
    }

    return null;   // TODO for persons we could do a preview..
  }

  public Node create(Serie serie, int seasonNumber) {
    BasePanel panel = new BasePanel();

    Production production = serie.findSeason(seasonNumber).getProduction();

    panel.title.set(serie.getProduction().getName());
    panel.season.set(seasonNumber == 0 ? "Specials" : "" + seasonNumber);
    panel.overview.set(production.getDescription());
    panel.imageURI.set(production.getImage());
    setReception(panel.rating, production.getReception());

    return panel;
  }

  public Node create(Episode episode) {
    BasePanel panel = new BasePanel();

    Production production = episode.getProduction();

    panel.episodeNumber.set("" + episode.getNumber());
    panel.title.set(production.getName());
    panel.releaseDate.set(MediaItemFormatter.formattedLocalDate(production.getDate()));
    panel.overview.set(production.getDescription());
    panel.imageURI.set(production.getImage());
    setReception(panel.rating, production.getReception());

    return panel;
  }

  public Node create(Production production) {
    BasePanel panel = new BasePanel();

    panel.title.set(production.getName());
    panel.releaseDate.set(MediaItemFormatter.formattedLocalDate(production.getDate()));
    panel.overview.set(production.getDescription());
    panel.imageURI.set(production.getImage());
    setReception(panel.rating, production.getReception());

    return panel;
  }

  public Node create(Person person) {
    BasePanel panel = new BasePanel();

    panel.title.set(person.getName());
    panel.imageURI.set(person.getImage());

    return panel;
  }

  private static void setReception(StringProperty rating, Reception reception) {
    if(reception != null && reception.getVoteCount() > 0) {
      rating.set(String.format("%.1f (%d votes)", reception.getRating(), reception.getVoteCount()));
    }
  }

  private class BasePanel extends VBox {
    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty releaseDate = new SimpleStringProperty();
    public final StringProperty season = new SimpleStringProperty();
    public final StringProperty episodeNumber = new SimpleStringProperty();
    public final StringProperty rating = new SimpleStringProperty();
    public final StringProperty overview = new SimpleStringProperty();
    public final ObjectProperty<ImageURI> imageURI = new SimpleObjectProperty<>();

    {
      AsyncImageProperty poster = new AsyncImageProperty();

      poster.imageHandleProperty().bind(Binds.monadic(imageURI).map(imageHandleFactory::fromURI));

      ScaledImageView imageView = new ScaledImageView() {{
        getStyleClass().add("poster-image");
        imageProperty().bind(poster);
        setPreserveRatio(true);
        setSmooth(true);
        setAlignment(Pos.TOP_CENTER);
      }};

      getChildren().addAll(
        Labels.create("title", title),
        Labels.create("SEASON", "header", season.isEmpty().not()),
        Labels.create("season", season, season.isEmpty().not()),
        Labels.create("EPISODE NUMBER", "header", episodeNumber.isEmpty().not()),
        Labels.create("episode-number", episodeNumber, episodeNumber.isEmpty().not()),
        Labels.create("RELEASE DATE", "header", releaseDate.isEmpty().not()),
        Labels.create("release-date", releaseDate, releaseDate.isEmpty().not()),
        Labels.create("RATING", "header", rating.isEmpty().not()),
        Labels.create("rating", rating, rating.isEmpty().not()),
        Labels.create("OVERVIEW", "header", overview.isEmpty().not()),
        Labels.create("overview", overview, overview.isEmpty().not()),
        imageView
      );

      VBox.setVgrow(imageView, Priority.ALWAYS);
    }
  }
}
