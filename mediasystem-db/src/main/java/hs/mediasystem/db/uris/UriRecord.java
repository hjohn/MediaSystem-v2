package hs.mediasystem.db.uris;

public record UriRecord(Integer id, int contentId, String uri) {

  public UriRecord with(int contentId) {
    return new UriRecord(id, contentId, uri);
  }

}

