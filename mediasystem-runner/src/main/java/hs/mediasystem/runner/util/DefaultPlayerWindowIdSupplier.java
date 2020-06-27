package hs.mediasystem.runner.util;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;

import java.util.Collections;

import javax.inject.Singleton;

@Singleton
public class DefaultPlayerWindowIdSupplier implements PlayerWindowIdSupplier {
  private static final String SEARCH_TITLE = "MediaSystem";

  @Override
  public long getWindowId() {
    if(Platform.isWindows()) {
      WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, SEARCH_TITLE);

      return Pointer.nativeValue(hwnd.getPointer());
    }

    throw new IllegalStateException("Unsupported platform: " + Platform.getOSType());
  }
}
