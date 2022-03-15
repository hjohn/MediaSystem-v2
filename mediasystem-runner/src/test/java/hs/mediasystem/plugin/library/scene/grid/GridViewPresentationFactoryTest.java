package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.GridViewPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.SettingsSource;

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
    when(settingsSource.getSetting("last-selected")).thenReturn("1");

    List<String> items = List.of("aap", "noot", "mies");

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

    assertEquals("noot", presentation.selectedItem.getValue());
  }
}
