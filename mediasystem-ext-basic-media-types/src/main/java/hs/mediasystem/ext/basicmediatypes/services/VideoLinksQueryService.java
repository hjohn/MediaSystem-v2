package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.util.List;

public interface VideoLinksQueryService {
  List<VideoLink> query(Identifier identifier);
}
