package hs.mediasystem.runner.util;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.stage.Screen;

public interface SceneManager {
  StackPane getRootPane();
  Scene getScene();
  void setPlayerRoot(Object root);
  void disposePlayerRoot();
  ObjectProperty<Paint> fillProperty();

  Screen getScreen();
  int getScreenNumber();
  void setScreenNumber(int screenNumber);
  void display();
}
