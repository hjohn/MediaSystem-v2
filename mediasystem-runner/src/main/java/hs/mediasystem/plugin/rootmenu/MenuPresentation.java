package hs.mediasystem.plugin.rootmenu;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationLoader;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.event.Event;
import javafx.scene.image.Image;

import javax.inject.Inject;
import javax.inject.Singleton;

public class MenuPresentation implements Presentation {
  public final List<Menu> menus;

  public interface Plugin {
    Menu getMenu();
  }

  public static class Menu {
    private final String title;
    private final Image image;
    private final List<MenuItem> items;

    public Menu(String title, Image image, List<MenuItem> items) {
      this.title = title;
      this.image = image;
      this.items = items;
    }

    public String getTitle() {
      return title;
    }

    public Image getImage() {
      return image;
    }

    public List<MenuItem> getMenuItems() {
      return items;
    }
  }

  public static class MenuItem {
    private final String title;
    private final Image image;
    private final Consumer<Event> eventConsumer;

    public MenuItem(String title, Image image, Consumer<Event> eventConsumer) {
      this.title = title;
      this.image = image;
      this.eventConsumer = eventConsumer;
    }

    public MenuItem(String title, Image image, Supplier<Presentation> presentationSupplier) {
      this(title, image, e -> PresentationLoader.navigate(e, presentationSupplier));
    }

    public String getTitle() {
      return title;
    }

    public Consumer<Event> getEventConsumer() {
      return eventConsumer;
    }
  }

  @Singleton
  public static class Factory implements hs.mediasystem.runner.StartupPresentationProvider.Plugin {
    @Inject private List<Plugin> plugins;

    @Override
    public MenuPresentation create() {
      return new MenuPresentation(plugins.stream().map(Plugin::getMenu).collect(Collectors.toList()));
    }
  }

  public MenuPresentation(List<Menu> menus) {
    this.menus = Collections.unmodifiableList(menus);
  }
}
