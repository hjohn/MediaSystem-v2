package hs.mediasystem.runner;

import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;

public interface SceneManager {
  StackPane getRootPane();
  void setPlayerRoot(Object root);
  void disposePlayerRoot();
  ObjectProperty<Paint> fillProperty();

  int getScreenNumber();
  void setScreenNumber(int screenNumber);
}
