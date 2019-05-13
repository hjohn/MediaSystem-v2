package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;

import java.time.LocalDateTime;
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

public class MediaManagerUpdaterTest {
  private static final StreamSource STREAM_SOURCE = new StreamSource(new StreamTags(Set.of("A", "B")), List.of("TMDB"));

  @Mock private LocalMediaManager localMediaManager;
  @Mock private DatabaseLocalMediaStore mediaStore;
  @Mock private LocalMediaCodec localMediaCodec;
  @InjectMocks private MediaManagerUpdater updater;

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldAddMedia() throws InterruptedException {
    BasicStream stream1 = basicStream(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");

    updater.update(1, STREAM_SOURCE, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(localMediaManager).put(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")), eq(STREAM_SOURCE), any());
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  @Test
  public void shouldAddAndRemoveMedia() throws InterruptedException {
    when(mediaStore.findByScannerId(1)).thenReturn(new HashMap<>(Map.of(123, new LocalMedia())));
    when(localMediaCodec.toMediaStream(any(LocalMedia.class))).thenReturn(new MediaStream(
      basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica"),
      null,
      null,
      new HashMap<>()
    ));

    BasicStream stream1 = basicStream(1234, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");

    updater.update(1, STREAM_SOURCE, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(localMediaManager).put(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")), eq(STREAM_SOURCE), any());
    verify(localMediaManager).remove(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  @Test
  public void shouldReplaceExistingMediaIfUnableToDecode() throws InterruptedException {
    when(mediaStore.findByScannerId(1)).thenReturn(new HashMap<>(Map.of(123, new LocalMedia())));

    BasicStream stream1 = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");

    updater.update(1, STREAM_SOURCE, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(localMediaManager).put(argThat(ms -> ms.getUri().toString().equals("/home/user/Battlestar%20Galactica")), eq(STREAM_SOURCE), any());
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  @Test
  public void shouldUndeleteExistingMediaIfWasDeleted() throws InterruptedException {
    LocalMedia localMedia = new LocalMedia();
    LocalMedia otherLocalMedia = new LocalMedia();

    localMedia.setDeleteTime(LocalDateTime.now());

    BasicStream basicStream = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    MediaStream existingMediaStream = new MediaStream(
      basicStream,
      null,
      null,
      new HashMap<>()
    );

    when(mediaStore.findByScannerId(1)).thenReturn(new HashMap<>(Map.of(123, localMedia)));
    when(localMediaCodec.toMediaStream(localMedia)).thenReturn(existingMediaStream);
    when(localMediaCodec.toLocalMedia(1, null, existingMediaStream)).thenReturn(otherLocalMedia);

    BasicStream stream1 = basicStream;

    updater.update(1, STREAM_SOURCE, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(mediaStore).store(otherLocalMedia);
    verify(localMediaManager).put(stream1, STREAM_SOURCE, Set.of());
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws InterruptedException {
    LocalMedia localMedia = new LocalMedia();
    MediaStream existingMediaStream = new MediaStream(
      basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v1"),
      null,
      null,
      new HashMap<>()
    );

    when(mediaStore.findByScannerId(1)).thenReturn(new HashMap<>(Map.of(123, localMedia)));
    when(localMediaCodec.toMediaStream(localMedia)).thenReturn(existingMediaStream);

    BasicStream stream1 = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    updater.update(1, STREAM_SOURCE, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(localMediaManager).put(argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")), eq(STREAM_SOURCE), any());
    verify(localMediaManager).incrementallyUpdateStream(stream1.getId());
    verifyNoMoreInteractions(localMediaManager);
  }

  private static BasicStream basicStream(int identifier, String uri, String title, List<BasicStream> childStreams) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), new StreamPrint(new StreamID(identifier), null, 10, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}), attributes, childStreams);
  }

  private static BasicStream basicStream(int identifier, String uri, String title) {
    return basicStream(identifier, uri, title, Collections.emptyList());
  }
}
