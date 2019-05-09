package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.scanner.api.StreamPrintProvider;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class LocalMediaCodecTest {
  private static final StreamID STREAM_ID = new StreamID(100);
  private static final StreamPrint STREAM_PRINT = new StreamPrint(STREAM_ID, 1024L, 0L, new byte[] {1, 2, 3});
  private static final Attributes ATTRIBUTES = Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015");
  private static final LocalDateTime NOW = LocalDateTime.now();

  @Mock private StreamPrintProvider streamPrintProvider;
  @Mock private StreamPrint streamPrint;
  private LocalMediaCodec codec;

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    injector.registerInstance(streamPrintProvider);

    codec = injector.getInstance(LocalMediaCodec.class);

    when(streamPrintProvider.get(STREAM_ID)).thenReturn(STREAM_PRINT);
  }

  @Test
  public void shouldEncodeAndDecode() {
    Map<Identifier, MediaRecord> map = new HashMap<>();
    ProductionIdentifier identifier = new ProductionIdentifier(DataSource.instance(MediaType.of("MOVIE"), "TMDB"), "12345");

    map.put(identifier, new MediaRecord(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Movies.create()));

    LocalMedia localMedia = codec.toLocalMedia(1, NOW, new MediaStream(new BasicStream(MediaType.of("MOVIE"), new StringURI("a"), STREAM_PRINT, ATTRIBUTES, Collections.emptyList()), null, null, map));
    MediaStream mediaStream = codec.toMediaStream(localMedia);

    assertNotNull(mediaStream);
    assertEquals((Long)1024L, mediaStream.getStreamPrint().getSize());
    assertTrue(MediaType.of("MOVIE") == mediaStream.getType());
    assertEquals("Terminator, The", mediaStream.getAttributes().get(Attribute.TITLE));
    assertEquals("Robot kills humans", ((Movie)mediaStream.getMediaRecords().get(identifier).getMediaDescriptor()).getDescription());
  }

  @Test
  public void shouldDecodeNewStreamPrintFormat() {
    LocalMedia localMedia = new LocalMedia();

    localMedia.setJson("{\"stream\": {\"streamPrint\": 100, \"uri\": \"a\", \"attributes\": {\"data\": {\"title\": \"Terminator\"}}, \"type\": \"MOVIE\"}, \"mediaRecords\": {}}".getBytes());

    MediaStream mediaStream = codec.toMediaStream(localMedia);

    assertEquals((Long)1024L, mediaStream.getStreamPrint().getSize());
  }
}
