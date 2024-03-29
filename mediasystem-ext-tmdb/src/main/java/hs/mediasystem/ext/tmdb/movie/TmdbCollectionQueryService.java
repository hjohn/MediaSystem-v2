package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@PluginScoped
public class TmdbCollectionQueryService extends AbstractQueryService {
  @Inject private TheMovieDatabase tmdb;

  public TmdbCollectionQueryService() {
    super(DataSources.TMDB_COLLECTION);
  }

  @Override
  public IdentifierCollection query(Identifier identifier) throws IOException {
    JsonNode node = tmdb.query("3/collection/" + identifier.getId(), "text:json:" + identifier);
    List<Identifier> items = new ArrayList<>();

    node.path("parts").forEach(p -> items.add(
      new ProductionIdentifier(DataSources.TMDB_MOVIE, p.path("id").asText())
    ));

    Identifier identifierCollectionIdentifier = new Identifier(DataSources.TMDB_COLLECTION, node.path("id").asText());

    return new IdentifierCollection(
      new CollectionDetails(
        identifierCollectionIdentifier,
        new Details(
          node.path("name").asText(),
          null,
          node.path("overview").asText(),
          null,
          tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + identifierCollectionIdentifier),  // as cover
          null,
          tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + identifierCollectionIdentifier)
        )
      ),
      items
    );
  }
}
