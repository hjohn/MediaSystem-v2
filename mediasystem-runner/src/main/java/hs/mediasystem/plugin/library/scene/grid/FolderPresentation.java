package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.Work;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

public class FolderPresentation extends GridViewPresentation<Work> {
  public final String settingPostfix;
  public final ViewOptions<Work> viewOptions;

  @Singleton
  public static class Factory {
    @Inject private SettingsClient settingsClient;
    @Inject private BinderProvider binderProvider;

    public FolderPresentation create(List<Work> items, String settingPostfix, ViewOptions<Work> viewOptions, Object contextItem) {
      return new FolderPresentation(
        settingsClient,
        binderProvider,
        settingPostfix,
        items,
        viewOptions,
        contextItem
      );
    }
  }

  protected FolderPresentation(SettingsClient settingsClient, BinderProvider binderProvider, String settingPostfix, List<Work> items, ViewOptions<Work> viewOptions, Object contextItem) {
    super(settingsClient.of(SYSTEM_PREFIX + "Folder:" + settingPostfix), binderProvider, items, viewOptions, contextItem);

    this.settingPostfix = settingPostfix;
    this.viewOptions = viewOptions;
  }
}
