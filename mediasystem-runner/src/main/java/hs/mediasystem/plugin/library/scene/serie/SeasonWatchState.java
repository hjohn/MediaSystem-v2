package hs.mediasystem.plugin.library.scene.serie;

public class SeasonWatchState {
  public final int totalEpisodes;
  public final int missingEpisodes;
  public final int watchedEpisodes;

  public SeasonWatchState(int totalEpisodes, int missingEpisodes, int watchedEpisodes) {
    this.totalEpisodes = totalEpisodes;
    this.missingEpisodes = missingEpisodes;
    this.watchedEpisodes = watchedEpisodes;
  }

}
