module hs.mediasystem.mediamanager {
  exports hs.mediasystem.mediamanager;
  exports hs.mediasystem.mediamanager.db;

  requires mediasystem.ext.basic.media.types;

  requires java.annotation;
  requires java.logging;
  requires javax.inject;
  requires hs.mediasystem.scanner;
  requires hs.mediasystem.util;
}
