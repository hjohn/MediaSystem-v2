package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.mediamanager.db.VideoDatabase;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;

public class PersonParticipationsPresentation extends GridViewPresentation {
  public final ObjectProperty<Person> person = new SimpleObjectProperty<>();
  public final ObjectProperty<PersonalProfile> personalProfile = new SimpleObjectProperty<>();

  @Inject private VideoDatabase videoDatabase;

  public static PresentationSetupTask<PersonParticipationsPresentation> createSetupTask(Person person) {
    return new PresentationSetupTask<>(PersonParticipationsPresentation.class) {
      @Override
      protected void call(PersonParticipationsPresentation p) {
        updateTitle("Loading...");

        p.person.set(person);
        p.personalProfile.set(p.videoDatabase.queryPersonalProfile(person.getIdentifier()));
      }
    };
  }
}
