package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.util.NaturalLanguage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class PersonParticipationsPresentation extends GridViewPresentation<ProductionRole> {
  public final PersonalProfile personalProfile;

  private static final List<SortOrder<ProductionRole>> SORT_ORDERS = List.of(
    new SortOrder<>("popularity", Comparator.comparing((MediaItem<ProductionRole> mediaItem) -> mediaItem.getData().getPopularity()).reversed()),
    new SortOrder<ProductionRole>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<ProductionRole>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
  );

  private static final List<Filter<ProductionRole>> FILTERS = List.of(
    new Filter<ProductionRole>("none", mi -> true),
    new Filter<ProductionRole>("cast", mi -> mi.getRole().getType() != Role.Type.CREW),
    new Filter<ProductionRole>("crew", mi -> mi.getRole().getType() == Role.Type.CREW)
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private VideoDatabase videoDatabase;
    @Inject private SettingsSourceFactory settingsSourceFactory;
    @Inject private MediaItem.Factory mediaItemFactory;

    public PersonParticipationsPresentation create(Person person) {
      PersonalProfile personalProfile = videoDatabase.queryPersonalProfile(person.getIdentifier());
      ObservableList<MediaItem<ProductionRole>> items = FXCollections.observableList(personalProfile.getProductionRoles().stream().map(this::wrap).collect(Collectors.toList()));

      return new PersonParticipationsPresentation(
        settingsSourceFactory.of(SYSTEM_PREFIX + "Roles"),
        mediaService,
        personalProfile,
        items
      );
    }

    private MediaItem<ProductionRole> wrap(ProductionRole data) {
      return mediaItemFactory.create(data, null);
    }
  }

  protected PersonParticipationsPresentation(SettingsSource settingsSource, MediaService mediaService, PersonalProfile personalProfile, ObservableList<MediaItem<ProductionRole>> items) {
    super(settingsSource, mediaService, items, new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED)));

    this.personalProfile = personalProfile;
  }
}
