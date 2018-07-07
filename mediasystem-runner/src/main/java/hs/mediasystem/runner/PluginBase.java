package hs.mediasystem.runner;

import javafx.scene.image.Image;

public interface PluginBase {
  default String getText(String name) {
    return ResourceManager.getText(getClass(), name);
  }

  default double getDouble(String name) {
    return ResourceManager.getDouble(getClass(), name);
  }

  default Image getImage(String name) {
    return ResourceManager.getImage(getClass(), name);
  }
}
