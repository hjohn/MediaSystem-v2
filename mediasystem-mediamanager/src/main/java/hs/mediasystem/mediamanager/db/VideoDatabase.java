package hs.mediasystem.mediamanager.db;

import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoDatabase {
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private List<QueryService> queryServices;

  public List<VideoLink> queryVideoLinks(ProductionIdentifier identifier) {
    return videoLinksQueryServices.get(0).query(identifier);
  }

  @SuppressWarnings("unchecked")
  public <T extends Production> T queryProduction(ProductionIdentifier identifier) {
    for(QueryService queryService : queryServices) {
      if(queryService.getDataSource().equals(identifier.getDataSource())) {
        return (T)queryService.query(identifier);
      }
    }

    return null;
  }
}
