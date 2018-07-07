package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.util.ImageHandle;

import java.net.URL;
import java.time.LocalDate;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Media {
  public final StringProperty title = new SimpleStringProperty(this, "title");
  public final StringProperty subtitle = new SimpleStringProperty(this, "subtitle");

  public final StringProperty prefix = new SimpleStringProperty(this, "prefix");
  public final StringProperty description = new SimpleStringProperty(this, "description");
  public final ObjectProperty<LocalDate> releaseDate = new SimpleObjectProperty<>(this, "releaseDate");

  public final ObjectProperty<ImageHandle> image = new SimpleObjectProperty<>(this, "image");
  public final ObjectProperty<ImageHandle> backdrop = new SimpleObjectProperty<>(this, "backdrop");
  public final ObjectProperty<ImageHandle> banner = new SimpleObjectProperty<>(this, "banner");
  public final ObjectProperty<Integer> sequence = new SimpleObjectProperty<>(this, "sequence");

  public final StringProperty language = new SimpleStringProperty(this, "language");
  public final StringProperty tagLine = new SimpleStringProperty(this, "tagLine");
  public final StringProperty groupTitle = new SimpleStringProperty(this, "groupTitle");

  public final ObjectProperty<URL> url = new SimpleObjectProperty<>(this, "url");

  public final ObjectProperty<Identifier> identifier = new SimpleObjectProperty<>(this, "identifier");
}
