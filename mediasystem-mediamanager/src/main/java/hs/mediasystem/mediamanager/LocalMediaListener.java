package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.Set;

public interface LocalMediaListener {
  <D extends MediaDescriptor> void mediaUpdated(BasicStream stream, Set<Tuple3<Identifier, Identification, D>> records);
}
