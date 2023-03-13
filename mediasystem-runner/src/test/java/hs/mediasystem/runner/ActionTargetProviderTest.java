package hs.mediasystem.runner;

import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.util.expose.AbstractExposedProperty;
import hs.mediasystem.util.expose.Expose;
import hs.mediasystem.util.expose.ExposedControl;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionTargetProviderTest {

  private ActionTargetProvider provider = new ActionTargetProvider();

  @BeforeEach
  public void before() {
    Expose.longProperty(TestRoot::volume)
      .of(TestRoot.class)
      .range(0, 100, 1)
      .as("volume");

    Expose.action(TestRoot::stop)
      .of(TestRoot.class)
      .as("stop");

    Expose.action(TestRoot::stop2)
      .of(TestRoot.class)
      .as("stop2");

    Expose.numberProperty(TestRoot::brightness)
      .of(TestRoot.class)
      .range(0.0, 2.0, 0.1)
      .as("brightness");

    Expose.nodeProperty(TestRoot::player)
      .of(TestRoot.class)
      .provides(TestPlayer.class)
      .as("player");

    Expose.longProperty(TestPlayer::position)
      .of(TestPlayer.class)
      .range(0, 100, 1)
      .as("position");
  }

  @AfterEach
  public void after() {
    ExposedControl.clear();
  }

  @Test
  public void shouldFindTargets() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    assertEquals(5, actionTargets.size());

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("brightness")).findFirst().orElse(null);
    ActionTarget stopActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("stop")).findFirst().orElse(null);
    ActionTarget stop2ctionTarget = actionTargets.stream().filter(at -> at.toPath().equals("stop2")).findFirst().orElse(null);
    ActionTarget positionActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("player.position")).findFirst().orElse(null);

    assertNotNull(volumeActionTarget);
    assertNotNull(brightnessActionTarget);
    assertNotNull(stopActionTarget);
    assertNotNull(stop2ctionTarget);
    assertNotNull(positionActionTarget);
  }

  @Test
  public void shouldGetValues() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("brightness")).findFirst().orElse(null);
    ActionTarget positionActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("player.position")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    assertEquals(100L, getProperty(volumeActionTarget, root).getValue());
    assertEquals(0.5, getProperty(brightnessActionTarget, root).getValue());
    assertEquals(1L, getProperty(positionActionTarget, root).getValue());
  }

  @Test
  public void shouldDoTriggerActions() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget stopActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("stop")).findFirst().orElse(null);
    ActionTarget stop2ActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("stop2")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    stopActionTarget.createTrigger("trigger", root).run(new Event(Event.ANY), null);

    assertTrue(root.stopCalled.get());

    stop2ActionTarget.createTrigger("trigger", root).run(new Event(Event.ANY), null);

    assertTrue(root.stop2Called.get());
  }

  @Test
  public void shouldDoPropertyActions() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("brightness")).findFirst().orElse(null);
    ActionTarget positionActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("player.position")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    volumeActionTarget.createTrigger("subtract(5)", root).run(new Event(Event.ANY), null);

    assertEquals(95L, getProperty(volumeActionTarget, root).getValue());

    brightnessActionTarget.createTrigger("subtract(0.1)", root).run(new Event(Event.ANY), null);

    assertEquals(0.4, getProperty(brightnessActionTarget, root).getValue());

    positionActionTarget.createTrigger("add(11)", root).run(new Event(Event.ANY), null);

    assertEquals(12L, getProperty(positionActionTarget, root).getValue());
  }

  @Test
  public void shouldRespectRange() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.toPath().equals("brightness")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    volumeActionTarget.createTrigger("add(1)", root).run(new Event(Event.ANY), null);
    brightnessActionTarget.createTrigger("add(3.0)", root).run(new Event(Event.ANY), null);

    assertEquals(100L, getProperty(volumeActionTarget, root).getValue());
    assertEquals(2.0, getProperty(brightnessActionTarget, root).getValue());

    volumeActionTarget.createTrigger("add(-50)", root).run(new Event(Event.ANY), null);
    brightnessActionTarget.createTrigger("add(-1.0)", root).run(new Event(Event.ANY), null);

    assertEquals(50L, getProperty(volumeActionTarget, root).getValue());
    assertEquals(1.0, getProperty(brightnessActionTarget, root).getValue());

    volumeActionTarget.createTrigger("subtract(60)", root).run(new Event(Event.ANY), null);
    brightnessActionTarget.createTrigger("subtract(3.0)", root).run(new Event(Event.ANY), null);

    assertEquals(0L, getProperty(volumeActionTarget, root).getValue());
    assertEquals(0.0, getProperty(brightnessActionTarget, root).getValue());
  }

  @SuppressWarnings("unchecked")
  private static <T> Property<T> getProperty(ActionTarget actionTarget, Object root) {
    Object ownerInstance = actionTarget.findDirectOwnerInstanceFromRoot(root);

    return ((AbstractExposedProperty<T>)actionTarget.getExposedControl()).getProperty(ownerInstance);
  }

  private static class TestRoot {
    private LongProperty volumeProperty = new SimpleLongProperty(100L);
    private ObjectProperty<TestPlayer> playerProperty = new SimpleObjectProperty<>(new TestPlayer());
    private DoubleProperty brightnessProperty = new SimpleDoubleProperty(0.5);

    AtomicBoolean stopCalled = new AtomicBoolean(false);
    AtomicBoolean stop2Called = new AtomicBoolean(false);

    public void stop() {
      stopCalled.set(true);
    }

    public void stop2(@SuppressWarnings("unused") Event event) {
      stop2Called.set(true);
    }

    public LongProperty volume() {
      return volumeProperty;
    }

    public ObjectProperty<TestPlayer> player() {
      return playerProperty;
    }

    public DoubleProperty brightness() {
      return brightnessProperty;
    }
  }

  private static class TestPlayer {
    private LongProperty positionProperty = new SimpleLongProperty(1L);

    public LongProperty position() {
      return positionProperty;
    }
  }
}
