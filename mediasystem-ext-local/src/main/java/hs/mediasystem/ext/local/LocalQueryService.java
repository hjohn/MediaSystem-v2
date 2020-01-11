package hs.mediasystem.ext.local;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.BasicStreamStore;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalQueryService extends AbstractQueryService {
  private static final Logger LOGGER = Logger.getLogger(LocalQueryService.class.getName());
  private static final DataSource SERIE = DataSource.instance(MediaType.of("SERIE"), "LOCAL");
  private static final DataSource SEASON = DataSource.instance(MediaType.of("SEASON"), "LOCAL");
  private static final DataSource EPISODE = DataSource.instance(MediaType.of("EPISODE"), "LOCAL");
  private static final Pattern PATTERN = Pattern.compile("([0-9]+),([0-9]+)");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new JavaTimeModule())
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES));

  @Inject private BasicStreamStore streamStore;

  public LocalQueryService() {
    super(SERIE);
  }

  @Override
  public Serie query(Identifier identifier) {
    StreamID streamId = new StreamID(Integer.parseInt(identifier.getId()));
    BasicStream stream = streamStore.findStream(streamId).orElseThrow();
    Map<Integer, List<Episode>> episodes = new HashMap<>();

    for(BasicStream childStream : stream.getChildren()) {
      Matcher matcher = PATTERN.matcher(childStream.getAttributes().get(Attribute.SEQUENCE, ""));

      if(matcher.matches()) {
        int seasonNumber = Integer.parseInt(matcher.group(1));
        int episodeNumber = Integer.parseInt(matcher.group(2));

        episodes.computeIfAbsent(seasonNumber, k -> new ArrayList<>()).add(new Episode(
          new EpisodeIdentifier(EPISODE, identifier.getId() + "/" + seasonNumber + "/" + episodeNumber),
          new Details(
            childStream.getAttributes().get(Attribute.TITLE),
            null,
            null,
            new ImageURI("localdb://" + childStream.getId().asInt() + "/1"),
            null
          ),
          null,
          null,
          seasonNumber,
          episodeNumber,
          Collections.emptyList()
        ));
      }
    }

    List<Season> seasons = episodes.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(e -> new Season(
      new ProductionIdentifier(SEASON, identifier.getId() + "/" + e.getKey()),
      new Details(
        "Season " + e.getKey(),
        null,
        null,
        null,
        null
      ),
      e.getKey(),
      e.getValue()
    )).collect(Collectors.toList());

    Description description = loadDescription(stream);

    return new Serie(
      (ProductionIdentifier)identifier,
      new Details(
        description == null || description.getTitle() == null ? stream.getAttributes().get(Attribute.TITLE) : description.getTitle(),
        description == null ? null : description.getDescription(),
        description == null ? null : description.getDate(),
        new ImageURI(stream.getUri().toString() + "/cover.jpg"),
        new ImageURI(stream.getUri().toString() + "/backdrop.jpg")
      ),
      null,
      Collections.emptyList(),
      description == null ? Collections.emptyList() : description.getGenres(),
      Collections.emptyList(),
      null,
      null,
      0,
      seasons,
      Set.of()
    );
  }

  private static Description loadDescription(BasicStream stream) {
    String urlText = stream.getUri().toString() + "/description.yaml";

    try {
      return OBJECT_MAPPER.readValue(new URL(urlText), Description.class);
    }
    catch(ConnectException e) {
      // ignore, file just doesn't exist
      return null;
    }
    catch(IOException e) {
      LOGGER.warning("Exception while parsing " + urlText + ": " + Throwables.formatAsOneLine(e));
      return null;
    }
  }
}
