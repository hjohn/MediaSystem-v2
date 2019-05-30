package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbCollectionQueryService extends AbstractQueryService<IdentifierCollection> {
  @Inject private TheMovieDatabase tmdb;

  public TmdbCollectionQueryService() {
    super(DataSources.TMDB_COLLECTION);
  }

  @Override
  public Result<IdentifierCollection> query(Identifier identifier) {
    JsonNode node = tmdb.query("3/collection/" + identifier.getId());
    List<Identifier> productions = new ArrayList<>();

    node.path("parts").forEach(p -> productions.add(
      new ProductionIdentifier(DataSources.TMDB_MOVIE, p.path("id").asText())
    ));

    return Result.of(new IdentifierCollection(
      new Identifier(DataSources.TMDB_CHRONOLOGY, node.path("id").asText()),
      node.path("name").asText(),
      node.path("overview").asText(),
      tmdb.createImageURI(node.path("poster_path").textValue(), "original"),
      tmdb.createImageURI(node.path("backdrop_path").textValue(), "original"),
      productions
    ));
  }
}
