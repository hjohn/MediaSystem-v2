package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.VideoLink;

import java.util.List;

public interface VideoLinksQueryService {
  List<VideoLink> query(Identifier identifier);
}
