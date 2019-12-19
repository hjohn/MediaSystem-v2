package hs.mediasystem.util.javafx.control.status;

import hs.mediasystem.util.javafx.control.Labels;

import javafx.beans.Observable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;

import org.reactfx.value.Var;

public class StatusIndicator extends StackPane {
  public final Var<Double> value = Var.newSimpleVar(0.0);
  public final Var<Double> missingFraction = Var.newSimpleVar(0.0);
  public final Var<Boolean> showPercentage = Var.newSimpleVar(Boolean.TRUE);

  private final Label label = Labels.create("size,percentage");
  private final Label backgroundLabel = Labels.create("size,background");
  private final Label icon = new Label();

  private final Arc circle = new Arc(0, 0, 22, 22, 90, 0);
  private final Arc arc = new Arc(0, 0, 22, 22, 90, 0);
  private final Arc missingArc = new Arc(0, 0, 22, 22, 90, 0);
  private final Label arcGraphic = new Label();

  public StatusIndicator() {
    getStyleClass().setAll("status-indicator");
    getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

    circle.getStyleClass().add("circle-shape");

    arc.setType(ArcType.OPEN);
    arc.getStyleClass().add("arc-shape");

    missingArc.setType(ArcType.OPEN);
    missingArc.getStyleClass().add("arc-shape-missing");

    Rectangle rectangle = new Rectangle(-25, -25, 50, 50);

    rectangle.setFill(Color.TRANSPARENT);

    arcGraphic.getStyleClass().addAll("size", "arc");
    arcGraphic.setGraphic(new Group(rectangle, arc, circle, missingArc));
    arcGraphic.setAlignment(Pos.TOP_CENTER);
    arcGraphic.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    getChildren().addAll(backgroundLabel, icon, arcGraphic, label);

    value.addListener(this::update);
    missingFraction.addListener(this::update);

    update(null);
  }

  private void update(@SuppressWarnings("unused") Observable obs) {
    double percentage = value.getValue();
    double mf = missingFraction.getValue();

    if(percentage != 0) {
      backgroundLabel.setVisible(true);

      if(percentage == 1.0) {
        icon.setVisible(true);
        icon.getStyleClass().setAll("size", "icon", "full");
        arcGraphic.setVisible(false);
        label.setVisible(false);
      }
      else if(percentage < 0) {
        icon.setVisible(true);
        icon.getStyleClass().setAll("size", "icon", "empty");
        arcGraphic.setVisible(false);
        label.setVisible(false);
      }
      else {
        circle.setLength(360 * (1 - percentage));
        arc.setLength(-360 * percentage);
        arcGraphic.setVisible(true);

        if(showPercentage.getValue()) {
          icon.setVisible(false);
          label.setText(String.format("%d%%", (int)(percentage * 100)));
          label.setVisible(true);
        }
        else {
          icon.setVisible(true);
          icon.getStyleClass().setAll("size", "icon", "partial");
          label.setText("");
          label.setVisible(false);
        }
      }
    }
    else {
      backgroundLabel.setVisible(false);
      icon.setVisible(false);
      arcGraphic.setVisible(false);
      label.setVisible(false);
    }

    if(mf != 0) {
      missingArc.setVisible(true);
      missingArc.setLength(360 * mf);
    }
    else {
      missingArc.setVisible(false);
    }
  }
}
