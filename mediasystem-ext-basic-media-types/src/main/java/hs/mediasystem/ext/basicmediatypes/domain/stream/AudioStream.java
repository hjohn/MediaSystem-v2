package hs.mediasystem.ext.basicmediatypes.domain.stream;

public class AudioStream {
  private final String title;
  private final String language;
  private final String codec;
  private final long channelLayout;

  public AudioStream(String title, String language, String codec, long channelLayout) {
    this.title = title;
    this.language = language;
    this.codec = codec;
    this.channelLayout = channelLayout;
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

  public long getChannelLayout() {
    return channelLayout;
  }
}
