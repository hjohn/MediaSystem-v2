package hs.mediasystem.domain.work;

public class VideoTrack {
  private final String title;
  private final String language;
  private final String codec;
  private final Resolution resolution;
  private final Long frameCount;
  private final Float frameRate;

  public VideoTrack(String title, String language, String codec, Resolution resolution, Long frameCount, Float frameRate) {
    this.title = title;
    this.language = language;
    this.codec = codec;
    this.resolution = resolution;
    this.frameCount = frameCount;
    this.frameRate = frameRate;
  }

  public String getTitle() {
    return title;
  }

  public String getLanguage() {
    return language;
  }

  public String getCodec() {
    return codec;
  }

  public Resolution getResolution() {
    return resolution;
  }

  public Long getFrameCount() {
    return frameCount;
  }

  public Float getFrameRate() {
    return frameRate;
  }
}
