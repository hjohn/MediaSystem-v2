package hs.mediasystem.runner.util;

import hs.mediasystem.runner.util.FXSceneManager.SceneLayout;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;

public interface SceneManager {
  StackPane getRootPane();
  Scene getScene();
  void setSceneLayout(SceneLayout sceneLayout);
  void setPlayerRoot(Object root);
  void disposePlayerRoot();

  Screen getScreen();
  int getScreenNumber();
  void setScreenNumber(int screenNumber);
  void display();
}
