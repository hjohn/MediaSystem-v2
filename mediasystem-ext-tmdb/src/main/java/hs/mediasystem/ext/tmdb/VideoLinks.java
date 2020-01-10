package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.VideoLink.Type;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class VideoLinks {

  public List<VideoLink> toVideoLinks(JsonNode videos) {
    List<VideoLink> list = new ArrayList<>();

    for(JsonNode result : videos.path("results")) {
      list.add(new VideoLink(
        toType(result.path("type").textValue()),
        result.path("name").textValue(),
        result.path("site").textValue(),
        result.path("key").textValue(),
        result.path("size").intValue()  // 1080
      ));
    }

    return list;
  }

  private static Type toType(String type) {
    try {
      return Type.valueOf(type.toUpperCase());
    }
    catch(IllegalArgumentException e) {
      return null;
    }
  }
}
