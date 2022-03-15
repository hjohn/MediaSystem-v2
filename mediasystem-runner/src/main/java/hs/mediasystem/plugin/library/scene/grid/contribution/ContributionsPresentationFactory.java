package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.WorkNotFoundException;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.ui.api.domain.Work;

import java.util.Comparator;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

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

  public ContributionsPresentation create(WorkId id) {
    return new ContributionsPresentation(id);
  }

  public class ContributionsPresentation extends GridViewPresentation<Contribution, Contribution> {
    public final ObjectProperty<Work> work = new SimpleObjectProperty<>();

    private final WorkId id;

    public ContributionsPresentation(WorkId id) {
      super("CastAndCrew", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), c -> c.getPerson().getId());

      this.id = id;

      createUpdateTask().run();
    }

    @Override
    public Runnable createUpdateTask() {
      Work work = workClient.find(id).orElseThrow(() -> new WorkNotFoundException(id));
      List<Contribution> contributions = workClient.findContributions(id);

      return () -> {
        this.work.set(work);
        this.inputItems.set(contributions);
      };
    }
  }
}
