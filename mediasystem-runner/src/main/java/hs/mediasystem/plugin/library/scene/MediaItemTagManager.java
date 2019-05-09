package hs.mediasystem.plugin.library.scene;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaItemTagManager {
  @Inject private TagStore tagStore;

  // mi = ...
  // x = tagManager.get(mi, ViewedTag.INSTANCE);
  // x = tagManager.of(mi).tag(Tags.VIEWED);
  // x.get();
  // x.set(true);

  // TODO remove
  public static void main(String[] args) {
    MediaItemTagManager mediaItemTagManager = new MediaItemTagManager();

    mediaItemTagManager.of(null).tag(Tags.WATCHED).set(true);
    boolean b = mediaItemTagManager.of(null).tag(Tags.WATCHED).get();
  }

  public MediaItemTagger of(MediaItem<?> mediaItem) {
    return new MediaItemTagger(mediaItem);
  }

  class MediaItemTagger {
    private final MediaItem<?> mediaItem;

    MediaItemTagger(MediaItem<?> mediaItem) {
      this.mediaItem = mediaItem;
    }

    public <T> TagInstance<T> tag(Tag<T> tag) {
      return new TagInstance<>() {
        @Override
        public T get() {
          String physicalId = mediaItem.getPhysicalId();
          String logicalId = mediaItem.getLogicalId();
          T result = null;

          if(physicalId != null) {
            result = tagStore.get(physicalId, tag);
          }
          if(result == null && logicalId != null) {
            result = tagStore.get(logicalId, tag);
          }

          return result;
        }

        @Override
        public void set(T value) {
          String physicalId = mediaItem.getPhysicalId();
          String logicalId = mediaItem.getLogicalId();

          if(physicalId != null) {
            tagStore.put(physicalId, tag, value);
          }
          if(logicalId != null) {
            tagStore.put(logicalId, tag, value);
          }
        }
      };
    }
  }
}
