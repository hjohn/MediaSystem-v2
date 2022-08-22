package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.ext.tmdb.VideoLinks;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class TmdbVideoLinksQueryService implements VideoLinksQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private VideoLinks videoLinks;

  @Override
  public List<VideoLink> query(WorkId id) throws IOException {
    JsonNode info = tmdb.query(idToLocation(id), "text:json:" + id);

    return videoLinks.toVideoLinks(info);
  }

  private static String idToLocation(WorkId id) {
    if(id.getType() == MediaType.MOVIE) {
      return "3/movie/" + id.getKey() + "/videos";
    }
    if(id.getType() == MediaType.SERIE) {
      return "3/tv/" + id.getKey() + "/videos";
    }
    if(id.getType() == MediaType.EPISODE) {
      String[] parts = id.getKey().split("/");

      return "3/tv/" + parts[0] + "/season/" + parts[1] + "/episode/" + parts[2] + "/videos";
    }

    throw new IllegalArgumentException("Unsupported type: " + id);
  }
}
