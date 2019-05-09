package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.mediamanager.MediaService;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.util.NaturalLanguage;

import java.util.Comparator;
import java.util.List;

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
    @Inject private SettingsStore settingsStore;

    public PersonParticipationsPresentation create(Person person) {
      return new PersonParticipationsPresentation(
        settingsStore,
        mediaService,
        videoDatabase.queryPersonalProfile(person.getIdentifier())
      );
    }
  }

  protected PersonParticipationsPresentation(SettingsStore settingsStore, MediaService mediaService, PersonalProfile personalProfile) {
    super(settingsStore, mediaService, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED));

    this.personalProfile = personalProfile;
  }
}
