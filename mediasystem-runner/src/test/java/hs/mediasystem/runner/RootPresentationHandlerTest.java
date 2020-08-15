package hs.mediasystem.runner;

import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Placer;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.util.PostConstructCaller;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class RootPresentationHandlerTest {
  @Mock private Theme theme;
  @Mock private SceneManager sceneManager;
  @Mock private Placer<ParentPresentation, TopPresentation> placer;
  @Mock private Placer<ParentPresentation, MiddlePresentation> middlePlacer;
  @Mock private Placer<ParentPresentation, BottomPresentation> bottomPlacer;
  @InjectMocks private RootPresentationHandler handler = new RootPresentationHandler();

  private Button button;
  private StackPane subPane;
  private StackPane rootPane;
  private Scene scene;

  private TopPresentation topPresentation = new TopPresentation();
  private Top2Presentation top2Presentation = new Top2Presentation();
  private MiddlePresentation middlePresentation = new MiddlePresentation();
  private BottomPresentation bottomPresentation = new BottomPresentation();

  @BeforeAll
  public static void beforeAll() {
    Platform.startup(() -> {});
  }

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    button = new Button();
    subPane = new StackPane(button);
    rootPane = new StackPane(subPane);
    scene = new Scene(rootPane);

    when(sceneManager.getScene()).thenReturn(scene);
    when(sceneManager.getRootPane()).thenReturn(rootPane);

    PostConstructCaller.call(handler);
  }

  @Test
  public void shouldReplaceTopLevelWithNewPresentation() {
    when(theme.findPlacer(null, topPresentation)).thenReturn(placer);
    when(placer.place(null, topPresentation)).thenReturn(button);

    Event.fireEvent(button, NavigateEvent.to(topPresentation));

    assertEquals(button, rootPane.getChildren().get(0));
    assertEquals(topPresentation, button.getProperties().get("presentation2"));
  }

  @Test
  public void shouldReplaceTopLevelWithTwoNewPresentations() {
    when(theme.createPresentation(MiddlePresentation.class)).thenReturn(middlePresentation);
    when(theme.findParent(TopPresentation.class)).thenAnswer(p -> MiddlePresentation.class);
    when(theme.findPlacer(null, middlePresentation)).thenReturn(middlePlacer);
    when(middlePlacer.place(null, middlePresentation)).thenReturn(button);

    Event.fireEvent(button, NavigateEvent.to(topPresentation));

    assertEquals(button, rootPane.getChildren().get(0));
    assertEquals(middlePresentation, button.getProperties().get("presentation2"));
    assertEquals(topPresentation, middlePresentation.childPresentation.get());
  }

  @Test
  public void shouldReplaceTopLevelWithThreeNewPresentations() {
    when(theme.createPresentation(MiddlePresentation.class)).thenReturn(middlePresentation);
    when(theme.createPresentation(BottomPresentation.class)).thenReturn(bottomPresentation);
    when(theme.findParent(TopPresentation.class)).thenAnswer(p -> MiddlePresentation.class);
    when(theme.findParent(MiddlePresentation.class)).thenAnswer(p -> BottomPresentation.class);
    when(theme.findPlacer(null, bottomPresentation)).thenReturn(bottomPlacer);
    when(bottomPlacer.place(null, bottomPresentation)).thenReturn(button);

    Event.fireEvent(button, NavigateEvent.to(topPresentation));

    assertEquals(button, rootPane.getChildren().get(0));
    assertEquals(bottomPresentation, button.getProperties().get("presentation2"));
    assertEquals(middlePresentation, bottomPresentation.childPresentation.get());
    assertEquals(topPresentation, middlePresentation.childPresentation.get());
  }

  @Test
  public void shouldReplaceMiddlePresentationChild() {
    shouldReplaceTopLevelWithTwoNewPresentations();

    when(theme.findParent(Top2Presentation.class)).thenAnswer(p -> MiddlePresentation.class);

    Event.fireEvent(button, NavigateEvent.to(top2Presentation));

    assertEquals(button, rootPane.getChildren().get(0));
    assertEquals(middlePresentation, button.getProperties().get("presentation2"));
    assertEquals(top2Presentation, middlePresentation.childPresentation.get());
  }

  private static class TopPresentation implements Presentation {
  }

  private static class Top2Presentation implements Presentation {
  }

  private static class MiddlePresentation extends ParentPresentation {
  }

  private static class BottomPresentation extends ParentPresentation {
  }
}
