package hs.mediasystem.plugin.home;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.carousel.CarouselListCell;
import hs.mediasystem.util.javafx.control.csslayout.StylableContainers;
import hs.mediasystem.util.javafx.control.csslayout.StylableHBox;
import hs.mediasystem.util.javafx.control.status.StatusIndicator;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MenuOptionCellFactory implements Callback<ListView<MenuOption>, ListCell<MenuOption>> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public CarouselListCell<MenuOption> call(ListView<MenuOption> view) {
    return new CarouselListCell<>() {
      private final BiasedImageView imageView = new BiasedImageView();
      private final AsyncImageProperty2 asyncImageProperty = new AsyncImageProperty2(600, 400);

      private final StatusIndicator indicator = new StatusIndicator();
      private final Label parentTitle = Labels.create("parent-title", Labels.HIDE_IF_EMPTY);
      private final Label title = Labels.create("title");
      private final Label subtitle = Labels.create("subtitle", Labels.HIDE_IF_EMPTY);
      private final Label sequence = Labels.create("sequence", Labels.HIDE_IF_EMPTY);
      private final Label age = Labels.create("age", Labels.HIDE_IF_EMPTY);

      private final StylableHBox overlay = StylableContainers.hbox("overlay", indicator, parentTitle, title, subtitle, sequence, age);

      private Timeline activeTimeline;

      {
        imageView.setOrientation(Orientation.HORIZONTAL);
        imageView.imageProperty().bind(asyncImageProperty);
        imageView.getOverlayRegion().getChildren().add(overlay);
        imageView.setPrefWidth(600);
        imageView.setMinRatio(4.0 / 3.0);
        imageView.setAlignment(Pos.CENTER);

        indicator.showPercentage.setValue(false);

        StackPane.setAlignment(overlay, Pos.BOTTOM_CENTER);

        overlay.setMaxHeight(Region.USE_PREF_SIZE);

        focusedProperty().addListener((obs, old, current) -> {
          if(activeTimeline != null) {
            activeTimeline.stop();
          }

          activeTimeline = current
            ? new Timeline(new KeyFrame(Duration.seconds(0.5), new KeyValue(zoomProperty(), 1.5)))
            : new Timeline(new KeyFrame(Duration.seconds(0.5), new KeyValue(zoomProperty(), 1.0)));

          activeTimeline.play();
        });
      }

      @Override
      protected void updateItem(MenuOption item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
          ImageHandle imageHandle = item.getImage().map(imageHandleFactory::fromURI).orElse(null);

          asyncImageProperty.imageHandleProperty().set(imageHandle);
          setGraphic(imageView);

          parentTitle.setText(item.getParentTitle());
          title.setText(item.getTitle());
          subtitle.setText(item.getSubtitle());
          sequence.setText(item.getSequence());
          age.setText(item.getRecommendationLastTimeWatched()
            .map(i -> i.atZone(ZoneId.systemDefault()))
            .map(ZonedDateTime::toLocalDateTime)
            .map(SizeFormatter::formatTimeAgo)
            .orElse(null)
          );

          if(item.getWatchedFraction() > 0) {
            indicator.value.setValue(item.getWatchedFraction());
          }
          else {
            indicator.value.setValue(-1.0);
          }
        }
      }
    };
  }
}