package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.Production;

import java.util.Collections;
import java.util.Set;

public class ProductionItem {
  private final Production production;
  private final Set<MediaStream<?>> mediaStreams;

  public ProductionItem(Production production, Set<MediaStream<?>> mediaStreams) {
    if(production == null) {
      throw new IllegalArgumentException("production cannot be null");
    }
    if(mediaStreams == null) {
      throw new IllegalArgumentException("mediaStreams cannot be null");
    }

    this.production = production;
    this.mediaStreams = mediaStreams;
  }

  public ProductionItem(Production production) {
    this(production, Collections.emptySet());
  }

  public Production getProduction() {
    return production;
  }

  public Set<MediaStream<?>> getMediaStreams() {
    return mediaStreams;
  }
}
