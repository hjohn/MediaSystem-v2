package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.GridViewPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.SettingsSource;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GridViewPresentationFactoryTest {

  @Mock private SettingsSource settingsSource;
  @Mock private SettingsClient settingsClient;
  @Mock private BinderProvider binderProvider;
  @InjectMocks private GridViewPresentationFactory factory = new GridViewPresentationFactory() {};

  @Test
  void shouldRememberLastSelection() {
    when(settingsClient.of("MediaSystem:Library:Presentation:settingName")).thenReturn(settingsSource);
    when(settingsSource.getSetting("last-selected")).thenReturn("2");
    doReturn("1").when(binderProvider).map(eq(IDBinder.class), any(), eq("aap"));
    doReturn("2").when(binderProvider).map(eq(IDBinder.class), any(), eq("noot"));
    doReturn("3").when(binderProvider).map(eq(IDBinder.class), any(), eq("mies"));

    GridViewPresentation<String, String> presentation = factory.new GridViewPresentation<>("settingName", new ViewOptions<String, String>(
      List.of(new SortOrder<>("alpha", (a, b) -> a.compareTo(b))),
      List.of(new Filter<>("none", a -> true)),
      List.of(new Filter<>("none", a -> true))
    ));

    presentation.inputItems.set(List.of("aap", "noot", "mies"));

    assertEquals("noot", presentation.selectedItem.getValue());
  }
}
