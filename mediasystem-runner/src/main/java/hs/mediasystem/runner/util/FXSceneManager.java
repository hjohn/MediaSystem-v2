package hs.mediasystem.runner.util;

import hs.mediasystem.util.javafx.SceneUtil;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Manages one or more Stages depending on the {@link SceneLayout} chosen.<p>
 *
 * The layout {@link SceneLayout#ROOT} is suitable for incorporating video
 * directly as a JavaFX node, while the {@link SceneLayout#CHILD} is best
 * when video can only be incorporated by specifying a native window to
 * use.
 */
public class FXSceneManager implements SceneManager {
  private static final String STYLES_URL = LessLoader.compile(FXSceneManager.class, "global.less");

  private final Stage mainStage;
  private final StackPane playerPane = new StackPane();
  private final StackPane mainStagePane = new StackPane();
  private final StackPane uiPane = new StackPane();
  private final Scene scene = SceneUtil.createScene(uiPane);
  private final Scene mainStageScene = new Scene(new StackPane(), Color.BLACK);
  private final String title;

  private Stage uiStage;  // child stage of main stage
  private int screenNumber;

  public enum SceneLayout {

    /**
     * UI is rendered on the root stage
     */
    ROOT,

    /**
     * UI is rendered on a nested (transparent) child stage
     */
    CHILD
  }

  public FXSceneManager(String title, int initialScreenNumber, boolean alwaysOnTop) {
    this.title = title;
    this.screenNumber = initialScreenNumber;

    mainStagePane.setBackground(Background.EMPTY);

    mainStage = new Stage(StageStyle.UNDECORATED);
    mainStage.setAlwaysOnTop(alwaysOnTop);
    mainStage.setTitle(title);

    uiPane.setBackground(Background.EMPTY);

    scene.setFill(Color.BLACK);
    scene.getStylesheets().add(STYLES_URL);

    setSceneLayout(SceneLayout.CHILD);
  }

  @Override
  public void setSceneLayout(SceneLayout sceneLayout) {
    if(sceneLayout == SceneLayout.ROOT) {
      if(uiStage != null) {
        uiStage.close();
        uiStage.setScene(null);

        uiStage = null;
      }

      mainStagePane.getChildren().setAll(playerPane, uiPane);

      mainStageScene.setRoot(new StackPane());

      scene.setRoot(mainStagePane);
      scene.setFill(Color.BLACK);

      mainStage.setScene(scene);
    }
    else {
      mainStagePane.getChildren().setAll(playerPane);

      scene.setRoot(uiPane);
      scene.setFill(Color.TRANSPARENT);

      mainStageScene.setRoot(mainStagePane);

      mainStage.setScene(mainStageScene);

      if(uiStage == null) {
        uiStage = new Stage(StageStyle.TRANSPARENT);

        uiStage.setTitle(title + " Dialog");
        uiStage.initModality(Modality.APPLICATION_MODAL);
        uiStage.initOwner(mainStage);
        uiStage.setScene(scene);

        if(mainStage.isShowing()) {
          setupStageLocation(uiStage, getScreen());

          uiStage.show();
        }
      }
    }
  }

  @Override
  public StackPane getRootPane() {
    return uiPane;
  }

  @Override
  public Scene getScene() {
    return scene;
  }

  @Override
  public void display() {
    updateStageLocations();

    mainStage.show();

    if(uiStage != null) {
      uiStage.show();
    }

    mainStage.requestFocus();
  }

  @Override
  public void setPlayerRoot(Object playerDisplay) {
    if(playerDisplay instanceof Node) {

      /*
       * JavaFX node, just put it in stackpane of main scene.
       */

      playerPane.getChildren().setAll((Node)playerDisplay);
    }
  }

  @Override
  public void disposePlayerRoot() {
    playerPane.getChildren().clear();
  }

  @Override
  public int getScreenNumber() {
    return screenNumber;
  }

  @Override
  public void setScreenNumber(int screenNumber) {
    this.screenNumber = screenNumber;

    updateStageLocations();
  }

  private void updateStageLocations() {
    Screen screen = getScreen();

    setupStageLocation(mainStage, screen);

    if(uiStage != null) {
      setupStageLocation(uiStage, screen);
    }
  }

  @Override
  public Screen getScreen() {
    ObservableList<Screen> screens = Screen.getScreens();

    return screens.size() <= screenNumber ? Screen.getPrimary() : screens.get(screenNumber);
  }

  private static void setupStageLocation(Stage stage, Screen screen) {
    Rectangle2D bounds = screen.getBounds();

    boolean primary = screen.equals(Screen.getPrimary());    // WORKAROUND: this doesn't work nice in combination with full screen, so this hack is used to prevent going fullscreen when screen is not primary

    stage.setX(bounds.getMinX());
    stage.setY(bounds.getMinY());
    stage.setWidth(bounds.getWidth());
    stage.setHeight(bounds.getHeight());

    if(primary && stage.getOwner() == null) {
      stage.setFullScreen(true);
    }
  }
}
