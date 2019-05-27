package hs.mediasystem.db;

import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StreamCacheUpdateServiceTest {
  @Mock private DatabaseStreamStore streamStore;
  @Mock private LocalMediaIdentificationService identificationService;
  @InjectMocks private StreamCacheUpdateService updater;

  private final List<String> allowedDataSources = List.of("TMDB");

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    when(streamStore.findStreamSource(any(StreamID.class))).thenReturn(new StreamSource(new StreamTags(Set.of("A")), allowedDataSources));
  }

  @Test
  public void shouldAddMedia() throws InterruptedException {
    BasicStream stream1 = basicStream(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");

    when(streamStore.findStream(new StreamID(1234))).thenReturn(stream1);

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).add(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(identificationService).identify(stream1, allowedDataSources);
    verifyNoMoreInteractions(identificationService);
  }

  @Test
  public void shouldAddAndRemoveMedia() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(20), basicStream(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(21, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");

    when(streamStore.findStream(new StreamID(21))).thenReturn(stream1);

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).add(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
    verify(streamStore).remove(new StreamID(20));
    verify(identificationService).identify(stream1, allowedDataSources);
    verifyNoMoreInteractions(identificationService);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(123), basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    when(streamStore.findStream(new StreamID(123))).thenReturn(stream1);

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).add(eq(1), argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")));
    verify(identificationService).identify(stream1, allowedDataSources);
    verifyNoMoreInteractions(identificationService);
  }

  private static BasicStream basicStream(int identifier, String uri, String title, List<BasicStream> childStreams) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), new StreamID(identifier), attributes, childStreams);
  }

  private static BasicStream basicStream(int identifier, String uri, String title) {
    return basicStream(identifier, uri, title, Collections.emptyList());
  }
}
