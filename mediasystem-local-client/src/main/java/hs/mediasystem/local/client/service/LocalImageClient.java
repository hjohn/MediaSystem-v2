package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.ImageService;
import hs.mediasystem.ui.api.ImageClient;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalImageClient implements ImageClient {
  @Inject private ImageService service;

  @Override
  public Optional<byte[]> findImage(String id) throws IOException {
    return service.findImage(id);
  }

}
