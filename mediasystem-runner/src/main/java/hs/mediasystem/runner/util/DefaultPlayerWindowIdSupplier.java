package hs.mediasystem.runner.util;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.Window;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;

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

    if(Platform.isLinux()) {
      Display display = X11.INSTANCE.XOpenDisplay(null);
      Window root = X11.INSTANCE.XDefaultRootWindow(display);

      return getWindowId(display, root);
    }

    throw new IllegalStateException("Unsupported platform: " + Platform.getOSType());
  }

  private static long getWindowId(Display display, Window root) {
    PointerByReference childrenRef = new PointerByReference();
    IntByReference childCountRef = new IntByReference();

    X11.WindowByReference windowRef = new X11.WindowByReference();
    X11.WindowByReference parentRef = new X11.WindowByReference();

    X11.INSTANCE.XQueryTree(display, root, windowRef, parentRef, childrenRef, childCountRef);

    if(childrenRef.getValue() == null) {
      throw new IllegalStateException("Unable to query X11 for Window");
    }

    long[] ids;

    if(Native.LONG_SIZE == Long.BYTES) {
      ids = childrenRef.getValue().getLongArray(0, childCountRef.getValue());
    }
    else if(Native.LONG_SIZE == Integer.BYTES) {
      int[] intIds = childrenRef.getValue().getIntArray(0, childCountRef.getValue());

      ids = new long[intIds.length];

      for(int i = 0; i < intIds.length; i++) {
        ids[i] = intIds[i];
      }
    }
    else {
      throw new IllegalStateException("Unexpected size for Native.LONG_SIZE: " + Native.LONG_SIZE);
    }

    for(long id : ids) {
      if(id == 0) {
        continue;
      }

      Window window = new Window(id);
      X11.XTextProperty name = new X11.XTextProperty();

      if(X11.INSTANCE.XGetWMName(display, window, name) != 0) {
        try {
          if(name.value != null && name.value.equals(SEARCH_TITLE)) {
            return id;
          }
        }
        finally {
          X11.INSTANCE.XFree(name.getPointer().getPointer(0));  // this frees the string "name.value" (at offset 0)
        }
      }
    }

    throw new IllegalStateException("Unable to find Window named: " + SEARCH_TITLE);
  }
}
