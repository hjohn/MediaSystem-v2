package hs.mediasystem.plugin.library.scene.overview;

import hs.jfx.eventstream.core.Values;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.StarRating;
import hs.mediasystem.util.javafx.ui.status.StatusIndicator;
import hs.mediasystem.util.javafx.ui.transition.StandardTransitions;
import hs.mediasystem.util.javafx.ui.transition.TransitionPane;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProductionOverviewPane extends HBox {
  private static final String STYLES_URL = LessLoader.compile(ProductionOverviewPane.class, "styles.less");

  public final Model model = new Model();

  public static class Model {
    public final ObjectProperty<Work> work = new SimpleObjectProperty<>();
    public final DoubleProperty watchedFraction = new SimpleDoubleProperty();
    public final DoubleProperty missingFraction = new SimpleDoubleProperty();
    public final ObjectProperty<Supplier<Node>> dynamicPanel = new SimpleObjectProperty<>();
  }

  private final AsyncImageProperty imageProperty = new AsyncImageProperty(840, 840);
  private final BiasedImageView poster = new BiasedImageView(2.0 / 3);

  {
    poster.setOrientation(Orientation.VERTICAL);
    poster.imageProperty().bind(imageProperty);
  }

  private final Label titleLabel = Labels.create("title", model.work.map(Work::getDetails).map(Details::getTitle));

  {
    Values.of(titleLabel.textProperty()).subscribe(text -> {
      titleLabel.getStyleClass().remove("smaller");

      if(text != null && text.length() > 40) {
        titleLabel.getStyleClass().add("smaller");
      }
    });
  }

  private final VBox leftTitleBox = Containers.vbox(
    "left-title-box",
    titleLabel,
    Containers.hbox(
      "subtitle-box",
      Labels.create("release-year", model.work.map(Work::getDetails).map(Details::getYearRange)),
      Labels.create("content-rating", model.work.map(this::extractContentRating), Labels.HIDE_IF_EMPTY, Labels.REVERSE_CLIP_TEXT),
      Labels.create("adult-rating", model.work.map(w -> Boolean.TRUE.equals(w.getDetails().getClassification().pornographic()) ? "XXX" : ""), Labels.HIDE_IF_EMPTY, Labels.REVERSE_CLIP_TEXT)
    ),
    Labels.create("genres", model.work.map(w -> w.getDetails().getClassification().genres().stream().collect(Collectors.joining(" / "))))
  );

  {
    HBox.setHgrow(leftTitleBox, Priority.ALWAYS);
  }

  private final TransitionPane dynamicBoxContainer = new TransitionPane(StandardTransitions.fade());

  {
    VBox.setVgrow(dynamicBoxContainer, Priority.ALWAYS);

    Values.of(model.dynamicPanel).subscribe(panel -> {
      if(panel == null) {
        dynamicBoxContainer.clear();
      }
      else {
        dynamicBoxContainer.add(panel.get());
      }
    });
  }

  private final StatusIndicator indicator = new StatusIndicator();

  {
    indicator.setAlignment(Pos.BOTTOM_RIGHT);
    indicator.getStyleClass().add("indicator-pane");
    indicator.value.bind(model.watchedFraction);
    indicator.missingFraction.bind(model.missingFraction);
  }

  private final StarRating starRating = new StarRating(20, 8, 5);

  {
    Values.of(model.work).subscribe(work -> {
      double rating = Optional.ofNullable(work).map(Work::getDetails).flatMap(Details::getReception).map(Reception::rating).orElse(0.0);

      starRating.setRating(rating * 0.1);  // convert rating to fraction
      starRating.setVisible(rating > 0);
      starRating.setManaged(rating > 0);
    });
  }

  private final VBox descriptionBox = Containers.vbox(
    Containers.hbox(
      "title-panel",
      leftTitleBox,
      Containers.vbox(
        "right-title-box",
        starRating,
        indicator
      )
    ),
    dynamicBoxContainer
  );

  {
    HBox.setHgrow(descriptionBox, Priority.ALWAYS);
  }

  public ProductionOverviewPane() {
    imageProperty.imageHandleProperty().bind(model.work.map(w -> w.getDetails().getAnyCover().orElse(null)));

    getChildren().addAll(poster, descriptionBox);

    getStyleClass().add("main-panel");
    getStylesheets().add(STYLES_URL);
  }

  private String extractContentRating(Work work) {
    return work.getDetails().getClassification().contentRatings().get("US");
  }
}
