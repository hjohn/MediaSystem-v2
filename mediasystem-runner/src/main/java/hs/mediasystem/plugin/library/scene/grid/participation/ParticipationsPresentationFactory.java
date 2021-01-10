package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory;
import hs.mediasystem.ui.api.PersonClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Person;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.NaturalLanguage;

import java.util.Comparator;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipationsPresentationFactory extends GridViewPresentationFactory {

  private static final List<SortOrder<Participation>> SORT_ORDERS = List.of(
    new SortOrder<>("popularity", Comparator.comparing((Participation p) -> p.getPopularity()).reversed()),
    new SortOrder<>("alpha", Comparator.comparing(p -> p.getWork().getDetails(), Comparator.comparing(Details::getTitle, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<>("release-date", Comparator.comparing(p -> p.getWork().getDetails(), Comparator.comparing((Details d) -> d.getReleaseDate().orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))))
  );

  private static final List<Filter<Participation>> FILTERS = List.of(
    new Filter<>("none", p -> true),
    new Filter<>("cast", p -> p.getRole().getType() != Role.Type.CREW),
    new Filter<>("crew", p -> p.getRole().getType() == Role.Type.CREW)
  );

  private static final List<Filter<Participation>> STATE_FILTERS = List.of(
    new Filter<>("none", p -> true),
    new Filter<>("available", p -> p.getWork().getPrimaryStream().isPresent()),
    new Filter<>("unwatched", p -> p.getWork().getPrimaryStream().isPresent() && !p.getWork().getState().isConsumed())
  );

  @Inject private PersonClient personClient;

  public ParticipationsPresentation create(PersonId id) {
    return new ParticipationsPresentation(id);
  }

  public class ParticipationsPresentation extends GridViewPresentation<Participation, Participation> {
    public final ObjectProperty<Person> person = new SimpleObjectProperty<>();

    private final PersonId id;

    public ParticipationsPresentation(PersonId id) {
      super("Roles", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS));

      this.id = id;

      createUpdateTask().run();
    }

    @Override
    public Runnable createUpdateTask() {
      Person person = personClient.findPerson(id).orElseThrow(() -> new PersonNotFoundException(id));

      return () -> {
        this.person.set(person);
        this.inputItems.set(person.getParticipations());
      };
    }
  }
}
