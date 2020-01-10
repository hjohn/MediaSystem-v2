package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;

public class EpisodeIdentifier extends ProductionIdentifier {

  public EpisodeIdentifier(DataSource dataSource, String id) {
    super(dataSource, id);

    if(!id.contains("/")) {
      throw new IllegalArgumentException("id must be of the format <parent-id>/<child-id>: " + id);
    }
  }

  @Override
  public Identifier getRootIdentifier() {
    return new Identifier(DataSource.instance(MediaType.of("SERIE"), getDataSource().getName()), getId().substring(0, getId().indexOf("/")));
  }
}
