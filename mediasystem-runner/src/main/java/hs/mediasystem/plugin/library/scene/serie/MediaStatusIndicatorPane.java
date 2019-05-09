package hs.mediasystem.plugin.library.scene.serie;

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

public class MediaStatusIndicatorPane extends StackPane {
  public final Var<Double> value = Var.newSimpleVar(0.0);
  public final Var<Double> missingFraction = Var.newSimpleVar(0.0);

  private final Label label = Labels.create("", "indicator-percentage");
  private final Label backgroundLabel = Labels.create("", "indicator-pane-background");
  private final Label checkmarkLabel = Labels.create("", "indicator-checkmark");
  private final Label crossLabel = Labels.create("", "indicator-cross");

  private final Arc circle = new Arc(0, 0, 22, 22, 90, 0);
  private final Arc arc = new Arc(0, 0, 22, 22, 90, 0);
  private final Arc missingArc = new Arc(0, 0, 22, 22, 90, 0);
  private final Label arcGraphic = new Label();

  public MediaStatusIndicatorPane() {
    circle.getStyleClass().add("indicator-circle-shape");

    arc.setType(ArcType.OPEN);
    arc.getStyleClass().add("indicator-arc-shape");

    missingArc.setType(ArcType.OPEN);
    missingArc.getStyleClass().add("indicator-arc-shape-missing");

    Rectangle rectangle = new Rectangle(-25, -25, 50, 50);

    rectangle.setFill(Color.TRANSPARENT);

    arcGraphic.getStyleClass().add("indicator-arc");
    arcGraphic.setGraphic(new Group(rectangle, arc, circle, missingArc));
    arcGraphic.setAlignment(Pos.TOP_CENTER);
    arcGraphic.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    getChildren().addAll(backgroundLabel, checkmarkLabel, crossLabel, arcGraphic, label);

    value.addListener(this::update);
    missingFraction.addListener(this::update);
  }

  private void update(@SuppressWarnings("unused") Observable obs) {
    double percentage = value.getValue();
    double mf = missingFraction.getValue();

    if(percentage != 0) {
      backgroundLabel.setVisible(true);

      if(percentage == 1.0) {
        checkmarkLabel.setVisible(true);
        crossLabel.setVisible(false);
        arcGraphic.setVisible(false);
        label.setVisible(false);
      }
      else if(percentage < 0) {
        checkmarkLabel.setVisible(false);
        crossLabel.setVisible(true);
        arcGraphic.setVisible(false);
        label.setVisible(false);
      }
      else {
        circle.setLength(360 * (1 - percentage));
        arc.setLength(-360 * percentage);
        label.setText(String.format("%d%%", (int)(percentage * 100)));

        checkmarkLabel.setVisible(false);
        crossLabel.setVisible(false);
        arcGraphic.setVisible(true);
        label.setVisible(true);
      }
    }
    else {
      backgroundLabel.setVisible(false);
      checkmarkLabel.setVisible(false);
      crossLabel.setVisible(false);
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
