package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.ui.api.domain.Work;

import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContributionsPresentationFactory extends GridViewPresentationFactory {

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

  @Inject private WorkClient workClient;

  public ContributionsPresentation create(Work work) {
    return new ContributionsPresentation(work);
  }

  public class ContributionsPresentation extends GridViewPresentation<Contribution> {
    public final Work work;

    public ContributionsPresentation(Work work) {
      super("CastAndCrew", FXCollections.observableList(workClient.findContributions(work.getId())), new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null);

      this.work = work;
    }
  }
}
