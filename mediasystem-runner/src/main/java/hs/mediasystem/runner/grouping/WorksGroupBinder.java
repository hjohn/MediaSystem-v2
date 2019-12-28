package hs.mediasystem.runner.grouping;

import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory.Binder;
import hs.mediasystem.plugin.library.scene.grid.IDBinder;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;

import java.time.LocalDate;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorksGroupBinder implements Binder<WorksGroup>, IDBinder<WorksGroup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public Class<WorksGroup> getType() {
    return WorksGroup.class;
  }

  @Override
  public Function<WorksGroup, ObservableValue<? extends String>> titleBindProvider() {
    return rg -> new SimpleStringProperty(rg.getDetails().getName());
  }

  @Override
  public Function<WorksGroup, ImageHandle> imageHandleExtractor() {
    return rg -> rg.getDetails().getImage().map(imageHandleFactory::fromURI).orElse(null);
  }

  @Override
  public Function<WorksGroup, ObservableValue<? extends String>> sideBarTopLeftBindProvider() {
    return rg -> rg.getDetails().getDate().map(LocalDate::getYear).map(Object::toString).map(SimpleStringProperty::new).orElse(null);
  }

  @Override
  public String toId(WorksGroup item) {
    return item.getId().toString();
  }

}
