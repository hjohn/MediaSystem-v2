package hs.mediasystem.ui.api.player;

import hs.mediasystem.util.ini.Ini;

public interface PlayerFactory {
  String getName();
  PlayerPresentation create(Ini ini);
}
