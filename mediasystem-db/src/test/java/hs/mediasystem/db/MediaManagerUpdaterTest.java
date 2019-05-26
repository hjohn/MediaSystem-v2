package hs.mediasystem.db;

import hs.mediasystem.mediamanager.LocalMediaManager;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MediaManagerUpdaterTest {
  @Mock private DatabaseStreamStore streamStore;
  @Mock private LocalMediaManager localMediaManager;
  @InjectMocks private MediaManagerUpdater updater;

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldAddMedia() throws InterruptedException {
    BasicStream stream1 = basicStream(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).add(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  @Test
  public void shouldAddAndRemoveMedia() throws InterruptedException {
    when(streamStore.findByScannerId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(20), basicStream(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(21, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).add(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
    verify(streamStore).remove(new StreamID(20));
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws InterruptedException {
    when(streamStore.findByScannerId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(123), basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).add(eq(1), argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")));
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  private static BasicStream basicStream(int identifier, String uri, String title, List<BasicStream> childStreams) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), new StreamID(identifier), attributes, childStreams);
  }

  private static BasicStream basicStream(int identifier, String uri, String title) {
    return basicStream(identifier, uri, title, Collections.emptyList());
  }
}
