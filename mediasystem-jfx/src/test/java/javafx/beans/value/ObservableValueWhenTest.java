package javafx.beans.value;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObservableValueWhenTest {

  @Test
  void whenNotObservedShouldNeverCallDownstreamMapFunction() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(false);
    List<String> observedMappings = new ArrayList<>();

    property.when(condition).map(observedMappings::add);

    assertEquals(List.of(), observedMappings);

    condition.set(true);

    assertEquals(List.of(), observedMappings);

    property.set("b");

    assertEquals(List.of(), observedMappings);

    condition.set(false);

    assertEquals(List.of(), observedMappings);

    property.set("c");

    assertEquals(List.of(), observedMappings);

    condition.set(true);

    assertEquals(List.of(), observedMappings);
  }

  @Test  // TODO remove this test if it never fails alone
  void whenObservedShouldActAccordingly() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(false);

    property.addListener((obs, old, current) -> System.out.println(current));

    List<String> list = new ArrayList<>();

    property.when(condition).map(list::add);

    assertEquals(List.of(), list);

    condition.set(true);

    assertEquals(List.of(), list);

    property.set("b");

    assertEquals(List.of(), list);

    condition.set(false);

    assertEquals(List.of(), list);

    property.set("c");

    assertEquals(List.of(), list);

    condition.set(true);

    assertEquals(List.of(), list);
  }

  @Test
  void whenBindingIsObservedShouldCallDownstreamMapFunctionOnlyWhenAbsolutelyNecessary() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(false);
    List<String> observedMappings = new ArrayList<>();
    List<String> observedChanges = new ArrayList<>();

    property.when(condition).map(x -> {observedMappings.add(x); return x;}).addListener((obs, old, current) -> observedChanges.add(old + " -> " + current));

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of(), observedChanges);

    condition.set(true);

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of(), observedChanges);

    property.set("b");

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("a -> b"), observedChanges);

    condition.set(false);

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("a -> b"), observedChanges);

    property.set("c");

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("a -> b"), observedChanges);

    condition.set(true);

    assertEquals(List.of("a", "b", "c"), observedMappings);
    assertEquals(List.of("a -> b", "b -> c"), observedChanges);
  }

  @Test
  void whenBindingIsBoundShouldCallDownstreamMapFunctionOnlyWhenAbsolutelyNecessary() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(false);
    List<String> observedMappings = new ArrayList<>();
    List<String> observedChanges = new ArrayList<>();

    StringProperty bound = new SimpleStringProperty("x");

    bound.addListener((obs, old, current) -> observedChanges.add(old + " -> " + current));
    bound.bind(property.when(condition).map(x -> {observedMappings.add(x); return x;}));

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of("x -> a"), observedChanges);

    condition.set(true);

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of("x -> a"), observedChanges);

    property.set("b");

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("x -> a", "a -> b"), observedChanges);

    condition.set(false);

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("x -> a", "a -> b"), observedChanges);

    property.set("c");

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("x -> a", "a -> b"), observedChanges);

    condition.set(true);

    assertEquals(List.of("a", "b", "c"), observedMappings);
    assertEquals(List.of("x -> a", "a -> b", "b -> c"), observedChanges);
  }

  @Test
  void whenBindingIsObservedShouldActAccordingly_2() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(false);
    List<String> observedMappings = new ArrayList<>();
    List<String> observedChanges = new ArrayList<>();

    property.when(condition).map(x -> {observedMappings.add(x); return x;}).addListener((obs, old, current) -> observedChanges.add(old + " -> " + current));

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of(), observedChanges);

    property.set("b");

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of(), observedChanges);

    condition.set(true);

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("a -> b"), observedChanges);
  }

  @Test
  void whenBindingIsObservedShouldActAccordingly_3_true() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(true);
    List<String> observedMappings = new ArrayList<>();
    List<String> observedChanges = new ArrayList<>();

    property.when(condition).map(x -> {observedMappings.add(x); return x;}).addListener((obs, old, current) -> observedChanges.add(old + " -> " + current));

    assertEquals(List.of("a"), observedMappings);
    assertEquals(List.of(), observedChanges);

    property.set("b");

    assertEquals(List.of("a", "b"), observedMappings);
    assertEquals(List.of("a -> b"), observedChanges);
  }

  @Test
  void whenBindingAndPropertyIsObservedShouldActAccordingly() {
    StringProperty property = new SimpleStringProperty("a");
    BooleanProperty condition = new SimpleBooleanProperty(false);
    List<String> observedMappings = new ArrayList<>();

    property.addListener((obs, old, current) -> System.out.println("Property: " + current));
    property.when(condition).map(x -> {observedMappings.add(x); return x;}).addListener((obs, old, current) -> System.out.println("Binding: " + current));

    assertEquals(List.of("a"), observedMappings);

    condition.set(true);

    assertEquals(List.of("a"), observedMappings);

    property.set("b");

    assertEquals(List.of("a", "b"), observedMappings);

    condition.set(false);

    assertEquals(List.of("a", "b"), observedMappings);

    property.set("c");

    assertEquals(List.of("a", "b"), observedMappings);

    condition.set(true);

    assertEquals(List.of("a", "b", "c"), observedMappings);
  }
}
