package hs.mediasystem.ui.api;

import hs.mediasystem.domain.work.Collection;

import java.util.List;

public interface CollectionClient {
  List<Collection> findCollections();
}
