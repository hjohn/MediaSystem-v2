package hs.mediasystem.domain.media;

public record VideoTrack(String title, String language, String codec, Resolution resolution, Long frameCount, Float frameRate) {
}
