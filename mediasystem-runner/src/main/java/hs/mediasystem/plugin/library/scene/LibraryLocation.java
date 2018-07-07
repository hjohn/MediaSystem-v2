package hs.mediasystem.plugin.library.scene;

import java.util.HashMap;
import java.util.Map;

public abstract class LibraryLocation {
  private final Object item;
  private final Object path;

  private final Map<String, Object> status;

  // null, "movies" -> Movies root
  // MovieEntity, null -> specific movie
  // MovieEntity, "castings", -> casting list
  // ActorEntity, null -> specific actor
  // ActorEntity, "appearances" -> appearances list
  // null, "series" -> Series root
  // SerieProductionItem, null -> specific serie
  // SerieProductionItem, "children" -> episodes or seasons list
  // EpisodeProductionItem, null -> specific episode
  // EpisodeProductionItem, "cast&crew" -> cast of episode

  public LibraryLocation(Object item, Object path, Map<String, Object> status) {
    this.item = item;
    this.path = path;
    this.status = status;
  }

  public LibraryLocation(Object item, Object path) {
    this(item, path, new HashMap<>());
  }

  public Object getItem() {
    return item;
  }

  public Object getPath() {
    return path;
  }

  public <T> T getStatus(String key) {
    return (T)status.get(key);
  }

  public void putStatus(String key, Object data) {
    status.put(key, data);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[item=" + item + "; path=" + path + "]";
  }
}
