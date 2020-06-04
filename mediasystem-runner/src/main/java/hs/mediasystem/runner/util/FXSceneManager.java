package hs.mediasystem.runner.util;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;
import hs.mediasystem.util.javafx.SceneUtil;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Manages two Stages layered on top of each other, an empty top level stage,
 * and a content stage that is a child of the top level stage.  The content
 * stage is transparent, and the content of the empty top level stage can
 * show through when needed.<p>
 *
 * When playing videos, the empty top level stage's Window ID can be supplied
 * and the video can be placed directly into the top level stage.  By making
 * the content stage (partially) transparent, the video becomes visible and
 * controls can be overlayed using the content stage.<p>
 *
 * A major advantage vs the two top level window approach is that there is
 * only one window that can receive focus (and only one window will be on the
 * task bar), so it is not possible for the wrong window to have focus or for
 * the overlay to be invisible if the wrong window is at the front.<p>
 *
 * Note: Window ID is determined using a native call into User32.dll by finding
 * a Window with our title.  This is (currently) platform specific, and may
 * fail if there are other Windows with the same title.
 */
public class FXSceneManager implements SceneManager, PlayerWindowIdSupplier {
  private static final LessLoader LESS_LOADER = new LessLoader(FXSceneManager.class);

  private final Stage mainStage;
  private final Stage contentStage;  // child stage of main stage
  private final StackPane mainStagePane = new StackPane();
  private final StackPane rootPane = new StackPane();
  private final Scene scene = SceneUtil.createScene(rootPane);
  private final String title;

  private int screenNumber;

  public FXSceneManager(String title, int initialScreenNumber, boolean alwaysOnTop) {
    this.title = title;
    this.screenNumber = initialScreenNumber;

    Scene emptyScene = new Scene(mainStagePane);

    emptyScene.setFill(Color.BLACK);
    mainStagePane.setBackground(Background.EMPTY);

    mainStage = new Stage(StageStyle.UNDECORATED);
    mainStage.setAlwaysOnTop(alwaysOnTop);
    mainStage.setTitle(title);
    mainStage.setScene(emptyScene);

    rootPane.setBackground(Background.EMPTY);

    scene.setFill(Color.BLACK);
    scene.getStylesheets().add(LESS_LOADER.compile("global.less"));

    contentStage = new Stage(StageStyle.TRANSPARENT);
    contentStage.setTitle(title + " Dialog");
    contentStage.initModality(Modality.APPLICATION_MODAL);
    contentStage.initOwner(mainStage);
    contentStage.setScene(scene);
  }

  @Override
  public StackPane getRootPane() {
    return rootPane;
  }

  @Override
  public Scene getScene() {
    return scene;
  }

  @Override
  public ObjectProperty<Paint> fillProperty() {
    return scene.fillProperty();
  }

  @Override
  public void display() {
    Screen screen = determineScreen();

    setupStageLocation(mainStage, screen);
    setupStageLocation(contentStage, screen);

    mainStage.show();
    contentStage.show();

    mainStage.requestFocus();
  }

  @Override
  public void setPlayerRoot(Object playerDisplay) {
    if(playerDisplay instanceof Node) {

      /*
       * JavaFX node, just put it in stackpane of main scene.
       */

      mainStagePane.getChildren().add((Node)playerDisplay);
    }
  }

  @Override
  public void disposePlayerRoot() {
    mainStagePane.getChildren().clear();
  }

  @Override
  public int getScreenNumber() {
    return screenNumber;
  }

  @Override
  public void setScreenNumber(int screenNumber) {
    this.screenNumber = screenNumber;

    Screen screen = determineScreen();

    setupStageLocation(mainStage, screen);
    setupStageLocation(contentStage, screen);
  }

  @Override
  public long getWindowId() {
    WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, title);

    return Pointer.nativeValue(hwnd.getPointer());
  }

  private Screen determineScreen() {
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
