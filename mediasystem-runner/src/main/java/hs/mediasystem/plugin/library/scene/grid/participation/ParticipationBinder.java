package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Participation;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;

import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipationBinder implements MediaGridViewCellFactory.Binder<Participation>, IDBinder<Participation> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private WorkBinder workBinder;

  @Override
  public Class<Participation> getType() {
    return Participation.class;
  }

  @Override
  public Function<Participation, ObservableValue<? extends String>> titleBindProvider() {
    return c -> new SimpleStringProperty(c.getWork().getDetails().getName());
  }

  @Override
  public Function<Participation, ImageHandle> imageHandleExtractor() {
    return c -> c.getWork().getDetails().getImage().map(imageHandleFactory::fromURI).orElse(null);
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
  public Optional<BooleanProperty> watchedProperty(Participation participation) {
    return workBinder.watchedProperty(participation.getWork());
  }

  @Override
  public Optional<Boolean> hasStream(Participation participation) {
    return workBinder.hasStream(participation.getWork());
  }

  @Override
  public String toId(Participation item) {
    return item.getWork().getId().toString();
  }
}
