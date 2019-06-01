package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCollection<T> implements MediaDescriptor {
  private final CollectionDetails collectionDetails;
  private final List<T> items;

  public AbstractCollection(CollectionDetails collectionDetails, List<T> items) {
    if(collectionDetails == null) {
      throw new IllegalArgumentException("collectionDetails cannot be null");
    }
    if(items == null || items.isEmpty()) {
      throw new IllegalArgumentException("items cannot be null or empty");
    }

    this.collectionDetails = collectionDetails;
    this.items = Collections.unmodifiableList(new ArrayList<>(items));

    if(items.contains(null)) {
      throw new IllegalArgumentException("items cannot contain nulls");
    }
  }

  @Override
  public Identifier getIdentifier() {
    return collectionDetails.getIdentifier();
  }

  public CollectionDetails getCollectionDetails() {
    return collectionDetails;
  }

  public Details getDetails() {
    return collectionDetails.getDetails();
  }

  public List<T> getItems() {
    return items;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "['" + collectionDetails.getDetails().getName() + "' " + items + "]";
  }
}
