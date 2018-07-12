package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Person;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class PersonParticipationsPresentation extends GridViewPresentation {
  public final ObjectProperty<Person> person = new SimpleObjectProperty<>();

  public PersonParticipationsPresentation set(Person person) {
    this.person.set(person);

    return this;
  }
}
