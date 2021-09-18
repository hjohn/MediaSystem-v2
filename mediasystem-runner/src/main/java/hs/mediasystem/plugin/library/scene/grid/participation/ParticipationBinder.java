package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.plugin.cell.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.ImageHandle;

import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipationBinder implements MediaGridViewCellFactory.Binder<Participation>, IDBinder<Participation> {
  @Inject private WorkBinder workBinder;

  @Override
  public Class<Participation> getType() {
    return Participation.class;
  }

  @Override
  public Function<Participation, ObservableValue<? extends String>> titleBindProvider() {
    return c -> new SimpleStringProperty(c.getWork().getDetails().getTitle());
  }

  @Override
  public Function<Participation, ImageHandle> imageHandleExtractor() {
    return c -> c.getWork().getDetails().getAnyCover().orElse(null);
  }

  @Override
  public Function<Participation, String> detailExtractor() {
    return p -> {
      Role role = p.getRole();

      String text = role.getCharacter() != null && !role.getCharacter().isEmpty() ?
        "as " + role.getCharacter() :
        role.getJob() != null && !role.getJob().isEmpty() ? "(" + role.getJob() + ")" : "";

      if(p.getEpisodeCount() > 0) {
        text += " (" + p.getEpisodeCount() + ")";
      }

      return text;
    };
  }

  @Override
  public Function<Participation, ObservableValue<? extends String>> sideBarTopLeftBindProvider() {
    return p -> workBinder.sideBarTopLeftBindProvider().apply(p.getWork());
  }

  @Override
  public boolean watchedProperty(Participation participation) {
    return participation.getWork().getState().isConsumed();
  }

  @Override
  public boolean hasStream(Participation participation) {
    return workBinder.hasStream(participation.getWork());
  }

  @Override
  public String toId(Participation item) {
    return item.getWork().getId().toString();
  }
}
