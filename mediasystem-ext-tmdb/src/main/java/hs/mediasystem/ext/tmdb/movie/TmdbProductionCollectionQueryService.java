package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbProductionCollectionQueryService extends AbstractQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  public TmdbProductionCollectionQueryService() {
    super(DataSources.TMDB, MediaType.COLLECTION);
  }

  @Override
  public ProductionCollection query(WorkId id) throws IOException {
    JsonNode node = tmdb.query("3/collection/" + id.getKey(), "text:json:" + id);
    List<Production> productions = new ArrayList<>();

    for(JsonNode p : node.path("parts")) {
      productions.add(objectFactory.toProduction(p, DataSources.TMDB, MediaType.MOVIE));
    }

    WorkId collectionId = new WorkId(DataSources.TMDB, MediaType.COLLECTION, node.path("id").asText());

    return new ProductionCollection(
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
      productions
    );
  }
}
