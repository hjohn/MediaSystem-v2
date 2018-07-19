package hs.mediasystem.plugin.library.scene.serie;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class DebugTooltip {

  public static void set(Control control) {
    Tooltip tooltip = new Tooltip();

    tooltip.setHideDelay(Duration.minutes(1));
    control.setTooltip(tooltip);

    tooltip.setOnShowing(e -> {
      VBox vbox = new VBox();

      tooltip.setGraphic(vbox);

      Region node = control;

      for(;;) {
        vbox.getChildren().add(new Label(
          String.format(
            "%s: (%sx%s) - (%sx%s) - (%sx%s)",
            node.getClass().getSimpleName(),
            formatDimension(node.getMinWidth()), formatDimension(node.getMinHeight()),
            formatDimension(node.getPrefWidth()), formatDimension(node.getPrefHeight()),
            formatDimension(node.getMaxWidth()), formatDimension(node.getMaxHeight())
          )
        ));

        node = (Region)node.getParent();

        if(node == null) {
          break;
        }
      }
    });
  }

  public static String formatDimension(double x) {
    if(x == Pane.USE_COMPUTED_SIZE) {
      return "c";
    }
    if(x == Pane.USE_PREF_SIZE) {
      return "p";
    }

    return x % 1 == 0 ? "" + (long)x : "" + x;
  }
}
