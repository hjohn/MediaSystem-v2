package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation;
import hs.mediasystem.ui.api.PersonClient;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Person;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.NaturalLanguage;

import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ParticipationsPresentation extends GridViewPresentation<Participation> {
  public final Person person;

  private static final List<SortOrder<Participation>> SORT_ORDERS = List.of(
    new SortOrder<>("popularity", Comparator.comparing((Participation p) -> p.getPopularity()).reversed()),
    new SortOrder<>("alpha", Comparator.comparing(p -> p.getWork().getDetails(), Comparator.comparing(Details::getName, NaturalLanguage.ALPHABETICAL))),
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
    new Filter<>("unwatched", p -> p.getWork().getPrimaryStream().isPresent() && !p.getWork().getState().isConsumed().getValue())
  );

  @Singleton
  public static class Factory {
    @Inject private PersonClient personClient;
    @Inject private SettingsClient settingsClient;
    @Inject private BinderProvider binderProvider;

    public ParticipationsPresentation create(PersonId id) {
      Person person = personClient.findPerson(id).orElseThrow();

      return new ParticipationsPresentation(
        settingsClient,
        binderProvider,
        person,
        FXCollections.observableList(person.getParticipations())
      );
    }
  }

  protected ParticipationsPresentation(SettingsClient settingsClient, BinderProvider binderProvider, Person person, ObservableList<Participation> items) {
    super(settingsClient.of(SYSTEM_PREFIX + "Roles"), binderProvider, items, new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null);

    this.person = person;
  }
}
