package hs.mediasystem.domain.work;

import hs.mediasystem.util.ImageURI;

public record Snapshot(ImageURI imageUri, int frameNumber) {
}
