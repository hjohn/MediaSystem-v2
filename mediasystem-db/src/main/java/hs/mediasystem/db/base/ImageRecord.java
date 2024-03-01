package hs.mediasystem.db.base;

import java.time.LocalDateTime;

public record ImageRecord(String url, String key, LocalDateTime creationTime, LocalDateTime accessTime, byte[] image) {

  public static ImageRecord of(String url, String key, byte[] data) {
    LocalDateTime now = LocalDateTime.now();

    return new ImageRecord(url, key, now, now, data);
  }

  public ImageRecord with(String key, LocalDateTime creationTime, byte[] image) {
    return new ImageRecord(url, key, creationTime, accessTime, image);
  }

  public ImageRecord withAccessTime(LocalDateTime accessTime) {
    return new ImageRecord(url, key, creationTime, accessTime, image);
  }
}
