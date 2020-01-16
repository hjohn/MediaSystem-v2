package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.ui.api.domain.Work;

import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ContributionsPresentation extends GridViewPresentation<Contribution> {
  public final Work work;

  private static final List<SortOrder<Contribution>> SORT_ORDERS = List.of(
    new SortOrder<>("best", Comparator.comparing((Contribution c) -> c.getOrder()))
  );

  private static final List<Filter<Contribution>> FILTERS = List.of(
    new Filter<>("none", c -> true),
    new Filter<>("cast", c -> c.getRole().getType() != Role.Type.CREW),
    new Filter<>("crew", c -> c.getRole().getType() == Role.Type.CREW)
  );

  private static final List<Filter<Contribution>> STATE_FILTERS = List.of(
    new Filter<>("none", c -> true)
  );

  @Singleton
  public static class Factory {
    @Inject private WorkClient workClient;
    @Inject private SettingsClient settingsClient;
    @Inject private BinderProvider binderProvider;

    public ContributionsPresentation create(Work work) {
      return new ContributionsPresentation(
        settingsClient,
        binderProvider,
        work,
        workClient.findContributions(work.getId())
      );
    }
  }

  protected ContributionsPresentation(SettingsClient settingsClient, BinderProvider binderProvider, Work work, List<Contribution> contributors) {
    super(settingsClient.of(SYSTEM_PREFIX + "CastAndCrew"), binderProvider, FXCollections.observableList(contributors), new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null);

    this.work = work;
  }
}
