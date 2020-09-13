package hs.mediasystem.plugin.library.scene.grid;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class GenericCollectionPresentationFactory extends GridViewPresentationFactory {

  public <T> GenericCollectionPresentation<T> create(List<T> items, String settingPostfix, ViewOptions<T> viewOptions, Object contextItem) {
    return new GenericCollectionPresentation<>(settingPostfix, items, viewOptions, contextItem);
  }

  public class GenericCollectionPresentation<T> extends GridViewPresentation<T> {
    public GenericCollectionPresentation(String settingPostfix, List<T> items, ViewOptions<T> viewOptions, Object contextItem) {
      super("Generic:" + settingPostfix, items, viewOptions, contextItem);
    }
  }
}
