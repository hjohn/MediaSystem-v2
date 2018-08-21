package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.util.Attributes;

public interface IdentificationService {
  DataSource getDataSource();
  Identification identify(Attributes attributes);
}
