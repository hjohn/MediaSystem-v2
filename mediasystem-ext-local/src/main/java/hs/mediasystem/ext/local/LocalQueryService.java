package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.StreamableStore;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalQueryService extends AbstractQueryService {
  private static final DataSource LOCAL = DataSource.instance("LOCAL");
  private static final Pattern PATTERN = Pattern.compile("([0-9]+),([0-9]+)");

  @Inject private StreamableStore streamStore;
  @Inject private DescriptionService descriptionService;

  public LocalQueryService() {
    super(LOCAL, MediaType.SERIE);
  }

  @Override
  public Serie query(WorkId id) {
    StreamID streamId = StreamID.of(id.getKey());
    Streamable streamable = streamStore.findStream(streamId).orElseThrow();
    Map<Integer, List<Episode>> episodes = new HashMap<>();

    for(Streamable childStream : streamStore.findChildren(streamId)) {
      Matcher matcher = PATTERN.matcher(childStream.getAttributes().get(Attribute.SEQUENCE, ""));

      if(matcher.matches()) {
        int seasonNumber = Integer.parseInt(matcher.group(1));
        int episodeNumber = Integer.parseInt(matcher.group(2));

        episodes.computeIfAbsent(seasonNumber, k -> new ArrayList<>()).add(new Episode(
          new WorkId(LOCAL, MediaType.EPISODE, id.getKey() + "/" + seasonNumber + "/" + episodeNumber),
          new Details(
            childStream.getAttributes().get(Attribute.TITLE),
            childStream.getAttributes().get(Attribute.SUBTITLE),
            childStream.getAttributes().get(Attribute.DESCRIPTION),
            null,
            null,
            new ImageURI("localdb://" + childStream.getId().getContentId().asInt() + "/1", null),
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
      new WorkId(LOCAL, MediaType.SEASON, id.getKey() + "/" + e.getKey()),
      new Details(
        "Season " + e.getKey(),
        null,
        null,
        null,
        null,
        null,
        null
      ),
      e.getKey(),
      e.getValue()
    )).collect(Collectors.toList());

    Optional<Description> description = descriptionService.loadDescription(streamable);
    Attributes attributes = streamable.getAttributes();

    return new Serie(
      id,
      new Details(
        description.map(Description::getTitle).orElse(attributes.get(Attribute.TITLE)),
        description.map(Description::getSubtitle).orElse(attributes.get(Attribute.SUBTITLE)),
        description.map(Description::getDescription).orElse(attributes.get(Attribute.ALTERNATIVE_TITLE)),
        description.map(Description::getDate).orElse(null),
        descriptionService.getCover(streamable).orElse(null),
        null,
        descriptionService.getBackdrop(streamable).orElse(null)
      ),
      null,
      null,
      new Classification(
        description.map(Description::getGenres).orElse(List.of()),
        Collections.emptyList(),
        Collections.emptyList(),
        Map.of(),
        null
      ),
      null,
      null,
      0,
      seasons,
      Set.of()
    );
  }
}
