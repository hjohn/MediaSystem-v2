package hs.mediasystem.mediamanager.db;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.PersonalProfileQueryService;
import hs.mediasystem.ext.basicmediatypes.services.ProductionCollectionQueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.Top100QueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.mediamanager.DescriptorStore;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoDatabase {
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private List<QueryService> queryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;
  @Inject private List<PersonalProfileQueryService> personalProfileQueryServices;
  @Inject private List<ProductionCollectionQueryService> productionCollectionQueryServices;
  @Inject private List<Top100QueryService> top100QueryServices;
  @Inject private DescriptorStore descriptorStore;

  public List<PersonRole> queryRoles(Identifier identifier) {
    for(RolesQueryService rolesQueryService : rolesQueryServices) {
      if(rolesQueryService.getDataSourceName().equals(identifier.getDataSource().getName())) {
        return rolesQueryService.query(identifier);
      }
    }

    return Collections.emptyList();
  }

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

  public List<Production> queryRecommendedProductions(ProductionIdentifier identifier) {
    return recommendationQueryServices.get(0).query(identifier);
  }

  public PersonalProfile queryPersonalProfile(PersonIdentifier identifier) {
    return personalProfileQueryServices.get(0).query(identifier);
  }

  public ProductionCollection queryProductionCollection(Identifier identifier) {
    MediaDescriptor mediaDescriptor = descriptorStore.find(identifier).orElse(null);

    if(mediaDescriptor instanceof IdentifierCollection) {  // If cached, get it from cache instead
      IdentifierCollection identifierCollection = (IdentifierCollection)mediaDescriptor;

      return new ProductionCollection(
        identifierCollection.getCollectionDetails(),
        identifierCollection.getItems().stream()
          .map(descriptorStore::find)
          .filter(Production.class::isInstance)
          .map(Production.class::cast)
          .collect(Collectors.toList())
      );
    }

    return productionCollectionQueryServices.get(0).query(identifier);
  }

  public List<Production> queryTop100() {
    return top100QueryServices.get(0).query();
  }
}
