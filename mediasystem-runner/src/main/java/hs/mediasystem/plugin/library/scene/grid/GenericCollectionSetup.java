package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.overview.ProductionPresentation;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.SettingsSource;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenericCollectionSetup extends AbstractSetup<Work, GenericCollectionPresentation<Work>> {
  @Inject private ProductionPresentation.Factory productionPresentationFactory;
  @Inject private SettingsClient settingsClient;

  @Override
  protected void onItemSelected(ItemSelectedEvent<Work> event, GenericCollectionPresentation<Work> presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getId()));
  }

  @Override
  protected SettingsSource getSettingsSource(GenericCollectionPresentation<Work> presentation) {
    return settingsClient.of(SYSTEM_PREFIX + "Generic:" + presentation.settingPostfix);
  }
}
