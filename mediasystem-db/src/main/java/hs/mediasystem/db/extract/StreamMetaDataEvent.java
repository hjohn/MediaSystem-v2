package hs.mediasystem.db.extract;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;

public interface StreamMetaDataEvent {
  public record Updated(StreamMetaData streamMetaData) implements StreamMetaDataEvent {}
  public record Removed(ContentID id) implements StreamMetaDataEvent {}
}
