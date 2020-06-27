package hs.mediasystem.ui.api.player;

public interface PlayerFactory {
  public enum IntegrationMethod {
    WINDOW,
    PIXEL_BUFFER
  }

  String getName();
  IntegrationMethod getIntegrationMethod();
  PlayerPresentation create();
}
