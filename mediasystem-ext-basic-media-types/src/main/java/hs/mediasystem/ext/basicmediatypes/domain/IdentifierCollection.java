package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.util.ImageURI;

import java.util.List;

public class IdentifierCollection extends AbstractCollection<Identifier> {

  public IdentifierCollection(Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Identifier> identifiers) {
    super(identifier, name, overview, image, backdrop, identifiers);
  }

}
