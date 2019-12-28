package hs.mediasystem.plugin.library.scene.grid;

import java.util.List;

import javax.inject.Singleton;

public class GenericCollectionPresentation<T> extends GridViewPresentation<T> {
  public final String settingPostfix;

  @Singleton
  public static class Factory {
    public <T> GenericCollectionPresentation<T> create(List<T> items, String settingPostfix, ViewOptions<T> viewOptions, Object contextItem) {
      return new GenericCollectionPresentation<>(settingPostfix, items, viewOptions, contextItem);
    }
  }

  protected GenericCollectionPresentation(String settingPostfix, List<T> items, ViewOptions<T> viewOptions, Object contextItem) {
    super(items, viewOptions, contextItem);

    this.settingPostfix = settingPostfix;
  }
}
