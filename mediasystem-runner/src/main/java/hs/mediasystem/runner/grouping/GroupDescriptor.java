package hs.mediasystem.runner.grouping;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;

public class GroupDescriptor implements MediaDescriptor {
  private final Identifier identifier;
  private final Details details;

  public GroupDescriptor(Identifier identifier, Details details) {
    this.identifier = identifier;
    this.details = details;
  }

  @Override
  public Identifier getIdentifier() {
    return identifier;
  }

  @Override
  public Details getDetails() {
    return details;
  }
}