package hs.mediasystem.domain.work;

import hs.mediasystem.util.ImageURI;

import java.util.Optional;

public record Collection(String title, Optional<ImageURI> cover, Optional<ImageURI> backdrop, CollectionDefinition definition) {
}
