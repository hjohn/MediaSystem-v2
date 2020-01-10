package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.CollectionService;
import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.ui.api.CollectionClient;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalCollectionClient implements CollectionClient {
  @Inject private CollectionService service;

  @Override
  public List<Collection> findCollections() {
    return service.findCollections();
  }

}
