package hs.mediasystem.runner;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LocationPresentation<L> {
  public final ObjectProperty<L> location = new SimpleObjectProperty<>();
}
