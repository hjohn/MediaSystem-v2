package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.ui.api.SettingsClient;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

public class GenericCollectionPresentation<T> extends GridViewPresentation<T> {

  @Singleton
  public static class Factory {
    @Inject private SettingsClient settingsClient;
    @Inject private BinderProvider binderProvider;

    public <T> GenericCollectionPresentation<T> create(List<T> items, String settingPostfix, ViewOptions<T> viewOptions, Object contextItem) {
      return new GenericCollectionPresentation<>(
        settingsClient,
        binderProvider,
        settingPostfix,
        items,
        viewOptions,
        contextItem
      );
    }
  }

  protected GenericCollectionPresentation(SettingsClient settingsClient, BinderProvider binderProvider, String settingPostfix, List<T> items, ViewOptions<T> viewOptions, Object contextItem) {
    super(settingsClient.of(SYSTEM_PREFIX + "Generic:" + settingPostfix), binderProvider, items, viewOptions, contextItem);
  }
}
