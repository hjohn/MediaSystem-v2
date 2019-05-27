package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

public class CastAndCrewPresentation extends GridViewPresentation<PersonRole> {
  public final MediaItem<?> mediaItem;
  public final List<MediaItem<PersonRole>> participants;

  private static final List<SortOrder<PersonRole>> SORT_ORDERS = List.of(
    new SortOrder<PersonRole>("best", Comparator.comparing(mediaItem -> mediaItem.getData().getOrder()))
  );

  private static final List<Filter<PersonRole>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("cast", mi -> mi.getRole().getType() != Role.Type.CREW),
    new Filter<>("crew", mi -> mi.getRole().getType() == Role.Type.CREW)
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private VideoDatabase videoDatabase;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private SettingsStore settingsStore;

    public CastAndCrewPresentation create(MediaItem<?> mediaItem) {
      return new CastAndCrewPresentation(
        settingsStore,
        mediaService,
        mediaItem,
        videoDatabase.queryRoles(mediaItem.getRelease().getIdentifier()).stream()
          .map(pr -> mediaItemFactory.create(pr, null))
          .collect(Collectors.toList())
      );
    }
  }

  protected CastAndCrewPresentation(SettingsStore settingsStore, MediaService mediaService, MediaItem<?> mediaItem, List<MediaItem<PersonRole>> participants) {
    super(settingsStore, mediaService, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL));

    this.mediaItem = mediaItem;
    this.participants = participants;
  }
}
