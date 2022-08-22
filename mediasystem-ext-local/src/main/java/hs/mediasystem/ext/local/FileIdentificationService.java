package hs.mediasystem.ext.local;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

@Singleton
public class FileIdentificationService extends AbstractIdentificationService {
  private static final DataSource LOCAL = DataSource.instance("LOCAL");

  public FileIdentificationService() {
    super(LOCAL, MediaType.FILE);
  }

  @Override
  public Optional<Identification> identify(Streamable streamable, WorkDescriptor parent) {
    return Optional.of(new Identification(List.of(new WorkId(LOCAL, MediaType.FILE, streamable.getId().asString())), new Match(Type.MANUAL, 1.0f, Instant.now())));
  }
}
