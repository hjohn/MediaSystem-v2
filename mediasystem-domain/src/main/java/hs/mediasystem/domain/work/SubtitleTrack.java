package hs.mediasystem.domain.work;

public class SubtitleTrack {
  private final String title;
  private final String language;
  private final String codec;

  public SubtitleTrack(String title, String language, String codec) {
    this.title = title;
    this.language = language;
    this.codec = codec;
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
}
