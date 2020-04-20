package hs.mediasystem.util.javafx.action;

import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;

import org.reactfx.value.Val;

public class SimpleAction implements Action {
  private final ReadOnlyStringWrapper titleProperty = new ReadOnlyStringWrapper("(Untitled)");
  private final ReadOnlyBooleanWrapper enabledProperty = new ReadOnlyBooleanWrapper(true);
  private final Consumer<Event> consumer;
  private final ObservableValue<Boolean> enabled;

  public SimpleAction(String title, ObservableValue<Boolean> enabled, Consumer<Event> consumer) {
    this.titleProperty.set(title);
    this.enabled = enabled;
    this.consumer = consumer;

    this.enabledProperty.bind(this.enabled);
  }

  public SimpleAction(String title, Consumer<Event> consumer) {
    this.titleProperty.set(title);
    this.enabled = null;
    this.consumer = consumer;
  }

  public SimpleAction(Consumer<Event> consumer) {
    this.enabled = null;
    this.consumer = consumer;
  }

  @Override
  public ReadOnlyStringProperty titleProperty() {
    return titleProperty.getReadOnlyProperty();
  }

  @Override
  public Val<Boolean> enabledProperty() {
    return Val.wrap(enabledProperty.getReadOnlyProperty());
  }

  @Override
  public void trigger(Event event) {
    if(enabledProperty.get()) {
      consumer.accept(event);
      event.consume();
    }
  }
}
