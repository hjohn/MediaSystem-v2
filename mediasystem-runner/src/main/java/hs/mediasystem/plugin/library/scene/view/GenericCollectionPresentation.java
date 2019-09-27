package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

public class GenericCollectionPresentation<T extends MediaDescriptor> extends GridViewPresentation<T> {

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private SettingsSourceFactory settingsSourceFactory;
    @Inject private MediaItem.Factory factory;

    public <T extends MediaDescriptor> GenericCollectionPresentation<T> create(List<MediaItem<T>> items, String settingPostFix, ViewOptions<T> viewOptions, MediaDescriptor parentDescriptor) {
      return new GenericCollectionPresentation<>(settingsSourceFactory.of(SYSTEM_PREFIX + "Generic:" + settingPostFix), mediaService, items, viewOptions, parentDescriptor == null ? null : factory.create(parentDescriptor, null));
    }
  }

  protected GenericCollectionPresentation(SettingsSource settingsSource, MediaService mediaService, List<MediaItem<T>> items, ViewOptions<T> viewOptions, MediaItem<MediaDescriptor> contextItem) {
    super(settingsSource, mediaService, items, viewOptions, contextItem);
  }
}
