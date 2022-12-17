package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.GridViewPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.SettingsSource;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GridViewPresentationFactoryTest {

  @Mock private SettingsSource settingsSource;
  @Mock private SettingsClient settingsClient;
  @InjectMocks private GridViewPresentationFactory factory = new GridViewPresentationFactory() {};

  @Test
  void shouldRememberLastSelection() {
    when(settingsClient.of("MediaSystem:Library:Presentation:settingName")).thenReturn(settingsSource);
    when(settingsSource.getSetting("last-selected")).thenReturn("noot");

    List<String> items = List.of("aap", "noot", "mies");

    GridViewPresentation<String, String> presentation = factory.new GridViewPresentation<>(
      "settingName",
      new ViewOptions<String, String>(
        List.of(new SortOrder<>("alpha", (a, b) -> a.compareTo(b))),
        List.of(new Filter<>("none", a -> true)),
        List.of(new Filter<>("none", a -> true))
      ),
      s -> s
    );

    presentation.inputItems.set(items);

    assertEquals("noot", presentation.selectedItem.getValue());

    presentation.inputItems.set(List.of("aap", "mies", "hond", "noot", "hok"));

    assertEquals("noot", presentation.selectedItem.getValue());
  }

  @Test
  void shouldSelectNearestMatchWhenFilterChanges() {
    when(settingsClient.of("MediaSystem:Library:Presentation:settingName")).thenReturn(settingsSource);
    when(settingsSource.getSetting("last-selected")).thenReturn("2");

    List<String> items = List.of("aap", "noot", "mies", "hond", "hok");

    GridViewPresentation<String, String> presentation = factory.new GridViewPresentation<>(
      "settingName",
      new ViewOptions<String, String>(
        List.of(new SortOrder<>("alpha", (a, b) -> a.compareTo(b))),
        List.of(new Filter<>("none", a -> true)),
        List.of(new Filter<>("none", a -> true))
      ),
      items::indexOf
    );

    presentation.inputItems.set(items);

    assertEquals("mies", presentation.selectedItem.getValue());

    presentation.filter.set(new Filter<>("no-h", a -> !a.startsWith("h")));

    assertEquals("mies", presentation.selectedItem.getValue());

    presentation.filter.set(new Filter<>("only-h", a -> a.startsWith("h")));

    assertEquals("hok", presentation.selectedItem.getValue());
  }

  @Test
  void shouldAllowEmptyList() {
    // Make exception caught by JavaFX visible:
    Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        throw new IllegalStateException(e);
      }
    });

    when(settingsClient.of("MediaSystem:Library:Presentation:settingName")).thenReturn(settingsSource);
    when(settingsSource.getSetting("last-selected")).thenReturn("1");

    List<String> items = List.of();

    GridViewPresentation<String, String> presentation = factory.new GridViewPresentation<>(
      "settingName",
      new ViewOptions<String, String>(
        List.of(new SortOrder<>("alpha", (a, b) -> a.compareTo(b))),
        List.of(new Filter<>("all", a -> true), new Filter<>("none", a -> false)),
        List.of(new Filter<>("none", a -> true))
      ),
      items::indexOf
    );

    presentation.inputItems.set(items);

    presentation.filter.set(presentation.availableFilters.get(1));
  }
}
