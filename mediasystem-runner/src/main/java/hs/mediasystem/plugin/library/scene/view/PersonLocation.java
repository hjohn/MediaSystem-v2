package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.plugin.library.scene.LibraryLocation;

public class PersonLocation extends LibraryLocation {

  public PersonLocation(Person person) {
    super(person, null);
  }

}
