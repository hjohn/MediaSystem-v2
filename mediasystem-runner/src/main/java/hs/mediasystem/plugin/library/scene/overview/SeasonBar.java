package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.runner.util.grid.MediaStatus;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import org.reactfx.util.Interpolator;
import org.reactfx.value.Var;

public class SeasonBar extends HBox {
  public final ObjectProperty<List<Entry>> entries = new SimpleObjectProperty<>(Collections.emptyList());
  public final IntegerProperty activeIndex = new SimpleIntegerProperty();
  public final HBox content = Containers.hbox("items");
  public final ScrollPane scrollPane = new ScrollPane(content);

  private final Var<Double> hscroll = Var.newSimpleVar(0.0);

  private boolean hasSeasons = true;

  public SeasonBar() {
    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER);

    entries.addListener((obs, old, current) -> recreateChildren());

    activeIndex.addListener((ov, old, current) -> updateActiveEntry(entries.getValue().get(current.intValue())));

    content.sceneProperty().addListener(obs -> Platform.runLater(() -> {
      hscroll.setValue(calculateScrollPositionForActiveNode());
      scrollPane.hvalueProperty().bind(hscroll.animate(Duration.ofMillis(250), Interpolator.LINEAR_DOUBLE));
    }));

    content.setPadding(new Insets(0, 20, 0, 20));

    scrollPane.hvalueProperty().addListener(obs -> {
      scrollPane.setClip(createClip(scrollPane, 20, 1.0, 1.0));
    });
  }

  private static Rectangle createClip(Region region, double fadePadding, double topFadePercentage, double bottomFadePercentage) {
    Rectangle rectangle = new Rectangle(0, 0, region.getWidth(), region.getHeight());
    double offset = fadePadding / region.getWidth();

    rectangle.setFill(new LinearGradient(0, 0, region.getWidth(), 0, false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.TRANSPARENT),
      new Stop(offset * topFadePercentage, Color.BLACK),
      new Stop(1 - offset * bottomFadePercentage, Color.BLACK),
      new Stop(1.0, Color.TRANSPARENT)
    ));

    return rectangle;
  }

  private void updateActiveEntry(Entry activeEntry) {
    hscroll.setValue(calculateScrollPositionForActiveNode());

    for(Node stackPaneNode : content.getChildren()) {
      Node node = ((StackPane)stackPaneNode).getChildren().get(0);

      node.getStyleClass().remove("focused");
      node.setEffect(null);

      if(stackPaneNode.getUserData().equals(activeEntry)) {
        node.getStyleClass().add("focused");
        node.setEffect(new Bloom(0.2));
      }
    }

    if(hasSeasons) {
      Node node = getChildren().get(0);

      node.getStyleClass().remove("focused");
      node.setEffect(null);

      if(activeEntry.isSeason) {
        node.getStyleClass().add("focused");
        node.setEffect(new Bloom(0.2));
      }
    }
  }

  private double calculateScrollPositionForActiveNode() {
    Entry activeEntry = entries.getValue().get(activeIndex.getValue());
    int totalChildren = content.getChildren().size();

    if(totalChildren > 0) {
      Node activeChildNode = content.getChildren().get(0);

      for(int i = 0; i < totalChildren; i++) {
        if(activeEntry.equals(content.getChildren().get(i).getUserData())) {
          activeChildNode = content.getChildren().get(i);
          break;
        }
      }

      return calculateScrollPosition(scrollPane, activeChildNode);
    }

    return 0;
  }

  private static double calculateScrollPosition(ScrollPane scrollPane, Node node) {
    double w = scrollPane.getContent().getBoundsInLocal().getWidth();
    double x = (node.getBoundsInParent().getMaxX() + node.getBoundsInParent().getMinX()) / 2;
    double v = scrollPane.getViewportBounds().getWidth();

    return scrollPane.getHmax() * ((x - 0.5 * v) / (w - v));
  }

  private void recreateChildren() {
    getChildren().clear();
    content.getChildren().clear();

    hasSeasons = !entries.getValue().isEmpty();

    for(Entry entry : entries.getValue()) {
      if(entry.isSeason && getChildren().isEmpty()) {
        Label seasonsLabel = new Label("Season");

        getChildren().add(seasonsLabel);

        seasonsLabel.setMinWidth(Region.USE_PREF_SIZE);
      }

      Label label = new Label(entry.title);
      Label graphic = Labels.create("graphic");
      Label graphicBackground = Labels.create("graphic-bg");

      StackPane stackPane = Containers.stack(label, graphicBackground, graphic);

      entry.mediaStatus.values(ms -> setStyle(stackPane, ms));

      StackPane.setAlignment(graphic, Pos.CENTER_RIGHT);
      StackPane.setAlignment(graphicBackground, Pos.CENTER_RIGHT);

      stackPane.setUserData(entry);

      content.getChildren().add(stackPane);
    }

    getChildren().add(scrollPane);

    updateActiveEntry(entries.getValue().get(activeIndex.getValue()));
  }

  private static void setStyle(Node node, MediaStatus current) {
    switch(current == null ? MediaStatus.UNAVAILABLE : current) {
    case WATCHED:
      node.getStyleClass().add("watched");
      node.getStyleClass().remove("available");
      node.getStyleClass().remove("unavailable");
      break;
    case AVAILABLE:
      node.getStyleClass().add("available");
      node.getStyleClass().remove("watched");
      node.getStyleClass().remove("unavailable");
      break;
    case UNAVAILABLE:
      node.getStyleClass().remove("available");
      node.getStyleClass().remove("watched");
      node.getStyleClass().add("unavailable");
      break;
    }
  }

  public static class Entry {
    public final ObjectProperty<MediaStatus> mediaStatus = new SimpleObjectProperty<>();

    private final String title;
    private final boolean isSeason;

    public Entry(String title, MediaStatus mediaStatus, boolean isSeason) {
      this.title = title;
      this.mediaStatus.setValue(mediaStatus);
      this.isSeason = isSeason;
    }
  }
}
