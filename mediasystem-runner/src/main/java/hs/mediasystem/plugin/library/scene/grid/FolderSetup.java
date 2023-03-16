package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.plugin.library.scene.grid.FolderPresentationFactory.FolderPresentation;
import hs.mediasystem.plugin.library.scene.grid.common.ViewStatusBarFactory;
import hs.mediasystem.plugin.library.scene.grid.common.WorkCellPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.presentation.PresentationLoader;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.grid.MediaGridView;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.MediaStream;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.javafx.base.ItemSelectedEvent;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.ui.csslayout.StylableContainers;
import hs.mediasystem.util.javafx.ui.csslayout.StylableVBox;

import java.util.Objects;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.EventStreams;

@Singleton
public class FolderSetup implements NodeFactory<FolderPresentation> {
  private static final String STYLES_URL = LessLoader.compile(FolderSetup.class, "folder-scene-styles.less");

  @Inject private ProductionPresentationFactory productionPresentationFactory;
  @Inject private FolderPresentationFactory folderPresentationFactory;
  @Inject private WorkClient workClient;
  @Inject private Function<ObservableValue<?>, WorkCellPresentation> workCellPresentationFactory;
  @Inject private ViewStatusBarFactory viewStatusBarFactory;

  @Override
  public Node create(FolderPresentation presentation) {
    StylableVBox root = StylableContainers.vbox("folder-scene", createMediaGridView(presentation), viewStatusBarFactory.create(presentation));

    root.getStylesheets().add(STYLES_URL);

    return root;
  }

  private void onItemSelected(ItemSelectedEvent<Work> event, FolderPresentation presentation) {
    Work work = event.getItem();

    if(work.getType().equals(MediaType.FOLDER)) {
      PresentationLoader.navigate(event, () -> folderPresentationFactory.create(
        workClient.findChildren(work.getId()),
        presentation.settingPostfix,
        presentation.viewOptions,
        work
      ));
    }
    else {
      PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getId()));
    }
  }

  private MediaGridView<Work> createMediaGridView(FolderPresentation presentation) {
    MediaGridView<Work> listView = new MediaGridView<>();

    listView.setOrientation(Orientation.VERTICAL);
    listView.cellAlignment.set(Pos.BOTTOM_CENTER);
    listView.visibleColumns.set(3);
    listView.getStyleClass().add("glass-pane");
    listView.onItemSelected.set(e -> onItemSelected(e, presentation));

    workCellPresentationFactory.apply(presentation.selectedItem).associate(Nodes.toEventHandlerTarget(listView));

    AnnotatedImageCellFactory<Work> cellFactory = new AnnotatedImageCellFactory<>((work, model) -> {
      model.title.set(work.getDetails().getTitle());
      model.subtitle.set(work.getDetails().getSubtitle().orElse(null));
      model.watchedFraction.set(work.getPrimaryStream().flatMap(MediaStream::duration).map(d -> {
        if(work.getState().consumed()) {
          return 1.0;
        }

        if(d.isZero()) {
          return Double.NaN;
        }

        double fraction = (double)work.getState().resumePosition().toSeconds() / d.toSeconds();

        if(fraction <= 0) {
          fraction = -1;
        }

        return fraction;
      }).orElse(-1.0));

      ImageHandle imageHandle = work.getDetails().getSampleImage()
        .or(() -> work.getDetails().getBackdrop())
        .orElse(null);

      model.imageHandle.set(imageHandle);
    });

    listView.setCellFactory(cellFactory);
    listView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
      if(current != null) {
        presentation.selectItem(current);
      }
    });

    EventStreams.valuesOf(Nodes.showing(listView))
      .map(visible -> visible ? presentation.items : FXCollections.<Work>emptyObservableList())
      .feedTo(listView.itemsProperty());

    EventStreams.valuesOf(presentation.selectedItem)
      .withDefaultEvent(presentation.selectedItem.getValue())
      .repeatOn(EventStreams.changesOf(listView.itemsProperty()))
      .conditionOnShowing(listView)
      .observe(item -> updateSelectedItem(listView, presentation, item));

    return listView;
  }

  private static void updateSelectedItem(MediaGridView<Work> listView, FolderPresentation presentation, Object selectedItem) {
    if(Objects.equals(selectedItem, listView.getSelectionModel().getSelectedItem())) {
      return;
    }

    int selectedIndex = presentation.items.indexOf(selectedItem);

    if(selectedIndex == -1) {
      selectedIndex = 0;
    }

    listView.getSelectionModel().select(selectedIndex);
  }
}
