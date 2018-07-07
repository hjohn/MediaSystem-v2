package hs.mediasystem.mediamanager.db;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.services.ParticipationsQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoDatabase {
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<ParticipationsQueryService> participationsQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;

  public List<ProductionRole> queryParticipations(PersonIdentifier identifier) {
    return participationsQueryServices.get(0).query(identifier);
  }

  public List<PersonRole> queryRoles(Identifier identifier) {
    return rolesQueryServices.get(0).query(identifier);
  }

  public List<VideoLink> queryVideoLinks(ProductionIdentifier identifier) {
    return videoLinksQueryServices.get(0).query(identifier);
  }
}
