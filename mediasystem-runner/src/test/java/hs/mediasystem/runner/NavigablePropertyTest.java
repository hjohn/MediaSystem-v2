package hs.mediasystem.runner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NavigablePropertyTest {

  private NavigableProperty<String> property = new NavigableProperty<>();

  @Test
  public void shouldNavigateCorrectly() {
    assertNull(property.get());
    assertFalse(property.back());
    assertFalse(property.forward());

    property.set("A");

    assertEquals("A", property.get());

    assertFalse(property.forward());
    assertTrue(property.back());
    assertNull(property.get());

    assertTrue(property.forward());
    assertEquals("A", property.get());

    property.set("B");

    assertEquals("B", property.get());
    assertTrue(property.back());
    assertEquals("A", property.get());

    property.set("C");

    assertEquals("C", property.get());
    assertFalse(property.forward());
    assertTrue(property.back());
    assertEquals("A", property.get());
    assertTrue(property.back());
    assertNull(property.get());
    assertFalse(property.back());
    assertTrue(property.forward());
    assertEquals("A", property.get());
    assertTrue(property.forward());
    assertEquals("C", property.get());
    assertFalse(property.forward());
    assertEquals("C", property.get());
  }
}
