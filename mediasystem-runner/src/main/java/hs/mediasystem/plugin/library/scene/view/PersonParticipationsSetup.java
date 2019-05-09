package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersonParticipationsSetup extends AbstractSetup<ProductionRole, PersonParticipationsPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;
  @Inject private MediaItem.Factory mediaItemFactory;

  @Override
  public ObservableList<MediaItem<ProductionRole>> getItems(PersonParticipationsPresentation presentation) {
    return FXCollections.observableList(presentation.personalProfile.getProductionRoles().stream().map(this::wrap).collect(Collectors.toList()));
  }

  private MediaItem<ProductionRole> wrap(ProductionRole data) {
    return mediaItemFactory.create(data, null);
  }

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<ProductionRole> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setSideBarTopLeftBindProvider(item -> item.productionYearRange);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
    cellFactory.setDetailExtractor(
      m -> formatDetail(m)
    );
  }

  private static String formatDetail(MediaItem<ProductionRole> m) {
    Role role = m.getRole();
    ProductionRole productionRole = m.getData();

    String detail = role.getCharacter() != null && !role.getCharacter().isEmpty() ? "as " + role.getCharacter()
      : role.getJob() != null && !role.getJob().isEmpty() ? role.getJob() : "";

    if(productionRole.getEpisodeCount() != null && productionRole.getEpisodeCount() > 0) {
      detail += " (" + productionRole.getEpisodeCount() + ")";
    }

    return detail;
  }

  @Override
  protected Node createContextPanel(PersonParticipationsPresentation presentation) {
    return contextLayout.create(presentation.personalProfile);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<ProductionRole>> event, PersonParticipationsPresentation presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(mediaItemFactory.create(event.getItem().getProduction(), null)));
  }

  @Override
  public Node create(PersonParticipationsPresentation presentation) {
    return createView(presentation);
  }
}
