package hs.mediasystem.runner;

import hs.mediasystem.framework.actions.ExposedMethod;
import hs.mediasystem.framework.actions.Member;
import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.runner.InputActionHandler.Action;

import java.util.ArrayDeque;
import java.util.Deque;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RootPresentationHandler {
  private static final Logger LOGGER = Logger.getLogger(RootPresentationHandler.class.getName());
  private static final Action BACK_ACTION;

  static {
    try {
      BACK_ACTION = new Action(new ActionTarget(List.of(new ExposedMethod(new Member(Navigable.class.getMethod("navigateBack", Event.class)), "navigateBack"))), "trigger");
    }
    catch(NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException();
    }
  }

  @Inject private Theme theme;
  @Inject private SceneManager sceneManager;
  @Inject private InputActionHandler inputActionHandler;

  @PostConstruct
  private void postConstruct() {
    sceneManager.getScene().addEventHandler(NavigateEvent.NAVIGATION_TO, e -> handleNavigateEvent(e));
    sceneManager.getScene().addEventHandler(NavigateEvent.NAVIGATION_BACK, e -> handleNavigateBackEvent(e));
    sceneManager.getScene().setOnKeyPressed(this::onKeyPressed);
    sceneManager.getScene().setOnKeyReleased(this::onKeyReleased);
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

      if(keyPressedCode == event.getCode() && heldTime >= 700) {
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

    handleEvent(event, actions);
  }

  private void handleEvent(Event event, List<Action> actions) {
    Node target = event.getTarget() instanceof Scene ? ((Scene)event.getTarget()).getRoot() : (Node)event.getTarget();

    List<Presentation> activePresentations = Stream.iterate(target, s -> s != null, Node::getParent)
      .map(s -> s.getProperties().get("presentation2"))
      .filter(Objects::nonNull)
      .map(Presentation.class::cast)
      .collect(Collectors.toList());

    LOGGER.fine("Possible Actions: " + actions + ", active presentations: " + activePresentations + ", for event: " + event);

    for(Presentation presentation : activePresentations) {
      inputActionHandler.keyPressed(event, presentation, actions);

      if(event.isConsumed()) {
        return;
      }
    }
  }

  public void handleNavigateBackEvent(NavigateEvent event) {
    handleEvent(event, List.of(BACK_ACTION));
  }

  public void handleNavigateEvent(NavigateEvent e) {
    // Find out hierarchy, including RootPresentation and create it all
    // Since this is the top level, it includes creating a Node.. normally it won't bubble up all the way to here because it is handled earlier (by RootPResentation for example) and no new node needs to be created, just the child changed
    // - In theory, we could have each presentation level handle this event, making it unnecessary to reverse look for the highest matching presentation, since the level that can handle the navigation is the correct level
    // - WHether or not a level can handle a presentation depends on whether the given presentation's hierarchy contains the current presentation or not.

    System.out.println("$$$$$$ Navigating to " + e.getPresentation());

    Presentation presentation = e.getPresentation();
    Node target = (Node)e.getTarget();

    Map<Class<? extends ParentPresentation>, ParentPresentation> activePresentations = Stream.iterate(target, s -> s.getParent() != null, Node::getParent)
      .map(s -> s.getProperties().get("presentation2"))
      .filter(ParentPresentation.class::isInstance)
      .map(ParentPresentation.class::cast)
      .collect(Collectors.toMap(ParentPresentation::getClass, Function.identity()));

    // Build a list of presentations:
    Deque<Presentation> presentations = new ArrayDeque<>();

    for(Presentation p = presentation;;) {
      presentations.addFirst(p);

      Class<? extends Presentation> parentCls = theme.findParent(p.getClass());
      ParentPresentation activePresentation = activePresentations.get(parentCls);

      if(activePresentation != null) {
        activePresentation.childPresentation.set(p);

        return;
      }
      if(parentCls == null) {
        break;
      }

      ParentPresentation instance = (ParentPresentation)theme.createPresentation(parentCls);

      instance.childPresentation.set(presentation);
      p = instance;
    }

    Node node = theme.findPlacer(null, presentations.getFirst()).place(null, presentations.getFirst());

    node.getProperties().put("presentation2", presentations.getFirst());

    sceneManager.getRootPane().getChildren().add(node);
  }
}
