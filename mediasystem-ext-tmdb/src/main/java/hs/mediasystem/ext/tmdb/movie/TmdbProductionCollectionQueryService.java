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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbProductionCollectionQueryService implements ProductionCollectionQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public ProductionCollection query(Identifier identifier) {
    JsonNode node = tmdb.query("3/collection/" + identifier.getId());
    List<Production> productions = new ArrayList<>();

    node.path("parts").forEach(p -> productions.add(objectFactory.toProduction(p, DataSources.TMDB_MOVIE)));

    return new ProductionCollection(
      new CollectionDetails(
        new Identifier(DataSources.TMDB_COLLECTION, node.path("id").asText()),
        new Details(
          node.path("name").asText(),
          node.path("overview").asText(),
          null,
          tmdb.createImageURI(node.path("poster_path").textValue(), "original"),
          tmdb.createImageURI(node.path("backdrop_path").textValue(), "original")
        )
      ),
      productions
    );
  }
}
