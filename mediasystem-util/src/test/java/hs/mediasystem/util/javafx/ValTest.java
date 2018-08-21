package hs.mediasystem.util.javafx;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("static-method")
public class ValTest {

  @Test
  public void shouldMapAndFilter() {
    IntegerProperty number = new SimpleIntegerProperty(2);

    MonadicObjectBinding<Integer> mob = Binds.monadic(number).map(i -> (Integer)i * 2).filter(i -> i < 10);

    assertEquals((Integer)4, mob.get());

    number.set(3);

    assertEquals((Integer)6, mob.get());

    number.set(5);

    assertNull(mob.get());
  }

  @Test
  public void shouldHandleFilter() {
    IntegerProperty ip = new SimpleIntegerProperty(2);
    StringProperty text = new SimpleStringProperty("Name");

    MonadicObjectBinding<Boolean> condition = Binds.monadic(ip).map(i -> (Integer)i > 0);
    MonadicObjectBinding<String> mob = Binds.monadic(text).filter(Objects::nonNull).filter(condition);

    assertTrue(condition.get());
    assertEquals("Name", mob.get());

    text.set(null);

    assertTrue(condition.get());
    assertNull(mob.get());

    ip.set(-1);

    assertFalse(condition.get());
    assertNull(mob.get());

    text.set("Name");

    assertFalse(condition.get());
    assertNull(mob.get());

    ip.set(5);

    assertTrue(condition.get());
    assertEquals("Name", mob.get());
  }

  @Test
  public void shouldFlatMap() {
    BorderPane borderPane = new BorderPane();
    AtomicInteger invalidations = new AtomicInteger();

    Val<Scene> val = Binds.monadic(borderPane.parentProperty())
      .flatMap(Node::sceneProperty);

    val.addListener(obs -> invalidations.incrementAndGet());

    assertNull(val.getValue());
    assertEquals(0, invalidations.get());

    StackPane stackPane = new StackPane();

    stackPane.getChildren().add(borderPane);

    assertNull(val.getValue());
    assertEquals(1, invalidations.get());  // Value didn't change, invalidated so Val can start listening to the Scene property now

    Scene scene = new Scene(stackPane);

    assertEquals(scene, stackPane.getScene());
    assertEquals(scene, val.getValue());
    assertEquals(2, invalidations.get());

    stackPane.getChildren().remove(0);

    assertNull(val.getValue());
    assertEquals(3, invalidations.get());

    stackPane.getChildren().add(borderPane);

    assertEquals(scene, val.getValue());
    assertEquals(4, invalidations.get());

    StackPane stackPane2 = new StackPane();

    Scene scene2 = new Scene(stackPane2);

    stackPane.getChildren().remove(0);
    stackPane2.getChildren().add(borderPane);

    assertEquals(scene2, val.getValue());
    assertEquals(5, invalidations.get());

    scene.setRoot(new StackPane());  // If old listener was registered, this would trigger an invalidation, even though it is no longer part of the chain

    assertEquals(scene2, val.getValue());
    assertEquals(5, invalidations.get());  // Should not have changed.
  }
}
