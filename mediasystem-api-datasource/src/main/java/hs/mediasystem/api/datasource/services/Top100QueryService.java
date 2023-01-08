package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.domain.Production;

import java.io.IOException;
import java.util.List;

public interface Top100QueryService {
  List<Production> query() throws IOException;
}
