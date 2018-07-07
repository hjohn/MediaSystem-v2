package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Map;

public interface MediaStream<D extends MediaDescriptor> {
  Type getType();
  StringURI getParentUri();
  StringURI getUri();
  Attributes getAttributes();
  StreamPrint getStreamPrint();

  /**
   * Returns a read-only copy of the {@link MediaRecord}s.
   *
   * @return a read-only copy of the {@link MediaRecord}s
   */
  Map<Identifier, MediaRecord<D>> getMediaRecords();

  /**
   * Merges the given {@link Identifier} and {@link MediaRecord} with
   * the records in this object.
   *
   * @param mediaRecord a {@link MediaRecord}
   */
  void mergeMediaRecord(MediaRecord<D> mediaRecord);
}
