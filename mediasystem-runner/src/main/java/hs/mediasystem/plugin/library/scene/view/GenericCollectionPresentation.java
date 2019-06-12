package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;

import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class GenericCollectionPresentation<T extends MediaDescriptor> extends GridViewPresentation<T> {
  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private SettingsSourceFactory settingsSourceFactory;

    public <T extends MediaDescriptor> GenericCollectionPresentation<T> create(ObservableList<MediaItem<T>> items, String settingPostFix, ViewOptions<T> viewOptions) {
      return new GenericCollectionPresentation<>(settingsSourceFactory.of(SYSTEM_PREFIX + "Generic:" + settingPostFix), mediaService, items, viewOptions);
    }
  }

  protected GenericCollectionPresentation(SettingsSource settingsSource, MediaService mediaService, ObservableList<MediaItem<T>> items, ViewOptions<T> viewOptions) {
    super(settingsSource, mediaService, items, viewOptions);
  }
}
