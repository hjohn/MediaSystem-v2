package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role.Type;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastPaneFactory {
  private static final Logger LOGGER = Logger.getLogger(CastPaneFactory.class.getName());

  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private WorkClient workClient;

  public Region create(WorkId id) {
    GridPane actorsGrid = Containers.grid("actors-panel");

    Stream.generate(ColumnConstraints::new)
      .peek(cc -> cc.setPercentWidth(100 / 3))
      .limit(3)
      .forEach(cc -> actorsGrid.getColumnConstraints().add(cc));

    RowConstraints rowConstraints1 = new RowConstraints();
    RowConstraints rowConstraints2 = new RowConstraints();

    rowConstraints1.setVgrow(Priority.ALWAYS);
    rowConstraints1.setValignment(VPos.BOTTOM);
    rowConstraints2.setVgrow(Priority.NEVER);
    rowConstraints2.setValignment(VPos.TOP);

    actorsGrid.getRowConstraints().addAll(rowConstraints1, rowConstraints2);

    CompletableFuture.supplyAsync(() -> workClient.findContributions(id))
      .thenAcceptAsync(contributors -> fillActorGrid(contributors, actorsGrid), Platform::runLater)
      .whenComplete((v, e) -> {
        if(e != null) {
          LOGGER.log(Level.WARNING, "Unable to create cast pane", e);
        }
      });

    return actorsGrid;
  }

  private void fillActorGrid(List<Contribution> contributors, GridPane actorsGrid) {
    List<Contribution> topContributors = contributors.stream()
      .filter(c -> c.getRole().getType() == Type.CAST)
      .sorted(Comparator.comparing(c -> c.getOrder()))
      .limit(3)
      .collect(Collectors.toList());

    for(int i = 0; i < topContributors.size(); i++) {
      Contribution contributor = topContributors.get(i);
      AsyncImageProperty imageProperty = new AsyncImageProperty(400, 600);

      imageProperty.imageHandleProperty().set(contributor.getPerson().getImage().map(imageHandleFactory::fromURI).orElse(null));

      Label ph = Labels.create("ph", "?");

      ph.setMaxSize(1000, 1000);

      BiasedImageView photo = new BiasedImageView(ph, 400.0 / 600);

      photo.imageProperty().bind(imageProperty);
      photo.setAlignment(Pos.BOTTOM_CENTER);

      String role = contributor.getRole().getCharacter();

      Label roleLabel = Labels.create("role", "as " + role, Labels.hide(new SimpleBooleanProperty(role == null || role.isEmpty())));

      photo.setPrefHeight(50);

      actorsGrid.at(i, 0).add(photo);
      actorsGrid.at(i, 1).add(Containers.vbox(
        "actor-description",
        Labels.create("name", contributor.getPerson().getName()),
        roleLabel
      ));
    }
  }
}
