package hs.mediasystem.ui.api.player;

public interface PlayerFactory {
  String getName();
  PlayerPresentation create();
}
