package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

@Singleton
public class LocalEpisodeIdentificationService extends AbstractIdentificationService {
  private static final DataSource EPISODE = DataSource.instance(MediaType.of("EPISODE"), "LOCAL");
  private static final Pattern PATTERN = Pattern.compile("([0-9]+),([0-9]+)");

  public LocalEpisodeIdentificationService() {
    super(EPISODE);
  }

  @Override
  public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
    return createEpisodeIdentifier(streamable, parent.getIdentifier().getId())
      .map(i -> new Identification(List.of(i), new Match(Type.DERIVED, 1.0f, Instant.now())));
  }

  private static Optional<EpisodeIdentifier> createEpisodeIdentifier(Streamable streamable, String parentId) {
    Matcher matcher = PATTERN.matcher(streamable.getAttributes().get(Attribute.SEQUENCE, ""));

    if(matcher.matches()) {
      int seasonNumber = Integer.parseInt(matcher.group(1));
      int episodeNumber = Integer.parseInt(matcher.group(2));

      return Optional.of(new EpisodeIdentifier(EPISODE, parentId + "/" + seasonNumber + "/" + episodeNumber));
    }

    return Optional.empty();
  }
}
