package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import javax.inject.Inject;
import javax.inject.Singleton;

public class CastAndCrewPresentation extends GridViewPresentation<PersonRole> {
  public final MediaItem<?> mediaItem;

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
    @Inject private SettingsSourceFactory settingsSourceFactory;

    public CastAndCrewPresentation create(MediaItem<?> mediaItem) {
      return new CastAndCrewPresentation(
        settingsSourceFactory.of(SYSTEM_PREFIX + "CastAndCrew"),
        mediaService,
        mediaItem,
        videoDatabase.queryRoles(mediaItem.getRelease().getIdentifier()).stream()
          .map(pr -> mediaItemFactory.create(pr, null))
          .collect(Collectors.toList())
      );
    }
  }

  protected CastAndCrewPresentation(SettingsSource settingsSource, MediaService mediaService, MediaItem<?> mediaItem, List<MediaItem<PersonRole>> participants) {
    super(settingsSource, mediaService, FXCollections.observableList(participants), new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL)), null);

    this.mediaItem = mediaItem;
  }
}
