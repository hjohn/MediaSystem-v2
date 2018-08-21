package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;

public interface LocalMediaListener {
  void mediaAdded(MediaStream<?> mediaStream);
  void mediaUpdated(MediaStream<?> mediaStream);
  void mediaRemoved(MediaStream<?> mediaStream);
}
