package hs.mediasystem.runner.collection;

import hs.mediasystem.presentation.Presentation;

public interface CollectionType {
  String getId();
  Presentation createPresentation(String tag);
}
