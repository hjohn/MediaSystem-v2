package hs.mediasystem.domain.work;

import hs.mediasystem.util.image.ImageURI;

public record Snapshot(ImageURI imageUri, int frameNumber) {
}
