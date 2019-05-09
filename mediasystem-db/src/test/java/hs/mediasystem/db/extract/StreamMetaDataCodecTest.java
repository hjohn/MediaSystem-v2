package hs.mediasystem.db.extract;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.ext.basicmediatypes.domain.stream.AudioStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Resolution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Snapshot;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.stream.VideoStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class StreamMetaDataCodecTest {
  private StreamMetaDataCodec codec;

  @BeforeEach
  void beforeEach() {
    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    codec = injector.getInstance(StreamMetaDataCodec.class);
  }

  @Test
  void test() throws IOException {
    byte[] data = codec.encode(new StreamMetaData(
      new StreamID(120),
      Duration.ofSeconds(999),
      List.of(
        new VideoStream("title", "lang", "mp4", new Resolution(100, 200, 1.2f), 1234567L, 23.93f)
      ),
      List.of(
        new AudioStream("audiotitle", "audiolang", "mp3", 3)
      ),
      List.of(
        new Snapshot(new ImageURI("localdb://12345/1"), 34222)
      )
    ));

    StreamMetaData metaData = codec.decode(data);

    assertEquals(new StreamID(120), metaData.getStreamId());
    assertEquals(Duration.ofSeconds(999), metaData.getDuration());
    assertEquals("title", metaData.getVideoStreams().get(0).getTitle());
    assertEquals("mp3", metaData.getAudioStreams().get(0).getCodec());
    assertEquals(new StringURI("localdb://12345/1"), metaData.getSnapshots().get(0).getImageUri());
  }

}
