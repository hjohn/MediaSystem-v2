package hs.mediasystem.runner.action;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationEvent;
import hs.mediasystem.runner.presentation.Presentations;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.runner.util.resource.ResourceManager;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Comparator;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContextMenuHandler {
  private static final String STYLES_URL = LessLoader.compile(ContextMenuHandler.class, "option-menu-dialog.less");

  @Inject private ActionTargetProvider actionTargetProvider;
  @Inject private FXControlFactory controlFactory;

  /*
   * In future, can add support for grouping certain options, and
   * spreading the options across tabs.
   */

  public void handle(PresentationEvent event) {
    GridPane gridPane = new GridPane();
    int row = 0;

    for(Presentation presentation : event.getPresentations()) {
      List<ActionTarget> actionTargets = actionTargetProvider.getActionTargets(presentation.getClass());
      List<LabeledControl> labeledControls = actionTargets.stream()
        .filter(ActionTarget::isVisible)
        .sorted(Comparator.comparing(ActionTarget::getOrder))
        .map(target -> new LabeledControl(target.getLabel(), controlFactory.create(target, presentation)))
        .filter(lc -> lc.control() != null)
        .toList();

      if(!labeledControls.isEmpty()) {
        Class<?> cls = presentation.getClass();

        while(cls.getDeclaringClass() != null) {
          cls = cls.getDeclaringClass();
        }

        Label sectionLabel = Labels.create("section", ResourceManager.getText(cls, "section.label").toUpperCase());

        gridPane.addRow(row++, sectionLabel);

        GridPane.setColumnSpan(sectionLabel, 2);

        for(LabeledControl lc : labeledControls) {
          gridPane.addRow(row++, lc.label == null ? new Label() : Labels.create("header", lc.label), lc.control);
        }
      }
    }

    if(row > 0) {
      gridPane.getStyleClass().add("option-menu-dialog");
      gridPane.getStylesheets().add(STYLES_URL);

      Presentations.showDialog(event, gridPane);

      event.consume();
    }
  }

  private static record LabeledControl(String label, Node control) {}
}
