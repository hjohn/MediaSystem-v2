package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.ContentID;

import java.util.Optional;

public class MediaStream {
  private final ContentID contentId;
  private final Optional<ContentID> parentId;
  private final StreamAttributes attributes;
  private final State state;
  private final Optional<StreamMetaData> metaData;
  private final Optional<Match> match;

  public MediaStream(ContentID contentId, ContentID parentId, StreamAttributes attributes, State state, StreamMetaData metaData, Match match) {
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }

    this.contentId = contentId;
    this.parentId = Optional.ofNullable(parentId);
    this.attributes = attributes;
    this.state = state;
    this.metaData = Optional.ofNullable(metaData);
    this.match = Optional.ofNullable(match);
  }

  public ContentID getId() {
    return contentId;
  }

  public Optional<ContentID> getParentId() {
    return parentId;
  }

  public StreamAttributes getAttributes() {
    return attributes;
  }

  public State getState() {
    return state;
  }

  public Optional<StreamMetaData> getMetaData() {
    return metaData;
  }

  public Optional<Match> getMatch() {
    return match;
  }
}
