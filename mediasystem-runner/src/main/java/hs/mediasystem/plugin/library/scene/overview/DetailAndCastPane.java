package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role.Type;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

public class DetailAndCastPane extends HBox {
  public final Model model = new Model();

  public static class Model {
    public final StringProperty tagline = new SimpleStringProperty();
    public final StringProperty description = new SimpleStringProperty();
    public final ObjectProperty<List<Contribution>> contributors = new SimpleObjectProperty<>();
  }

  private final Label tagLineLabel = Labels.create("tag-line", model.tagline, Labels.HIDE_IF_EMPTY);
  private final Label descriptionLabel = Labels.create("description", model.description);
  private final AutoVerticalScrollPane pane = new AutoVerticalScrollPane(descriptionLabel, 12000, 40);

  {

    /*
     * Limit height of ScrollPane, but at same time give it the rest of the space in the leftBox VBox; this allows
     * the tag-line to wrap if needed, instead of tag-line and description competing for max space
     */

    pane.setPrefHeight(100);

    VBox.setVgrow(pane, Priority.ALWAYS);
  }

  private final VBox leftBox = Containers.vbox(tagLineLabel, pane);

  {
    leftBox.setPrefWidth(100);  // Limit pref width, so free space can be assigned according to Grow / Percentage settings

    HBox.setHgrow(leftBox, Priority.ALWAYS);
  }

  public DetailAndCastPane() {
    Region castPane = create();

    castPane.setPrefWidth(100);  // Limit pref width, so free space can be assigned according to Grow / Percentage settings

    HBox.setHgrow(castPane, Priority.ALWAYS);

    getChildren().addAll(leftBox, castPane);
  }

  public Region create() {
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

    model.contributors.addListener((obs, old, contributors) -> {
      fillActorGrid(contributors, actorsGrid);
    });

    return actorsGrid;
  }

  private static void fillActorGrid(List<Contribution> contributors, GridPane actorsGrid) {
    actorsGrid.getChildren().clear();

    List<Contribution> topContributors = contributors.stream()
      .filter(c -> c.getRole().getType() == Type.CAST)
      .sorted(Comparator.comparing(c -> c.getOrder()))
      .limit(3)
      .collect(Collectors.toList());

    for(int i = 0; i < topContributors.size(); i++) {
      Contribution contributor = topContributors.get(i);
      AsyncImageProperty imageProperty = new AsyncImageProperty(400, 600);

      imageProperty.imageHandleProperty().set(contributor.getPerson().getCover().orElse(null));

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
