package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.plugin.cell.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.ImageHandle;

import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Singleton;

@Singleton
public class ContributionBinder implements MediaGridViewCellFactory.Binder<Contribution>, IDBinder<Contribution> {

  @Override
  public Class<Contribution> getType() {
    return Contribution.class;
  }

  @Override
  public Function<Contribution, ObservableValue<? extends String>> titleBindProvider() {
    return c -> new SimpleStringProperty(c.getPerson().getName());
  }

  @Override
  public Function<Contribution, ImageHandle> imageHandleExtractor() {
    return c -> c.getPerson().getCover().orElse(null);
  }

  @Override
  public Function<Contribution, String> detailExtractor() {
    return c -> {
      Role role = c.getRole();

      return role.getCharacter() != null && !role.getCharacter().isEmpty() ? "as " + role.getCharacter() :
        role.getJob() != null && !role.getJob().isEmpty() ? "(" + role.getJob() + ")" : "";
    };
  }

  @Override
  public String toId(Contribution item) {
    return item.getPerson().getId().toString();
  }
}
