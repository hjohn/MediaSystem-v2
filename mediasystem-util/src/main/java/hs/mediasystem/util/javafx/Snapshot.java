package hs.mediasystem.util.javafx;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

public class Snapshot extends StackPane {
  private WritableImage snapshot;
  private int teller = 0;
  public Snapshot(Node node) {
    super(node);
  }

  @Override
  protected void layoutChildren() {
    if(snapshot == null && teller++ > 5) {
      snapshot = snapshot(null, null);
      ImageView imageView = new ImageView(snapshot);

      imageView.setFitWidth(snapshot.getWidth());
      imageView.setFitHeight(snapshot.getHeight());

      getChildren().setAll(imageView);
    }

    super.layoutChildren();
  }
}
