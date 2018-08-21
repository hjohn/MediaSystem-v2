package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.ContextLayout;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastAndCrewSetup extends AbstractSetup<PersonRole, CastAndCrewPresentation> {
  @Inject private VideoDatabase database;
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private PresentationLoader presentationLoader;

  @Override
  public ObservableList<MediaItem<PersonRole>> getItems(CastAndCrewPresentation presentation) {
    MediaItem<?> mediaItem = presentation.mediaItem.get();

    List<MediaItem<PersonRole>> participants = database.queryRoles(mediaItem.getProduction().getIdentifier()).stream().map(this::wrap).collect(Collectors.toList());

    return FXCollections.observableList(participants);
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<PersonRole> cellFactory) {
    cellFactory.setTitleBindProvider(m -> m.personName);
    cellFactory.setImageExtractor(m -> m.getPerson().getImage() == null ? null : imageHandleFactory.fromURI(m.getPerson().getImage()));
    cellFactory.setDetailExtractor(m -> m.getRole().getCharacter() != null && !m.getRole().getCharacter().isEmpty() ? "as " + m.getRole().getCharacter() :
                       m.getRole().getJob() != null && !m.getRole().getJob().isEmpty() ? "(" + m.getRole().getJob() + ")" : "");
  }

  @Override
  protected Node createContextPanel(CastAndCrewPresentation presentation) {
    MediaItem<?> mediaItem = presentation.mediaItem.get();

    return contextLayout.create(mediaItem);
  }

  private MediaItem<PersonRole> wrap(PersonRole data) {
    return new MediaItem<>(data, null, Collections.emptySet(), 0, 0);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<PersonRole>> event, CastAndCrewPresentation presentation) {
//    Event.fireEvent(event.getTarget(), NavigateEvent.to(personParticipationsPresentationProvider.get().set(event.getItem().getPerson())));
//    event.consume();
    presentationLoader.loadAndNavigate(event, PersonParticipationsPresentation.createSetupTask(event.getItem().getPerson()));
  }

  @Override
  protected List<SortOrder<PersonRole>> getAvailableSortOrders() {
    return List.of(
      new SortOrder<PersonRole>("appearances", Comparator.comparing(mediaItem -> mediaItem.getData().getOrder()))
    );
  }

  @Override
  protected List<Filter<PersonRole>> getAvailableFilters() {
    return List.of(
      new Filter<PersonRole>("none", mi -> true),
      new Filter<PersonRole>("cast", mi -> mi.getRole().getType() != Role.Type.CREW),
      new Filter<PersonRole>("crew", mi -> mi.getRole().getType() == Role.Type.CREW)
    );
  }

  @Override
  protected boolean showViewed() {
    return false;
  }

  @Override
  public Node create(CastAndCrewPresentation presentation) {
    return createView(presentation);
  }
}
