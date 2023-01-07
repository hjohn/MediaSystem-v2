package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.plugin.library.scene.MediaItemFormatter;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.StarRating;
import hs.mediasystem.util.javafx.ui.status.StatusIndicator;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class EpisodePane extends HBox {
  public final Model model = new Model();

  public static class Model {
    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty description = new SimpleStringProperty();
    public final ObjectProperty<LocalDate> releaseDate = new SimpleObjectProperty<>();
    public final ObjectProperty<Reception> reception = new SimpleObjectProperty<>();
    public final ObjectProperty<Sequence> sequence = new SimpleObjectProperty<>();
    public final ObjectProperty<ImageHandle> sampleImage = new SimpleObjectProperty<>();
    public final DoubleProperty mediaStatus = new SimpleDoubleProperty();
  }

  private final StringProperty subtitle = new SimpleStringProperty();

  public EpisodePane() {
    AsyncImageProperty imageProperty = new AsyncImageProperty(840, 840);

    imageProperty.imageHandleProperty().bind(model.sampleImage);

    Label label = Labels.create("ph", "?");

    label.setMaxSize(1000, 1000);

    BiasedImageView poster = new BiasedImageView(label);

    poster.setOrientation(Orientation.VERTICAL);
    poster.imageProperty().bind(imageProperty);

    StackPane indicatorPane = createMediaStatusIndicatorPane(model.mediaStatus);

    poster.getOverlayRegion().getChildren().add(indicatorPane);

    model.sequence.addListener(this::updateSubtitle);
    model.releaseDate.addListener(this::updateSubtitle);

    Label titleLabel = Labels.create("title", model.title);
    Label subtitleLabel = Labels.create("subtitle", subtitle);

    titleLabel.setMinHeight(Region.USE_PREF_SIZE);  // With reflowed labels, sometimes not enough vertical space is assigned and the reflow fails to use the next line and adds an ellipsis instead...

    VBox titleBox = Containers.vbox(titleLabel, subtitleLabel);
    HBox.setHgrow(titleBox, Priority.ALWAYS);

    VBox vbox = Containers.vbox(
      Containers.hbox(
        titleBox,
        createStarRating(model.reception, 10, 4)
      ),
      new AutoVerticalScrollPane(Labels.create("description", model.description), 12000, 40)
    );

    getChildren().add(vbox);
    getChildren().add(poster);

    /*
     * The following four tweaks to the layout are needed to have the description box and poster play nice.
     *
     * The poster should take up as much space as possible, aspect ratio allowing, without "pushing" its parent
     * bigger (as that would resize the root of this control even).  The outer container that is returned therefore
     * gets its min and pref sizes forced to a lower arbirtrary values; min size to prevent the entire root to
     * become bigger; preferred size to prevent it stealing space from parent containers (like the primary left poster).
     *
     * Secondly, the description box gets its preferred width lowered, as Labels with long texts use a preferred
     * width that would fit their entire text instead.  Containers with such a Label are not smart enough to use
     * Label's content bias to find a more reasonable configuration by themselves.
     *
     * Finally, the description box is the only box allowed to take up left-over space.  Everything else will
     * get the exact space required, and no more.
     */

    HBox.setHgrow(vbox, Priority.ALWAYS);  // Description box can grow, poster however should just try and attain its preferred size (which can be huge).

    vbox.setPrefWidth(100);  // Limit pref width on the description box, so other content can be resized to their preferred sizes.

    setMinSize(100, 100);
    setPrefSize(100, 100);
  }

  private void updateSubtitle(@SuppressWarnings("unused") Observable observable) {
    String formattedDate = MediaItemFormatter.formattedLocalDate(model.releaseDate.get());
    String seasonEpisodeText = model.sequence.get() == null ? "" : createSeasonEpisodeText(model.sequence.get());

    subtitle.set(Stream.of(seasonEpisodeText, formattedDate).filter(Objects::nonNull).collect(Collectors.joining(" • ")));
  }

  private static StarRating createStarRating(ObjectProperty<Reception> reception, double radius, double innerRadius) {
    StarRating starRating = new StarRating(radius, innerRadius, 5);

    reception.addListener((obs, old, current) -> {
      starRating.setVisible(current != null);
      starRating.setManaged(current != null);
      starRating.setRating(current == null ? 0.0 : current.rating() / 10);
    });

    return starRating;
  }

  private static StatusIndicator createMediaStatusIndicatorPane(DoubleProperty percentage) {
    StatusIndicator indicator = new StatusIndicator();

    indicator.setAlignment(Pos.BOTTOM_RIGHT);
    indicator.getStyleClass().add("indicator-pane");
    indicator.value.bind(percentage);

    return indicator;
  }

  private static String createSeasonEpisodeText(Sequence sequence) {
    int seasonNumber = sequence.seasonNumber().orElse(-1);

    if(sequence.type() == Type.SPECIAL) {
      return "Special";
    }
    if(sequence.type() == Type.EXTRA || seasonNumber == -1) {
      return "Extra";
    }

    return "Season " + seasonNumber + ", Episode " + sequence.number();
  }
}
