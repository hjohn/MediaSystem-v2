package hs.mediasystem.runner;

import hs.ddif.core.Injector;
import hs.mediasystem.plugin.basictheme.BasicTheme;
import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.InputActionHandler.Action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class SceneNavigator {
  private static final Logger LOGGER = Logger.getLogger(SceneNavigator.class.getName());

  @Inject private SceneManager sceneManager;
  @Inject private InputActionHandler inputActionHandler;
  @Inject private Provider<BasicTheme> themeProvider;
  @Inject private RootNodeFactory rootNodeFactory;
  @Inject private Injector injector;

  private final RootPresentation rootPresentation = new RootPresentation();

  private StackPane sceneLayoutPane;
  private BasicTheme theme;

  @PostConstruct
  private void postConstruct() {
    theme = themeProvider.get();
    sceneLayoutPane = (StackPane)rootNodeFactory.create(rootPresentation);

    sceneLayoutPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));  // TODO probably not needed

    sceneManager.getRootPane().getChildren().add(sceneLayoutPane);

    sceneLayoutPane.setOnKeyPressed(this::onKeyPressed);
    sceneLayoutPane.setOnKeyReleased(this::onKeyReleased);
  }

  private KeyCode keyPressedCode;
  private long keyPressedStartTime;

  private void onKeyReleased(KeyEvent event) {
    if(event.getCode().isModifierKey()) {
      return;
    }

    if(event.getCode().isFunctionKey() && keyPressedCode == event.getCode()) {
      long heldTime = System.currentTimeMillis() - keyPressedStartTime;

      if(heldTime >= 0 && heldTime <= 500) {
        handleKeyEvent(event, false);
      }

      keyPressedCode = null;
    }
  }

  private void onKeyPressed(KeyEvent event) {

    /*
     * Handling of user defined key combinations:
     * - Check up the chain from the event target to find relevant presentations
     * - Check each presentation in turn for potential actions
     */

    if(event.getCode().isModifierKey()) {
      return;
    }

    if(event.getCode().isFunctionKey()) {
      if(keyPressedCode != event.getCode()) {
        keyPressedCode = event.getCode();
        keyPressedStartTime = System.currentTimeMillis();
      }

      long heldTime = System.currentTimeMillis() - keyPressedStartTime;

      if(keyPressedCode == event.getCode() && heldTime >= 1000) {
        keyPressedStartTime = Long.MAX_VALUE;  // prevents repeating long press keys

        handleKeyEvent(event, true);
      }

      return;
    }

    handleKeyEvent(event, false);
  }

  private void handleKeyEvent(KeyEvent event, boolean longPress) {
    List<Action> actions = inputActionHandler.findActions(InputActionHandler.keyEventToKeyCodeCombination(event, longPress));

    if(actions.isEmpty()) {
      return;
    }

    ArrayDeque<Presentation> presentations = new ArrayDeque<>();

    presentations.add(rootPresentation);

    while(presentations.getFirst() instanceof ParentPresentation && ((ParentPresentation)presentations.getFirst()).childPresentationProperty().get() != null) {
      presentations.addFirst(((ParentPresentation)presentations.getFirst()).childPresentationProperty().get());
    }

    for(Presentation presentation : presentations) {
      System.out.println(">>> Attempting " + presentation + " for actions " + actions);
      inputActionHandler.keyPressed(event, presentation, actions);

      if(event.isConsumed()) {
        return;
      }
    }
  }

  public void navigateTo(Presentation presentation) {
    if(presentation == null || presentation instanceof RootPresentation) {
      throw new IllegalStateException();
    }

    // Build a list of presentations:
    List<Presentation> presentations = new ArrayList<>();

    for(Presentation p = presentation;;) {
      presentations.add(p);

      Class<? extends Presentation> parent = theme.findParent(p.getClass());

      if(parent == null) {
        throw new IllegalStateException(theme + " does not define parent for " + p.getClass());
      }
      if(parent.equals(RootPresentation.class)) {
        break;
      }

      ParentPresentation instance = (ParentPresentation)injector.getInstance(parent);

      instance.childPresentationProperty().set(presentation);
      p = instance;
    }

    Collections.reverse(presentations);

    // Find common parent in root presentation, and replace the stack at that level:
    ParentPresentation parent = rootPresentation;

    for(Presentation p : presentations) {
      Presentation child = parent.childPresentationProperty().get();

      if(child == null || !child.getClass().equals(p.getClass())) {
        parent.childPresentationProperty().set(p);
        break;
      }

      parent = (ParentPresentation)child;
    }
  }
}
