package hs.mediasystem.plugin.library.scene.overview;

import com.sun.javafx.scene.control.skin.Utils;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.text.Font;

/**
 * Hacks button skin to take content bias of graphic into
 * account.
 */
public class NavigationDialogButtonSkin extends ButtonSkin {

  public NavigationDialogButtonSkin(Button control) {
    super(control);
  }

  @Override
  protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
    Node graphic = getSkinnable().getGraphic();

    // Get the preferred width of the text
    final Labeled labeled = getSkinnable();
    final Font font = getSkinnable().getFont();
    String string = labeled.getText();
    double widthPadding = leftInset + rightInset;

    if(!isIgnoreText()) {
      widthPadding += leftLabelPadding() + rightLabelPadding();
    }

    double textWidth = 0.0;
    if(string != null && !string.isEmpty()) {
      if(labeled.isMnemonicParsing()) {
        if(string.contains("_") && (string.indexOf("_") != string.length() - 1)) {
          string = string.replaceFirst("_", "");
        }
      }
      textWidth = Utils.computeTextWidth(font, string, 0);
    }

    /*
     * Here this skin differs from the standard skin; it will respect the content bias of the graphic
     * when calling the width functions by first calculating the height if bias is VERTICAL. In the
     * JavaFX code the width functions are simply called with -1, regardless of content bias.
     */

    double innerHeight = height;

    if(graphic != null && graphic.getContentBias() == Orientation.VERTICAL) {
      innerHeight = computePrefHeight(-1, topInset, rightInset, bottomInset, leftInset) - topInset - bottomInset;
    }

    double graphicWidth = graphic == null ? 0.0 : Utils.boundedSize(graphic.prefWidth(innerHeight), graphic.minWidth(innerHeight), graphic.maxWidth(innerHeight));

    // Now add on the graphic, gap, and padding as appropriate
    if(isIgnoreGraphic()) {
      return textWidth + widthPadding;
    }
    else if(isIgnoreText()) {
      return graphicWidth + widthPadding;
    }
    else if(labeled.getContentDisplay() == ContentDisplay.LEFT || labeled.getContentDisplay() == ContentDisplay.RIGHT) {
      return textWidth + labeled.getGraphicTextGap() + graphicWidth + widthPadding;
    }
    else {
      return Math.max(textWidth, graphicWidth) + widthPadding;
    }
  }

  private boolean isIgnoreGraphic() {
    Node graphic = getSkinnable().getGraphic();

    return (graphic == null ||
      !graphic.isManaged() ||
      getSkinnable().getContentDisplay() == ContentDisplay.TEXT_ONLY);
  }

  private boolean isIgnoreText() {
    final Labeled labeled = getSkinnable();
    final String txt = labeled.getText();
    return (txt == null ||
      txt.equals("") ||
      labeled.getContentDisplay() == ContentDisplay.GRAPHIC_ONLY);
  }

  private double leftLabelPadding() {
    return snapSizeX(getSkinnable().getLabelPadding().getLeft());
  }

  private double rightLabelPadding() {
    return snapSizeX(getSkinnable().getLabelPadding().getRight());
  }
}
