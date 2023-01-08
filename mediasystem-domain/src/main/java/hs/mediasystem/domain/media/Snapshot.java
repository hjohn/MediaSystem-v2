package hs.mediasystem.domain.media;

import hs.mediasystem.util.image.ImageURI;

public record Snapshot(ImageURI imageUri, int frameNumber) {
}
