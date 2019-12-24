package hs.mediasystem.plugin.home;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.Labels.Feature;
import hs.mediasystem.util.javafx.control.carousel.CarouselListCell;
import hs.mediasystem.util.javafx.control.status.StatusIndicator;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
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
      private final Label title = Labels.create("title");
      private final Label subtitle = Labels.create("subtitle", Feature.HIDE_IF_EMPTY);
      private final HBox overlay = Containers.hbox("overlay", indicator, Containers.vbox("title-box", title, subtitle));

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

          title.setText(item.getTitle());
          subtitle.setText(item.getSubtitle());

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