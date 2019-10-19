package hs.mediasystem.runner;

import hs.mediasystem.util.expose.Expose;
import hs.mediasystem.util.expose.ExposedControl;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactfx.value.Var;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ActionTargetProviderTest {

  private ActionTargetProvider provider = new ActionTargetProvider();

  @Before
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

  @After
  public void after() {
    ExposedControl.clear();
  }

  @Test
  public void shouldFindTargets() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    assertEquals(5, actionTargets.size());

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("brightness")).findFirst().orElse(null);
    ActionTarget stopActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("stop")).findFirst().orElse(null);
    ActionTarget stop2ctionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("stop2")).findFirst().orElse(null);
    ActionTarget positionActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("position")).findFirst().orElse(null);

    assertNotNull(volumeActionTarget);
    assertNotNull(brightnessActionTarget);
    assertNotNull(stopActionTarget);
    assertNotNull(stop2ctionTarget);
    assertNotNull(positionActionTarget);
  }

  @Test
  public void shouldGetValues() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("brightness")).findFirst().orElse(null);
    ActionTarget positionActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("position")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    assertEquals(100L, volumeActionTarget.getProperty(root).getValue());
    assertEquals(0.5, brightnessActionTarget.getProperty(root).getValue());
    assertEquals(1L, positionActionTarget.getProperty(root).getValue());
  }

  @Test
  public void shouldDoActions() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("brightness")).findFirst().orElse(null);
    ActionTarget stopActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("stop")).findFirst().orElse(null);
    ActionTarget stop2ActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("stop2")).findFirst().orElse(null);
    ActionTarget positionActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("position")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    stopActionTarget.doAction("trigger", root, new Event(Event.ANY));

    assertTrue(root.stopCalled.get());

    stop2ActionTarget.doAction("trigger", root, new Event(Event.ANY));

    assertTrue(root.stop2Called.get());

    volumeActionTarget.doAction("subtract(5)", root, new Event(Event.ANY));

    assertEquals(95L, volumeActionTarget.getProperty(root).getValue());

    brightnessActionTarget.doAction("subtract(0.1)", root, new Event(Event.ANY));

    assertEquals(0.4, brightnessActionTarget.getProperty(root).getValue());

    positionActionTarget.doAction("add(11)", root, new Event(Event.ANY));

    assertEquals(12L, positionActionTarget.getProperty(root).getValue());
  }

  @Test
  public void shouldRespectRange() {
    List<ActionTarget> actionTargets = provider.getActionTargets(TestRoot.class);

    ActionTarget volumeActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("volume")).findFirst().orElse(null);
    ActionTarget brightnessActionTarget = actionTargets.stream().filter(at -> at.getTargetName().equals("brightness")).findFirst().orElse(null);

    TestRoot root = new TestRoot();

    volumeActionTarget.doAction("add(1)", root, new Event(Event.ANY));
    brightnessActionTarget.doAction("add(3.0)", root, new Event(Event.ANY));

    assertEquals(100L, volumeActionTarget.getProperty(root).getValue());
    assertEquals(2.0, brightnessActionTarget.getProperty(root).getValue());

    volumeActionTarget.doAction("add(-50)", root, new Event(Event.ANY));
    brightnessActionTarget.doAction("add(-1.0)", root, new Event(Event.ANY));

    assertEquals(50L, volumeActionTarget.getProperty(root).getValue());
    assertEquals(1.0, brightnessActionTarget.getProperty(root).getValue());

    volumeActionTarget.doAction("subtract(60)", root, new Event(Event.ANY));
    brightnessActionTarget.doAction("subtract(3.0)", root, new Event(Event.ANY));

    assertEquals(0L, volumeActionTarget.getProperty(root).getValue());
    assertEquals(0.0, brightnessActionTarget.getProperty(root).getValue());
  }

  private static class TestRoot {
    private Var<Long> volumeProperty = Var.newSimpleVar(100L);
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

    public Var<Long> volume() {
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
    private Var<Long> positionProperty = Var.newSimpleVar(1L);

    public Var<Long> position() {
      return positionProperty;
    }
  }
}
