package hs.mediasystem.db.contentprints;

import java.time.Instant;

public record ContentPrintRecord(
  Integer id,
  byte[] hash,
  Long size,
  long lastModificationTime,  // in milliseconds since epoch
  Long lastSeenTime,  // in milliseconds since epoch, if null was seen recently
  long creationMillis  // signature creation millis
) {
  public ContentPrintRecord with(byte[] hash, Long size, Instant lastModificationTime) {
    return new ContentPrintRecord(id, hash, size, lastModificationTime.toEpochMilli(), lastSeenTime, creationMillis);
  }

  public ContentPrintRecord withId(int id) {
    return new ContentPrintRecord(id, hash, size, lastModificationTime, lastSeenTime, creationMillis);
  }
}