package hs.mediasystem.domain;

import hs.mediasystem.util.ini.Ini;

public interface PlayerFactory {
  String getName();
  PlayerPresentation create(Ini ini);
}
