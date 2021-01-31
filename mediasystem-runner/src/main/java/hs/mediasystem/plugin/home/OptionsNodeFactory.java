package hs.mediasystem.plugin.home;

import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.MarkdownTextView;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ResourceImageHandle;
import hs.mediasystem.util.javafx.ItemSelectedEvent;
import hs.mediasystem.util.javafx.control.ActionListView;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import javax.inject.Singleton;

@Singleton
public class OptionsNodeFactory {
  private static final String STYLES_URL = LessLoader.compile(OptionsNodeFactory.class, "exit-styles.less");

  public ActionListView<Option> create() {
    ActionListView<Option> mediaGridView = new HorizontalCarousel<>(
      List.of(
        new Option("Help", new ResourceImageHandle(HomeScreenNodeFactory.class, "help.jpg"), this::onHelp),
        new Option("Exit", new ResourceImageHandle(HomeScreenNodeFactory.class, "exit.jpg"), this::onExit)
      ),
      e -> e.getItem().action.accept(e),
      new AnnotatedImageCellFactory<>(this::fillOption)
    );

    return mediaGridView;
  }

  private void onExit(ItemSelectedEvent<Option> event) {
    VBox content = Containers.vbox(
      "content",
      Labels.create("text", "Do you want to exit MediaSystem?"),
      Containers.hbox(
        "buttons",
        Buttons.create("Cancel", e -> Event.fireEvent(e.getTarget(), NavigateEvent.back())),
        Buttons.create("Exit", e -> System.exit(0))
      )
    );

    content.getStylesheets().add(STYLES_URL);

    Dialogs.show(event, content);
  }

  private void onHelp(ItemSelectedEvent<Option> event) {
    try {
      try(InputStream stream = HomeScreenNodeFactory.class.getResourceAsStream("/help.markdown")) {
        String markdownText = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        MarkdownTextView textView = new MarkdownTextView();
        Node node = (Node)event.getSource();

        textView.markdownText.set(markdownText);
        textView.setMinSize(node.getScene().getWidth() / 2, node.getScene().getHeight() / 2);

        Dialogs.show(event, textView);
      }
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void fillOption(Option option, AnnotatedImageCellFactory.Model model) {
    model.title.set(option.title);
    model.imageHandle.set(option.imageHandle);
  }

  public static class Option {
    public final String title;
    public final ImageHandle imageHandle;
    public final Consumer<ItemSelectedEvent<Option>> action;

    public Option(String title, ImageHandle imageHandle, Consumer<ItemSelectedEvent<Option>> action) {
      this.title = title;
      this.imageHandle = imageHandle;
      this.action = action;
    }
  }
}
