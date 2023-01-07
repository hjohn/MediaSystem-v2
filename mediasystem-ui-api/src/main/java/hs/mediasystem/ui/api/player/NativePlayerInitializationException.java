package hs.mediasystem.ui.api.player;

import hs.mediasystem.util.Localizable;
import hs.mediasystem.util.exception.Throwables;

public class NativePlayerInitializationException extends RuntimeException implements Localizable {

  public NativePlayerInitializationException(Throwable cause) {
    super(cause);
  }

  @Override
  public String toLocalizedString() {
    boolean is32bit = System.getProperty("os.arch").equals("x86");

    return "### Unable to load installed video player\n"
        + "MediaSystem was unable to start playing the selected content because the "
        + "configured video player could not be initialized.\n"
        + "#### Solution\n"
        + "Please install a supported " + (is32bit ? "32" : "64") + " bit video player on your system and configure "
        + "MediaSystem with the correct player in `mediasystem.yaml`.\n"
        + "#### Technical details\n"
        + "```\n"
        + Throwables.toString(getCause())
        + "```\n";
  }
}
