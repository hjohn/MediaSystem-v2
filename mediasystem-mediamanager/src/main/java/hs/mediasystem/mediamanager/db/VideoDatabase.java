package hs.mediasystem.mediamanager.db;

import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.PersonalProfileQueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoDatabase {
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private List<QueryService<?>> queryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;
  @Inject private List<PersonalProfileQueryService> personalProfileQueryServices;

  public List<PersonRole> queryRoles(Identifier identifier) {
    return rolesQueryServices.get(0).query(identifier);
  }

  public List<VideoLink> queryVideoLinks(ProductionIdentifier identifier) {
    return videoLinksQueryServices.get(0).query(identifier);
  }

  public <T extends Production> T queryProduction(ProductionIdentifier identifier) {
    for(QueryService<?> queryService : queryServices) {
      if(queryService.getDataSource().equals(identifier.getDataSource())) {
        return (T)queryService.query(identifier).getMediaDescriptor();
      }
    }

    return null;
  }

  public List<Production> queryRecommendedProductions(ProductionIdentifier identifier) {
    return recommendationQueryServices.get(0).query(identifier);
  }

  public PersonalProfile queryPersonalProfile(PersonIdentifier identifier) {
    return personalProfileQueryServices.get(0).query(identifier);
  }
}
