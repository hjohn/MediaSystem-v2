package hs.mediasystem.runner;

import hs.mediasystem.runner.InputActionHandler.Action;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
public class SceneNavigator implements Navigable {
  private static final Logger LOGGER = Logger.getLogger(SceneNavigator.class.getName());

  @Inject private SceneManager sceneManager;
  @Inject private Provider<List<ScenePlugin<?, ?>>> pluginsProvider;
  @Inject private InputActionHandler inputActionHandler;

  private final NavigableProperty<SceneLocation> location = new NavigableProperty<>();
  private final StackPane sceneLayoutPane = new StackPane();

  @PostConstruct
  private void postConstruct() {
    location.addListener(this::locationChanged);

    sceneLayoutPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));  // TODO probably not needed

    sceneManager.getRootPane().getChildren().add(sceneLayoutPane);

    sceneLayoutPane.setOnKeyPressed(this::onKeyPressed);
    sceneLayoutPane.setOnKeyReleased(this::onKeyReleased);

    associatePresentation(sceneLayoutPane, this);
  }

  private final Map<Node, Object> presentations = new WeakHashMap<>();

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

    EventTarget currentEventChainNode = event.getTarget();

    while(currentEventChainNode != null) {
      Object presentation = presentations.get(currentEventChainNode);

      if(presentation != null) {
        inputActionHandler.keyPressed(event, presentation, actions);

        if(event.isConsumed()) {
          return;
        }
      }

      currentEventChainNode = currentEventChainNode instanceof Node ? ((Node)currentEventChainNode).getParent() : null;
    }
  }

  public void associatePresentation(Node node, Object presentation) {
    presentations.put(node, presentation);
  }

  public void setHistory(List<Object> values) {
    location.setHistory(values.stream().map(this::toSceneLocation).collect(Collectors.toList()));
  }

  public void update(Object location) {

  }

  public void go(Object newLocation) {
    location.set(toSceneLocation(newLocation));
  }

  private SceneLocation toSceneLocation(Object current) {
    @SuppressWarnings("unchecked")
    ScenePlugin<Object, Object> compatibleScenePlugin = (ScenePlugin<Object, Object>)pluginsProvider.get().stream()
      .filter(plugin -> plugin.getLocationClass().isAssignableFrom(current.getClass()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No matching ScenePlugin for location: " + current + "; available: " + pluginsProvider.get()));

    CachedScene cachedScene = location.getHistory().stream()
      .filter(Objects::nonNull)
      .map(sceneLocation -> sceneLocation.cachedScene)
      .filter(cs -> cs.plugin.equals(compatibleScenePlugin))
      .findFirst()
      .orElse(null);

    if(cachedScene != null) {
      return new SceneLocation(current, cachedScene);
    }

    Object presentation = compatibleScenePlugin.createPresentation();
    Node rootNode = compatibleScenePlugin.createNode(presentation);

    if(presentation != null) {
//      rootNode.setOnKeyPressed(e -> inputActionHandler.keyPressed(e, presentation));
      associatePresentation(rootNode, presentation);
    }

    return new SceneLocation(current, new CachedScene(compatibleScenePlugin, presentation, rootNode));
  }

  private void locationChanged(ObservableValue<? extends SceneLocation> ov, SceneLocation old, SceneLocation current) {
    Node rootNode = current.cachedScene.node;

    LOGGER.info("Navigating to " + current + " from " + old);

    if(current.cachedScene.presentation instanceof LocationPresentation) {
      @SuppressWarnings("unchecked")
      LocationPresentation<Object> locationPresentation = (LocationPresentation<Object>)current.cachedScene.presentation;

      locationPresentation.location.set(current.location);
    }

    sceneLayoutPane.getChildren().setAll(rootNode);

    /*
     * Special functionality for a background node:
     */

    Object backgroundNode = rootNode.getProperties().get("background");

    if(backgroundNode != null) {
      sceneManager.setPlayerRoot(backgroundNode);
      sceneManager.fillProperty().set(Color.TRANSPARENT);
    }
    else {
      sceneManager.disposePlayerRoot();
      sceneManager.fillProperty().set(Color.BLACK);
    }
  }

  @Override
  public void navigateBack(Event e) {
    LOGGER.fine("navigateBack(" + e + ") -- history = " + location.getHistory());

    if(location.back()) {
      e.consume();
    }
  }

  private static class SceneLocation {
    public final Object location;
    public final CachedScene cachedScene;

    public SceneLocation(Object location, CachedScene cachedScene) {
      this.location = location;
      this.cachedScene = cachedScene;
    }

    @Override
    public String toString() {
      return "SceneLocation[loc=" + location + "]";
    }
  }

  private static class CachedScene {
    public final ScenePlugin<?, ?> plugin;
    public final Object presentation;
    public final Node node;

    public CachedScene(ScenePlugin<?, ?> plugin, Object presentation, Node node) {
      this.plugin = plugin;
      this.presentation = presentation;
      this.node = node;
    }
  }
}
