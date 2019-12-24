package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CollectionPresentationProvider {
  @Inject private Set<CollectionType> collectionTypes;

  public Presentation createPresentation(String type, String tag) {
    for(CollectionType collectionType : collectionTypes) {
      if(collectionType.getId().equalsIgnoreCase(type)) {
        return collectionType.createPresentation(tag);
      }
    }

    return null;
  }
}
