package hs.mediasystem.ext.tmdb.provider;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.CollectionDetails;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.datasource.domain.ProductionCollection;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

public class CollectionProvider implements MediaProvider<ProductionCollection> {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public Optional<ProductionCollection> provide(String key) throws IOException {
    return tmdb.query("3/collection/" + key, "text:json:tmdb:collection:" + key)
      .map(node -> toProductionCollection(node));
  }

  private ProductionCollection toProductionCollection(JsonNode node) {
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
