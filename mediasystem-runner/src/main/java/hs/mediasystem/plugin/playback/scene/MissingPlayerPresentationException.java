package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.runner.util.Localizable;
import hs.mediasystem.ui.api.player.PlayerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MissingPlayerPresentationException extends RuntimeException implements Localizable {
  private final String configuredName;
  private final List<PlayerFactory> availablePlayerFactories;

  public MissingPlayerPresentationException(String configuredName, List<PlayerFactory> availablePlayerFactories) {
    this.configuredName = configuredName;
    this.availablePlayerFactories = availablePlayerFactories;
  }

  public String getConfiguredName() {
    return configuredName;
  }

  public List<PlayerFactory> getAvailablePlayerFactories() {
    return availablePlayerFactories;
  }

  @Override
  public String toLocalizedString() {
    return "### Unable to play video\n"
        + "MediaSystem was unable to start playing the selected content because the "
        + "configured video player was not available.  The currently configured player is:\n\n"
        + "- `" + getConfiguredName() + "`\n"
        + "#### Solution\n"
        + "Please check the configuration files and configure one of the players that is "
        + "available.  Available players are:"
        + getAvailablePlayerFactories().stream().map(Object::getClass).map(c -> "`" + c.getName() + "`").collect(Collectors.joining("\n- ", "\n- ", ""));
  }
}
