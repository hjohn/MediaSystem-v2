package hs.mediasystem.db.core;

import hs.mediasystem.api.discovery.Discoverer;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a source of media to be imported.  This includes how this source
 * should be discovered, and after discovery, how each items should be tagged and
 * (optionally) identified.
 *
 * @param discoverer a {@link Discoverer}, cannot be {@code null}
 * @param root a {@link URI}, cannot be {@code null}
 * @param identificationService an optional name of an identification service to use, cannot be {@code null}
 * @param tags a {@link StreamTags}, cannot be {@code null}
 */
public record ImportSource(Discoverer discoverer, URI root, Optional<String> identificationService, StreamTags tags) {

  public ImportSource {
    Objects.requireNonNull(discoverer, "discoverer");
    Objects.requireNonNull(root, "root").resolve("");
    Objects.requireNonNull(tags, "tags");
  }

  @Override
  public String toString() {
    return "ImportSource[" + discoverer.getClass().getSimpleName() + " @ " + root + " [" + tags + "]]";
  }
}
