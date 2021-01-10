package hs.mediasystem.plugin.library.scene.grid;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Singleton;

@Singleton
public class GenericCollectionPresentationFactory extends GridViewPresentationFactory {

  public <T, U> GenericCollectionPresentation<T, U> create(Supplier<List<T>> itemsSupplier, String settingPostfix, ViewOptions<T, U> viewOptions, Supplier<Object> contextItemSupplier) {
    return new GenericCollectionPresentation<>(settingPostfix, itemsSupplier, viewOptions, contextItemSupplier);
  }

  public class GenericCollectionPresentation<T, U> extends GridViewPresentation<T, U> {
    private final Supplier<List<T>> itemsSupplier;
    private final Supplier<Object> contextItemSupplier;

    public GenericCollectionPresentation(String settingPostfix, Supplier<List<T>> itemsSupplier, ViewOptions<T, U> viewOptions, Supplier<Object> contextItemSupplier) {
      super("Generic:" + settingPostfix, viewOptions);

      this.itemsSupplier = itemsSupplier;
      this.contextItemSupplier = contextItemSupplier;

      createUpdateTask().run();
    }

    @Override
    public Runnable createUpdateTask() {
      Object contextItem = contextItemSupplier == null ? null : contextItemSupplier.get();
      List<T> items = itemsSupplier.get();

      return () -> {
        rootContextItem.set(contextItem);
        inputItems.set(items);
      };
    }
  }
}
