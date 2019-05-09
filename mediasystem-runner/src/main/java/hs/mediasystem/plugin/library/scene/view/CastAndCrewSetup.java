package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastAndCrewSetup extends AbstractSetup<PersonRole, CastAndCrewPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private PersonParticipationsPresentation.Factory personParticipationsPresentationFactory;

  @Override
  public ObservableList<MediaItem<PersonRole>> getItems(CastAndCrewPresentation presentation) {
    return FXCollections.observableList(presentation.participants);
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
    return contextLayout.create(presentation.mediaItem);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<PersonRole>> event, CastAndCrewPresentation presentation) {
    PresentationLoader.navigate(event, () -> personParticipationsPresentationFactory.create(event.getItem().getPerson()));
  }

  @Override
  public Node create(CastAndCrewPresentation presentation) {
    return createView(presentation);
  }
}
