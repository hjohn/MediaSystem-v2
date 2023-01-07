package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.util.Localizable;
import hs.mediasystem.util.exception.Throwables;

import java.nio.file.Path;

public class VideoUnavailableException extends RuntimeException implements Localizable {
  private final Path path;

  public VideoUnavailableException(Throwable cause, Path path) {
    super(cause);

    this.path = path;
  }

  @Override
  public String toLocalizedString() {
    return "### Unable to play video\n"
        + "MediaSystem was unable to start playing the selected content because the "
        + "file is unavailable or cannot be accessed.\n\n"
        + "#### Solution\n"
        + "Please check the file at this location:\n\n"
        + "`" + path + "`\n"
        + "#### Technical details\n"
        + "```\n"
        + Throwables.toString(getCause())
        + "```\n";
  }
}
