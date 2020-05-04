package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.Production;

import java.io.IOException;
import java.util.List;

public interface Top100QueryService {
  List<Production> query() throws IOException;
}
