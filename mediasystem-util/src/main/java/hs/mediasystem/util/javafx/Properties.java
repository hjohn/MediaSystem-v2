package hs.mediasystem.util.javafx;

import hs.mediasystem.util.javafx.property.SimpleReadOnlyObjectProperty;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;

public class Properties {
  public static <T> ReadOnlyProperty<T> readOnly(ObjectProperty<T> property) {
    return new SimpleReadOnlyObjectProperty<>(property);
  }
}
