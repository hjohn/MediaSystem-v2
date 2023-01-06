package hs.mediasystem.domain.work;

import hs.mediasystem.util.ImageURI;

import java.util.Optional;

// TODO Not to be confused with TMDB collections; perhaps rename to library
public record Collection(String title, Optional<ImageURI> cover, Optional<ImageURI> backdrop, CollectionDefinition definition) {
}
