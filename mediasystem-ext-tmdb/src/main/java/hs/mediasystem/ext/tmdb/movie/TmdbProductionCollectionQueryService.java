package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.services.ProductionCollectionQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbProductionCollectionQueryService implements ProductionCollectionQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public ProductionCollection query(Identifier identifier) throws IOException {
    JsonNode node = tmdb.query("3/collection/" + identifier.getId(), "text:json:" + identifier);
    List<Production> productions = new ArrayList<>();

    for(JsonNode p : node.path("parts")) {
      productions.add(objectFactory.toProduction(p, DataSources.TMDB_MOVIE));
    }

    Identifier productionCollectionIdentifier = new Identifier(DataSources.TMDB_COLLECTION, node.path("id").asText());

    return new ProductionCollection(
      new CollectionDetails(
        productionCollectionIdentifier,
        new Details(
          node.path("name").asText(),
          null,
          node.path("overview").asText(),
          null,
          tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + productionCollectionIdentifier),  // as cover
          null,
          tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + productionCollectionIdentifier)
        )
      ),
      productions
    );
  }
}
