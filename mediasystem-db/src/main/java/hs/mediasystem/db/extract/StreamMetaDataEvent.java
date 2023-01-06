package hs.mediasystem.db.extract;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;

public sealed interface StreamMetaDataEvent {
  ContentID id();

  public record Updated(StreamMetaData streamMetaData) implements StreamMetaDataEvent {
    @Override
    public ContentID id() {
      return streamMetaData.contentId();
    }
  }

  public record Removed(ContentID id) implements StreamMetaDataEvent {}
}
