package hs.mediasystem.runner;

import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.runner.InputActionHandler.Action;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.util.expose.Expose;
import hs.mediasystem.util.expose.ExposedControl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RootPresentationHandler {
  private static final Logger LOGGER = Logger.getLogger(RootPresentationHandler.class.getName());
  private static final Action BACK_ACTION;

  static {
    Expose.action(Navigable::navigateBack)
      .of(Navigable.class)
      .as("navigateBack");

    BACK_ACTION = new Action(new ActionTarget(List.of(ExposedControl.find(Navigable.class, "navigateBack"))), "trigger");
  }

  @Inject private Theme theme;
  @Inject private SceneManager sceneManager;
  @Inject private InputActionHandler inputActionHandler;
  @Inject private ContextMenuHandler contextMenuHandler;

  @PostConstruct
  private void postConstruct() {
    sceneManager.getScene().addEventHandler(NavigateEvent.NAVIGATION_TO, e -> handleNavigateEvent(e));
    sceneManager.getScene().addEventHandler(NavigateEvent.NAVIGATION_BACK, e -> handleNavigateBackEvent(e));
    sceneManager.getScene().setOnKeyPressed(this::onKeyPressed);
    sceneManager.getScene().setOnKeyReleased(this::onKeyReleased);
    sceneManager.getScene().setOnMouseClicked(this::onMouseClicked);

    sceneManager.getRootPane().getStyleClass().setAll("root", "media-look");
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
      // Special handling of Context Menu key
      if(event.getCode() == KeyCode.F10) {
        contextMenuHandler.handle(event, createPresentationStack(event));

        return;
      }

      if(keyPressedCode != event.getCode()) {
        keyPressedCode = event.getCode();
        keyPressedStartTime = System.currentTimeMillis();
      }

      long heldTime = System.currentTimeMillis() - keyPressedStartTime;

      if(keyPressedCode == event.getCode() && heldTime >= 700) {
        keyPressedStartTime = Long.MAX_VALUE;  // prevents repeating long press keys

        handleKeyEvent(event, true);
      }

      return;
    }

    handleKeyEvent(event, false);
  }

  private void onMouseClicked(MouseEvent event) {
    if(event.getButton() == MouseButton.SECONDARY) {
      Event.fireEvent(event.getTarget(), NavigateEvent.back());
      event.consume();
    }
  }

  private void handleKeyEvent(KeyEvent event, boolean longPress) {
    List<Action> actions = inputActionHandler.findActions(InputActionHandler.keyEventToKeyCodeCombination(event, longPress));

    if(actions.isEmpty()) {
      return;
    }

    handleEvent(event, actions);
  }

  private void handleEvent(Event event, List<Action> actions) {
    List<Presentation> activePresentations = createPresentationStack(event);

    LOGGER.fine("Possible Actions: " + actions + ", active presentations: " + activePresentations + ", for event: " + event);

    for(Presentation presentation : activePresentations) {
      inputActionHandler.keyPressed(event, presentation, actions);

      if(event.isConsumed()) {
        return;
      }
    }
  }

  private static List<Presentation> createPresentationStack(Event event) {
    Node target = event.getTarget() instanceof Scene ? ((Scene)event.getTarget()).getRoot() : (Node)event.getTarget();

    return Stream.iterate(target, Objects::nonNull, Node::getParent)
      .map(s -> s.getProperties().get("presentation2"))
      .filter(Objects::nonNull)
      .map(Presentation.class::cast)
      .collect(Collectors.toList());
  }

  public void handleNavigateBackEvent(NavigateEvent event) {
    handleEvent(event, List.of(BACK_ACTION));
  }

  public void handleNavigateEvent(NavigateEvent e) {
    // Find out hierarchy, including RootPresentation and create it all
    // Since this is the top level, it includes creating a Node.. normally it won't bubble up all the way to here because it is handled earlier (by RootPResentation for example) and no new node needs to be created, just the child changed
    // - In theory, we could have each presentation level handle this event, making it unnecessary to reverse look for the highest matching presentation, since the level that can handle the navigation is the correct level
    // - WHether or not a level can handle a presentation depends on whether the given presentation's hierarchy contains the current presentation or not.

    /*
     * Create map of active presentations:
     */

    Node target = (Node)e.getTarget();

    Map<Class<? extends ParentPresentation>, ParentPresentation> activePresentations = Stream.iterate(target, s -> s.getParent() != null, Node::getParent)
      .map(s -> s.getProperties().get("presentation2"))
      .filter(ParentPresentation.class::isInstance)
      .map(ParentPresentation.class::cast)
      .collect(Collectors.toMap(ParentPresentation::getClass, Function.identity()));

    /*
     * Loop through target presentation and its parents to find nearest existing parent or create a new hierarchy:
     */

    Presentation targetPresentation = e.getPresentation();  // The new presentation to use

    for(Class<? extends Presentation> parentCls; (parentCls = theme.findParent(targetPresentation.getClass())) != null;) {
      ParentPresentation activePresentation = activePresentations.get(parentCls);  // See if we have intended parent already in current hierarchy

      if(activePresentation != null) {  // If intended parent already existed, just switch its child
        activePresentation.childPresentation.set(targetPresentation);

        return;
      }

      // Create the intended parent and set its child
      activePresentation = (ParentPresentation)theme.createPresentation(parentCls);

      activePresentation.childPresentation.set(targetPresentation);
      targetPresentation = activePresentation;
    }

    /*
     * Create a new root node as the target presentation required a new root presentation (not
     * necessarily the target presentation itself, it could also be one of its intended parents),
     * hence why "presentations.getFirst()" is used here.  The child hierarchy towards the target
     * presentation is already set up at this point.
     */

    Node node = theme.findPlacer(null, targetPresentation).place(null, targetPresentation);

    node.getProperties().put("presentation2", targetPresentation);

    sceneManager.getRootPane().getChildren().setAll(node);
  }
}
