module hs.mediasystem.mediamanager {
  exports hs.mediasystem.mediamanager;
  exports hs.mediasystem.mediamanager.db;

  requires transitive mediasystem.ext.basic.media.types;
  requires transitive hs.mediasystem.scanner;
  requires transitive hs.mediasystem.util;

  requires java.annotation;
  requires java.logging;
  requires javax.inject;
}
