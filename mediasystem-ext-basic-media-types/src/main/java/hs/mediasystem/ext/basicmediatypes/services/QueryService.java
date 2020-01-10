package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

public interface QueryService {
  DataSource getDataSource();
  MediaDescriptor query(Identifier identifier);
}
