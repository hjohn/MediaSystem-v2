package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory;
import hs.mediasystem.ui.api.PersonClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Person;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.NaturalLanguage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipationsPresentationFactory extends GridViewPresentationFactory {

  private static final List<SortOrder<ConsolidatedParticipation>> SORT_ORDERS = List.of(
    new SortOrder<>("popularity", Comparator.comparing((ConsolidatedParticipation p) -> p.getPopularity()).reversed()),
    new SortOrder<>("alpha", Comparator.comparing(p -> p.getWork().getDetails(), Comparator.comparing(Details::getTitle, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<>("release-date", Comparator.comparing(p -> p.getWork().getDetails(), Comparator.comparing((Details d) -> d.getReleaseDate().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
  );

  private static final List<Filter<ConsolidatedParticipation>> FILTERS = List.of(
    new Filter<>("none", p -> true),
    new Filter<>("cast", p -> p.getRoleType() != Role.Type.CREW),
    new Filter<>("crew", p -> p.getRoleType() == Role.Type.CREW)
  );

  private static final List<Filter<ConsolidatedParticipation>> STATE_FILTERS = List.of(
    new Filter<>("none", p -> true),
    new Filter<>("available", p -> p.getWork().getPrimaryStream().isPresent()),
    new Filter<>("unwatched", p -> p.getWork().getPrimaryStream().isPresent() && !p.getWork().getState().isConsumed())
  );

  @Inject private PersonClient personClient;

  public ParticipationsPresentation create(PersonId id) {
    return new ParticipationsPresentation(id);
  }

  public class ParticipationsPresentation extends GridViewPresentation<ConsolidatedParticipation, ConsolidatedParticipation> {
    public final ObjectProperty<Person> person = new SimpleObjectProperty<>();

    private final PersonId id;

    public ParticipationsPresentation(PersonId id) {
      super("Roles", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), p -> p.getWork().getId());

      this.id = id;

      createUpdateTask().run();
    }

    @Override
    public Runnable createUpdateTask() {
      Person person = personClient.findPerson(id).orElseThrow(() -> new PersonNotFoundException(id));

      return () -> {
        Map<WorkId, ConsolidatedParticipation> map = new HashMap<>();

        this.person.set(person);

        for(Participation participation : person.getParticipations()) {
          map.computeIfAbsent(participation.getWork().getId(), k -> new ConsolidatedParticipation(participation.getWork())).addParticipation(participation);
        }

        this.inputItems.set(new ArrayList<>(map.values()));
      };
    }
  }
}
