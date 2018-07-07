package hs.mediasystem.domain;

public class AudioTrack {
  public static final AudioTrack NO_AUDIO_TRACK = new AudioTrack(-1, "Unavailable");

  private final int id;
  private final String description;

  public AudioTrack(int id, String description) {
    this.id = id;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "('" + description + "', AudioTrack[id=" + id + "])";
  }
}
