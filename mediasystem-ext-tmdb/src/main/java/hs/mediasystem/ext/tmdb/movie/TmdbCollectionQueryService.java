package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.WorkIdCollection;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbCollectionQueryService extends AbstractQueryService {
  @Inject private TheMovieDatabase tmdb;

  public TmdbCollectionQueryService() {
    super(DataSources.TMDB, MediaType.COLLECTION);
  }

  @Override
  public WorkIdCollection query(WorkId id) throws IOException {
    JsonNode node = tmdb.query("3/collection/" + id.getKey(), "text:json:" + id);
    List<WorkId> items = new ArrayList<>();

    node.path("parts").forEach(p -> items.add(
      new WorkId(DataSources.TMDB, MediaType.MOVIE, p.path("id").asText())
    ));

    WorkId collectionId = new WorkId(DataSources.TMDB, MediaType.COLLECTION, node.path("id").asText());

    return new WorkIdCollection(
      new CollectionDetails(
        collectionId,
        new Details(
          node.path("name").asText(),
          null,
          node.path("overview").asText(),
          null,
          tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + collectionId),  // as cover
          null,
          tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + collectionId)
        )
      ),
      items
    );
  }
}
