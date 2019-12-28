package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;

import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContributionBinder implements MediaGridViewCellFactory.Binder<Contribution>, IDBinder<Contribution> {
  @Inject private ImageHandleFactory imageHandleFactory;

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
    return c -> Optional.ofNullable(c.getPerson().getImage()).map(imageHandleFactory::fromURI).orElse(null);
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
    return item.getPerson().getIdentifier().toString();
  }
}
