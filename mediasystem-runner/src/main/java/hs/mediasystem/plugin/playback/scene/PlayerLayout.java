package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.PlayerPresentation;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerLayout {
  @Inject private PlayerSetting playerSetting;

  public PlayerPresentation createPresentation() {
    return playerSetting.get();
  }

  public Object createView(PlayerPresentation presentation) {
    return presentation.getDisplayComponent();
  }
}
