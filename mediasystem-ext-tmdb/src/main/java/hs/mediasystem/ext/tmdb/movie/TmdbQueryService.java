package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.AbstractQueryService;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MovieDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class TmdbQueryService extends AbstractQueryService<MovieDescriptor> {
  @Inject private TheMovieDatabase tmdb;

  public TmdbQueryService() {
    super(DataSources.TMDB_MOVIE);
  }

  @Override
  public Result<MovieDescriptor> query(Identifier identifier) {
    JsonNode node = tmdb.query("3/movie/" + identifier.getId()); //, "append_to_response", "credits,videos");  // keywords,alternative_titles,recommendations,similar,reviews

    String releaseDate = node.path("release_date").textValue();
    Number runtime = node.path("runtime").numberValue();
    String imdbId = node.path("imdb_id").textValue();

    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original");
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original");

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    Set<Identification> newIdentifications = new HashSet<>();

    if(imdbId != null) {
      newIdentifications.add(new Identification(new Identifier(DataSources.IMDB_MOVIE, imdbId), MatchType.DERIVED, 1.0));
    }

    MovieDescriptor movieDescriptor = new MovieDescriptor(
      new Production(
        new ProductionIdentifier(DataSources.TMDB_MOVIE, identifier.getId()),
        node.get("title").textValue(),
        node.path("overview").textValue(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        posterURI,
        backdropURI,
        reception,
        runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
        node.path("spoken_languages").findValuesAsText("name"),
        node.path("genres").findValuesAsText("name")
      ),
      node.path("tagline").textValue()
    );

    return Result.of(movieDescriptor, newIdentifications);
  }
}
